/*
 * Copyright © 2015 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.jwhere.core;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jfunctional.Unit;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Functions for producing {@link CatalogDisk} values from existing directories.
 */

public final class CatalogFilesystemReader
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CatalogFilesystemReader.class);
  }

  private CatalogFilesystemReader()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Construct a new disk from the given directory. The directory is assumed to represent a single
   * mounted filesystem.
   *
   * @param disk_name The disk name
   * @param index     The disk ID
   * @param root      The root directory
   *
   * @return A new disk
   *
   * @throws IOException      On I/O errors
   * @throws CatalogException On other catalog-related errors
   */

  public static CatalogDisk newDisk(
    final CatalogDiskName disk_name,
    final CatalogDiskID index,
    final Path root)
    throws IOException, CatalogException
  {
    Objects.requireNonNull(disk_name, "disk_name");
    Objects.requireNonNull(index, "index");
    Objects.requireNonNull(root, "root");

    LOG.debug(
      "creating new disk \"{}\" index {} for root {}", disk_name, index, root);

    final var store = Files.getFileStore(root);
    final var size = BigInteger.valueOf(store.getTotalSpace());
    final var fs_type = store.type();

    final var id_pool =
      new AtomicReference<>(BigInteger.ZERO);
    final var root_dir =
      onDirectory(id_pool, root);

    final var db =
      CatalogDisk.newDiskBuilder(root_dir, disk_name, fs_type, index, size);

    final Deque<CatalogDirectoryNode> dirs = new LinkedList<>();
    dirs.push(root_dir);

    try {
      Files.walkFileTree(
        root,
        EnumSet.noneOf(FileVisitOption.class),
        Integer.MAX_VALUE,
        new DiskCreator(root, dirs, id_pool, db));

      return db.build();
    } catch (final IOException e) {
      if (e.getCause() instanceof CatalogException) {
        throw (CatalogException) e.getCause();
      } else {
        throw e;
      }
    }
  }

  /**
   * Verify a disk by walking a filesystem and checking that all files exist, no extra files exist,
   * and that each file matches that given in the given disk catalog.
   *
   * @param d        The disk to be verified
   * @param settings The report settings
   * @param root     The root directory
   * @param listener A listener that will receive verification results
   *
   * @throws IOException On I/O errors
   */

  public static void verifyDisk(
    final CatalogDisk d,
    final CatalogVerificationReportSettings settings,
    final Path root,
    final CatalogVerificationListenerType listener)
    throws IOException
  {
    Objects.requireNonNull(d, "disk");
    Objects.requireNonNull(settings, "settings");
    Objects.requireNonNull(root, "root");
    Objects.requireNonNull(listener, "listener");

    final var meta = d.getMeta();
    LOG.debug(
      "verifying disk \"{}\" index {} for root {}",
      meta.getDiskName(),
      meta.getDiskID(),
      root);

    final var id_pool =
      new AtomicReference<>(BigInteger.ZERO);

    final var logging_listener = new LoggingListener(d, listener);

    Files.walkFileTree(
      root,
      EnumSet.noneOf(FileVisitOption.class),
      Integer.MAX_VALUE,
      new VerifyingPathVisitor(root, d, logging_listener, id_pool, settings));

    final var g =
      d.getFilesystemGraph();
    for (final var v : g.vertexSet()) {
      final var p = d.getPathForNode(v);
      final var q = stringListToPath(root, p);

      if (!logging_listener.pathIsReferenced(q)) {
        listener.onItemError(CatalogVerificationVanishedItem.builder().setPath(q).build());
      }
    }

    listener.onCompleted();
  }

  private static void compareNodes(
    final CatalogVerificationReportSettings settings,
    final Path path,
    final CatalogNodeType node,
    final CatalogNodeType node_now,
    final LoggingListener rb)
  {
    if (!node.getClass().equals(node_now.getClass())) {
      rb.onItemError(new CatalogVerificationChangedType(path, node, node_now));
    }

    compareNodeOwnership(path, node, node_now, rb);
    compareNodeTimes(
      settings, path, node, node_now, rb);
    compareNodeHashes(path, node, node_now, rb);

    if (!rb.pathIsReferenced(path)) {
      rb.onItemVerified(CatalogVerificationOKItem.builder().setPath(path).build());
    }
  }

  private static void compareNodeHashes(
    final Path path,
    final CatalogNodeType node,
    final CatalogNodeType node_now,
    final LoggingListener rb)
  {
    node.matchNode(new CompareNodeHashesMatcher(node_now, rb, path));
  }

  private static void compareNodeTimes(
    final CatalogVerificationReportSettings settings,
    final Path path,
    final CatalogNodeType node,
    final CatalogNodeType node_now,
    final LoggingListener rb)
  {
    if (settings.ignoreAccessTime() == CatalogIgnoreAccessTime.DO_NOT_IGNORE_ACCESS_TIME) {
      final var then_atime = node.accessTime();
      final var curr_atime = node_now.accessTime();
      if (!then_atime.equals(curr_atime)) {
        rb.onItemError(
          CatalogVerificationChangedMetadata.builder()
            .setField(CatalogVerificationMetadataField.ACCESS_TIME)
            .setPath(path)
            .setValueThen(then_atime.toString())
            .setValueNow(curr_atime.toString())
            .build());
      }
    }

    final var then_mtime = node.modificationTime();
    final var curr_mtime = node_now.modificationTime();
    if (!then_mtime.equals(curr_mtime)) {
      rb.onItemError(
        CatalogVerificationChangedMetadata.builder()
          .setField(CatalogVerificationMetadataField.MODIFICATION_TIME)
          .setPath(path)
          .setValueThen(then_mtime.toString())
          .setValueNow(curr_mtime.toString())
          .build());
    }

    final var then_ctime = node.creationTime();
    final var curr_ctime = node_now.creationTime();
    if (!then_ctime.equals(curr_ctime)) {
      rb.onItemError(
        CatalogVerificationChangedMetadata.builder()
          .setField(CatalogVerificationMetadataField.CREATION_TIME)
          .setPath(path)
          .setValueThen(then_ctime.toString())
          .setValueNow(curr_ctime.toString())
          .build());
    }
  }

  private static void compareNodeOwnership(
    final Path path,
    final CatalogNodeType node,
    final CatalogNodeType node_now,
    final LoggingListener rb)
  {
    if (!node.owner().equals(node_now.owner())) {
      rb.onItemError(
        CatalogVerificationChangedMetadata.builder()
          .setField(CatalogVerificationMetadataField.OWNER)
          .setPath(path)
          .setValueThen(node.owner())
          .setValueNow(node_now.owner())
          .build());
    }

    if (!node.group().equals(node_now.group())) {
      rb.onItemError(
        CatalogVerificationChangedMetadata.builder()
          .setField(CatalogVerificationMetadataField.GROUP)
          .setPath(path)
          .setValueThen(node.group())
          .setValueNow(node_now.group())
          .build());
    }

    if (!node.permissions().equals(node_now.permissions())) {
      rb.onItemError(
        CatalogVerificationChangedMetadata.builder()
          .setField(CatalogVerificationMetadataField.OWNER)
          .setPath(path)
          .setValueThen(PosixFilePermissions.toString(node.permissions()))
          .setValueNow(PosixFilePermissions.toString(node_now.permissions()))
          .build());
    }
  }

  private static List<String> pathToStringList(final Path file)
  {
    final List<String> xs = new ArrayList<>(16);
    final var iter = file.iterator();
    while (iter.hasNext()) {
      xs.add(iter.next().toString());
    }
    return xs;
  }

  private static Path stringListToPath(
    final Path root,
    final List<String> file)
  {
    var p = root;
    final var iter = file.iterator();
    while (iter.hasNext()) {
      p = p.resolve(iter.next());
    }
    return root.relativize(p);
  }

  private static CatalogFileNode onFile(
    final AtomicReference<BigInteger> id_pool,
    final Path file)
    throws IOException
  {
    final BigInteger size;
    final Set<PosixFilePermission> perms;
    final String owner;
    final String group;
    final Instant a_time;
    final Instant c_time;
    final Instant m_time;

    final var posix_view = Files.getFileAttributeView(
      file, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

    if (posix_view != null) {
      final var attribs = posix_view.readAttributes();
      size = BigInteger.valueOf(attribs.size());
      owner = attribs.owner().getName();
      group = attribs.group().getName();
      perms = attribs.permissions();
      a_time = attribs.lastAccessTime().toInstant();
      m_time = attribs.lastModifiedTime().toInstant();
      c_time = attribs.creationTime().toInstant();
    } else {
      final var basic_view = Files.getFileAttributeView(
        file, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

      if (basic_view != null) {
        final var attribs = basic_view.readAttributes();
        owner = "nobody";
        group = "nobody";
        size = BigInteger.valueOf(attribs.size());
        perms = EnumSet.noneOf(PosixFilePermission.class);
        a_time = attribs.lastAccessTime().toInstant();
        m_time = attribs.lastModifiedTime().toInstant();
        c_time = attribs.creationTime().toInstant();
      } else {
        throw new UnreachableCodeException();
      }
    }

    LOG.debug("hashing {}", file);
    final var hash = CatalogFileHashes.fromFile(file);

    return CatalogFileNode.builder()
      .setPermissions(perms)
      .setOwner(owner)
      .setGroup(group)
      .setId(id_pool.updateAndGet(x -> x.add(BigInteger.ONE)))
      .setAccessTime(a_time)
      .setCreationTime(c_time)
      .setModificationTime(m_time)
      .setSize(size)
      .setHash(hash)
      .build();
  }

  private static CatalogDirectoryNode onDirectory(
    final AtomicReference<BigInteger> id_pool,
    final Path root)
    throws IOException
  {
    final Set<PosixFilePermission> perms;
    final String owner;
    final String group;
    final Instant a_time;
    final Instant c_time;
    final Instant m_time;

    final var posix_view = Files.getFileAttributeView(
      root, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

    if (posix_view != null) {
      final var attribs = posix_view.readAttributes();
      owner = attribs.owner().getName();
      group = attribs.group().getName();
      perms = attribs.permissions();
      a_time = attribs.lastAccessTime().toInstant();
      m_time = attribs.lastModifiedTime().toInstant();
      c_time = attribs.creationTime().toInstant();
    } else {
      final var basic_view = Files.getFileAttributeView(
        root, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

      if (basic_view != null) {
        final var attribs = basic_view.readAttributes();
        owner = "nobody";
        group = "nobody";
        perms = EnumSet.noneOf(PosixFilePermission.class);
        a_time = attribs.lastAccessTime().toInstant();
        m_time = attribs.lastModifiedTime().toInstant();
        c_time = attribs.creationTime().toInstant();
      } else {
        throw new UnreachableCodeException();
      }
    }

    return CatalogDirectoryNode.builder()
      .setPermissions(perms)
      .setOwner(owner)
      .setGroup(group)
      .setId(id_pool.updateAndGet(x -> x.add(BigInteger.ONE)))
      .setAccessTime(a_time)
      .setCreationTime(c_time)
      .setModificationTime(m_time)
      .build();
  }

  private static final class LoggingListener
    implements CatalogVerificationListenerType
  {
    private final Set<Path> reported_paths;
    private final CatalogVerificationListenerType delegate;

    LoggingListener(
      final CatalogDisk in_disk,
      final CatalogVerificationListenerType in_delegate)
    {
      Objects.requireNonNull(in_disk, "in_disk");
      this.delegate = Objects.requireNonNull(in_delegate, "in_delegate");
      this.reported_paths =
        new HashSet<>(in_disk.getFilesystemGraph().vertexSet().size());
    }

    @Override
    public void onItemVerified(final CatalogVerificationReportItemOKType ok)
    {
      this.reported_paths.add(ok.path());
      this.delegate.onItemVerified(ok);
    }

    @Override
    public void onItemError(final CatalogVerificationReportItemErrorType error)
    {
      this.reported_paths.add(error.path());
      this.delegate.onItemError(error);
    }

    @Override
    public void onCompleted()
    {
      this.delegate.onCompleted();
    }

    boolean pathIsReferenced(final Path p)
    {
      return this.reported_paths.contains(Objects.requireNonNull(p, "p"));
    }
  }

  private static final class CompareNodeHashesMatcher
    implements CatalogNodeMatcherType<Unit, UnreachableCodeException>
  {
    private final CatalogNodeType node_now;
    private final LoggingListener listener;
    private final Path path;

    CompareNodeHashesMatcher(
      final CatalogNodeType in_node_now,
      final LoggingListener in_listener,
      final Path in_path)
    {
      this.node_now = in_node_now;
      this.listener = in_listener;
      this.path = in_path;
    }

    @Override
    public Unit onFile(final CatalogFileNodeType file_then)
    {
      if (this.node_now instanceof CatalogFileNode) {
        final var file_now = (CatalogFileNode) this.node_now;
        final var then_opt = file_then.hash();
        final var now_opt = file_now.hash();

        if (then_opt.isPresent()) {
          final var hash_then = then_opt.get();
          final var hash_now = now_opt.get();
          if (!hash_then.equals(hash_now)) {
            this.listener.onItemError(
              CatalogVerificationChangedHash.builder()
                .setPath(this.path)
                .setHashNow(hash_now)
                .setHashThen(hash_then)
                .build());
          }
        }
      }

      return Unit.unit();
    }

    @Override
    public Unit onDirectory(final CatalogDirectoryNodeType d)
    {
      return Unit.unit();
    }
  }

  private static final class VerifyingPathVisitor implements FileVisitor<Path>
  {
    private final Path root;
    private final CatalogDisk disk;
    private final LoggingListener logging_listener;
    private final AtomicReference<BigInteger> id_pool;
    private final CatalogVerificationReportSettings settings;

    VerifyingPathVisitor(
      final Path in_root,
      final CatalogDisk in_disk,
      final LoggingListener in_logging_listener,
      final AtomicReference<BigInteger> in_id_pool,
      final CatalogVerificationReportSettings in_settings)
    {
      this.root = in_root;
      this.disk = in_disk;
      this.logging_listener = in_logging_listener;
      this.id_pool = in_id_pool;
      this.settings = in_settings;
    }

    @Override
    public FileVisitResult preVisitDirectory(
      final Path dir,
      final BasicFileAttributes attrs)
      throws IOException
    {
      LOG.debug(
        "preVisitDirectory: {}", dir);

      final var path_rel = this.root.relativize(dir);

      final var path =
        pathToStringList(path_rel);
      LOG.debug("path: {}", path);

      final Optional<CatalogNodeType> node_opt;
      if (dir.equals(this.root)) {
        node_opt = Optional.of(this.disk.getFilesystemRoot());
      } else {
        node_opt = this.disk.getNodeForPath(path);
      }

      if (node_opt.isEmpty()) {
        this.logging_listener.onItemError(CatalogVerificationUncataloguedItem.builder().setPath(
          path_rel).build());
        return FileVisitResult.CONTINUE;
      }

      final var node = node_opt.get();
      final var node_now =
        onDirectory(this.id_pool, dir);

      compareNodes(
        this.settings, path_rel, node, node_now, this.logging_listener);

      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(
      final Path file,
      final BasicFileAttributes attrs)
      throws IOException
    {
      final var path_rel = this.root.relativize(file);

      final var path =
        pathToStringList(path_rel);
      LOG.debug("path: {}", path);

      final var node_opt = this.disk.getNodeForPath(path);

      if (node_opt.isEmpty()) {
        this.logging_listener.onItemError(CatalogVerificationUncataloguedItem.builder().setPath(
          path_rel).build());
        return FileVisitResult.CONTINUE;
      }

      final var node = node_opt.get();
      final var node_now =
        onFile(this.id_pool, file);

      compareNodes(
        this.settings, path_rel, node, node_now, this.logging_listener);

      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(
      final Path file,
      final IOException exc)
      throws IOException
    {
      throw exc;
    }

    @Override
    public FileVisitResult postVisitDirectory(
      final Path dir,
      final IOException exc)
      throws IOException
    {
      LOG.debug(
        "postVisitDirectory: {}", dir);
      return FileVisitResult.CONTINUE;
    }
  }

  private static final class DiskCreator implements FileVisitor<Path>
  {
    private final Path root;
    private final Deque<CatalogDirectoryNode> directories;
    private final AtomicReference<BigInteger> id_pool;
    private final CatalogDiskBuilderType disk_builder;

    DiskCreator(
      final Path in_root,
      final Deque<CatalogDirectoryNode> in_directories,
      final AtomicReference<BigInteger> in_id_pool,
      final CatalogDiskBuilderType in_disk_builder)
    {
      this.root = in_root;
      this.directories = in_directories;
      this.id_pool = in_id_pool;
      this.disk_builder = in_disk_builder;
    }

    @Override
    public FileVisitResult preVisitDirectory(
      final Path dir,
      final BasicFileAttributes attrs)
      throws IOException
    {
      try {
        LOG.debug(
          "preVisitDirectory: {}", dir);

        final var fn = dir.getFileName();
        if (fn == null) {
          Preconditions.checkPreconditionV(dir.equals(this.root), "Root must match");
        } else {
          if (!dir.equals(this.root)) {
            final var current = this.directories.peek();
            final var new_dir =
              onDirectory(this.id_pool, dir);

            final var name = fn.toString();
            this.disk_builder.addNode(current, name, new_dir);
            this.directories.push(new_dir);
          }
        }

        return FileVisitResult.CONTINUE;
      } catch (final CatalogNodeException e) {
        throw new IOException(e);
      }
    }

    @Override
    public FileVisitResult visitFile(
      final Path file,
      final BasicFileAttributes attrs)
      throws IOException
    {
      try {
        LOG.debug("visitFile: {}", file);

        if (attrs.isRegularFile()) {
          final var current = this.directories.peek();
          final var new_file =
            onFile(this.id_pool, file);
          final var name = file.getFileName().toString();
          this.disk_builder.addNode(current, name, new_file);
        }

        return FileVisitResult.CONTINUE;
      } catch (final CatalogNodeException e) {
        throw new IOException(e);
      }
    }

    @Override
    public FileVisitResult visitFileFailed(
      final Path file,
      final IOException exc)
      throws IOException
    {
      throw exc;
    }

    @Override
    public FileVisitResult postVisitDirectory(
      final Path dir,
      final IOException exc)
      throws IOException
    {
      LOG.debug(
        "postVisitDirectory: {}", dir);

      Preconditions.checkPreconditionV(!this.directories.isEmpty(), "Must have empty directories");
      this.directories.pop();
      return FileVisitResult.CONTINUE;
    }
  }
}
