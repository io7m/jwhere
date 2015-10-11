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

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.jgrapht.graph.UnmodifiableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valid4j.Assertive;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileStore;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Functions for producing {@link CatalogDisk} values from existing
 * directories.
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
   * Construct a new disk from the given directory. The directory is assumed to
   * represent a single mounted filesystem.
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
    NullCheck.notNull(disk_name);
    NullCheck.notNull(index);
    NullCheck.notNull(root);

    CatalogFilesystemReader.LOG.debug(
      "creating new disk \"{}\" index {} for root {}", disk_name, index, root);

    final FileStore store = Files.getFileStore(root);
    final BigInteger size = BigInteger.valueOf(store.getTotalSpace());
    final String fs_type = store.type();

    final AtomicReference<BigInteger> id_pool =
      new AtomicReference<>(BigInteger.ZERO);
    final CatalogDirectoryNode root_dir =
      CatalogFilesystemReader.onDirectory(id_pool, root);

    final CatalogDiskBuilderType db =
      CatalogDisk.newDiskBuilder(root_dir, disk_name, fs_type, index, size);

    final Deque<CatalogDirectoryNode> dirs = new LinkedList<>();
    dirs.push(root_dir);

    try {
      Files.walkFileTree(
        root,
        EnumSet.noneOf(FileVisitOption.class),
        Integer.MAX_VALUE,
        new FileVisitor<Path>()
        {
          @Override public FileVisitResult preVisitDirectory(
            final Path dir,
            final BasicFileAttributes attrs)
            throws IOException
          {
            try {
              CatalogFilesystemReader.LOG.debug(
                "preVisitDirectory: {}", dir);

              final Path fn = dir.getFileName();
              if (fn == null) {
                Assertive.ensure(dir.equals(root));
              } else {
                if (!dir.equals(root)) {
                  final CatalogDirectoryNode current = dirs.peek();
                  final CatalogDirectoryNode new_dir =
                    CatalogFilesystemReader.onDirectory(id_pool, dir);

                  final String name = fn.toString();
                  db.addNode(current, name, new_dir);
                  dirs.push(new_dir);
                }
              }

              return FileVisitResult.CONTINUE;
            } catch (final CatalogNodeException e) {
              throw new IOException(e);
            }
          }

          @Override public FileVisitResult visitFile(
            final Path file,
            final BasicFileAttributes attrs)
            throws IOException
          {
            try {
              CatalogFilesystemReader.LOG.debug("visitFile: {}", file);

              final CatalogDirectoryNode current = dirs.peek();
              final CatalogFileNode new_file =
                CatalogFilesystemReader.onFile(id_pool, file);
              final String name = file.getFileName().toString();
              db.addNode(current, name, new_file);
              return FileVisitResult.CONTINUE;
            } catch (final CatalogNodeException e) {
              throw new IOException(e);
            }
          }

          @Override public FileVisitResult visitFileFailed(
            final Path file,
            final IOException exc)
            throws IOException
          {
            throw exc;
          }

          @Override public FileVisitResult postVisitDirectory(
            final Path dir,
            final IOException exc)
            throws IOException
          {
            CatalogFilesystemReader.LOG.debug(
              "postVisitDirectory: {}", dir);

            Assertive.ensure(!dirs.isEmpty());
            dirs.pop();
            return FileVisitResult.CONTINUE;
          }
        });

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
   * Verify a disk by walking a filesystem and checking that all files exist, no
   * extra files exist, and that each file matches that given in the given disk
   * catalog.
   *
   * @param d        The disk to be verified
   * @param settings The report settings
   * @param root     The root directory
   *
   * @return A verification report
   *
   * @throws IOException On I/O errors
   */

  public static CatalogVerificationReport verifyDisk(
    final CatalogDisk d,
    final CatalogVerificationReport.Settings settings,
    final Path root)
    throws IOException
  {
    NullCheck.notNull(d);
    NullCheck.notNull(settings);
    NullCheck.notNull(root);

    final CatalogDiskMetadata meta = d.getMeta();
    CatalogFilesystemReader.LOG.debug(
      "verifying disk \"{}\" index {} for root {}",
      meta.getDiskName(),
      meta.getDiskID(),
      root);

    final CatalogVerificationReportBuilderType rb =
      CatalogVerificationReport.newBuilder();

    final AtomicReference<BigInteger> id_pool =
      new AtomicReference<>(BigInteger.ZERO);

    Files.walkFileTree(
      root,
      EnumSet.noneOf(FileVisitOption.class),
      Integer.MAX_VALUE,
      new FileVisitor<Path>()
      {
        @Override public FileVisitResult preVisitDirectory(
          final Path dir,
          final BasicFileAttributes attrs)
          throws IOException
        {
          CatalogFilesystemReader.LOG.debug(
            "preVisitDirectory: {}", dir);

          final Path path_rel = root.relativize(dir);
          final List<String> path =
            CatalogFilesystemReader.pathToStringList(path_rel);
          CatalogFilesystemReader.LOG.debug("path: {}", path);

          final Optional<CatalogNodeType> node_opt;
          if (dir.equals(root)) {
            node_opt = Optional.of(d.getFilesystemRoot());
          } else {
            node_opt = d.getNodeForPath(path);
          }

          if (!node_opt.isPresent()) {
            rb.addItemUncatalogued(path_rel);
            return FileVisitResult.CONTINUE;
          }

          final CatalogNodeType node = node_opt.get();
          final CatalogDirectoryNode node_now =
            CatalogFilesystemReader.onDirectory(id_pool, dir);

          CatalogFilesystemReader.compareNodes(
            settings, path_rel, node, node_now, rb);

          return FileVisitResult.CONTINUE;
        }

        @Override public FileVisitResult visitFile(
          final Path file,
          final BasicFileAttributes attrs)
          throws IOException
        {
          final Path path_rel = root.relativize(file);
          final List<String> path =
            CatalogFilesystemReader.pathToStringList(path_rel);
          CatalogFilesystemReader.LOG.debug("path: {}", path);

          final Optional<CatalogNodeType> node_opt = d.getNodeForPath(path);

          if (!node_opt.isPresent()) {
            rb.addItemUncatalogued(path_rel);
            return FileVisitResult.CONTINUE;
          }

          final CatalogNodeType node = node_opt.get();
          final CatalogFileNode node_now =
            CatalogFilesystemReader.onFile(id_pool, file);

          CatalogFilesystemReader.compareNodes(
            settings, path_rel, node, node_now, rb);

          return FileVisitResult.CONTINUE;
        }

        @Override public FileVisitResult visitFileFailed(
          final Path file,
          final IOException exc)
          throws IOException
        {
          throw exc;
        }

        @Override public FileVisitResult postVisitDirectory(
          final Path dir,
          final IOException exc)
          throws IOException
        {
          CatalogFilesystemReader.LOG.debug(
            "postVisitDirectory: {}", dir);
          return FileVisitResult.CONTINUE;
        }
      });

    final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> g =
      d.getFilesystemGraph();
    for (final CatalogNodeType v : g.vertexSet()) {
      final List<String> p = d.getPathForNode(v);
      final Path q = CatalogFilesystemReader.stringListToPath(root, p);

      if (!rb.pathIsReferenced(q)) {
        rb.addItemDisappeared(q);
      }
    }

    return rb.build();
  }

  private static void compareNodes(
    final CatalogVerificationReport.Settings settings,
    final Path path,
    final CatalogNodeType node,
    final CatalogNodeType node_now,
    final CatalogVerificationReportBuilderType rb)
  {
    if (!node.getClass().equals(node_now.getClass())) {
      rb.addItemChangedType(path, node, node_now);
    }

    CatalogFilesystemReader.compareNodeOwnership(path, node, node_now, rb);
    CatalogFilesystemReader.compareNodeTimes(
      settings, path, node, node_now, rb);
    CatalogFilesystemReader.compareNodeHashes(path, node, node_now, rb);

    if (!rb.pathIsReferenced(path)) {
      rb.addItemOK(path);
    }
  }

  private static void compareNodeHashes(
    final Path path,
    final CatalogNodeType node,
    final CatalogNodeType node_now,
    final CatalogVerificationReportBuilderType rb)
  {
    node.matchNode(
      new CatalogNodeMatcherType<Unit, UnreachableCodeException>()
      {
        @Override public Unit onFile(final CatalogFileNode file_then)
        {
          if (node_now instanceof CatalogFileNode) {
            final CatalogFileNode file_now = (CatalogFileNode) node_now;
            final Optional<CatalogFileHash> then_opt = file_then.getHash();
            final Optional<CatalogFileHash> now_opt = file_now.getHash();

            if (then_opt.isPresent()) {
              final CatalogFileHash hash_then = then_opt.get();
              final CatalogFileHash hash_now = now_opt.get();
              if (!hash_then.equals(hash_now)) {
                rb.addItemHashChanged(
                  path, hash_then, hash_now);
              }
            }
          }

          return Unit.unit();
        }

        @Override public Unit onDirectory(final CatalogDirectoryNode d)
        {
          return Unit.unit();
        }
      });
  }

  private static void compareNodeTimes(
    final CatalogVerificationReport.Settings settings,
    final Path path,
    final CatalogNodeType node,
    final CatalogNodeType node_now,
    final CatalogVerificationReportBuilderType rb)
  {
    if (settings.getIgnoreAccessTime()
        == CatalogVerificationReport.IgnoreAccessTime
          .DO_NOT_IGNORE_ACCESS_TIME) {
      final Instant then_atime = node.getAccessTime();
      final Instant curr_atime = node_now.getAccessTime();
      if (!then_atime.equals(curr_atime)) {
        rb.addItemMetadataChanged(
          path,
          CatalogVerificationChangedMetadata.Field.ACCESS_TIME,
          then_atime.toString(),
          curr_atime.toString());
      }
    }

    final Instant then_mtime = node.getModificationTime();
    final Instant curr_mtime = node_now.getModificationTime();
    if (!then_mtime.equals(curr_mtime)) {
      rb.addItemMetadataChanged(
        path,
        CatalogVerificationChangedMetadata.Field.MODIFICATION_TIME,
        then_mtime.toString(),
        curr_mtime.toString());
    }

    final Instant then_ctime = node.getCreationTime();
    final Instant curr_ctime = node_now.getCreationTime();
    if (!then_ctime.equals(curr_ctime)) {
      rb.addItemMetadataChanged(
        path,
        CatalogVerificationChangedMetadata.Field.CREATION_TIME,
        then_ctime.toString(),
        curr_ctime.toString());
    }
  }

  private static void compareNodeOwnership(
    final Path path,
    final CatalogNodeType node,
    final CatalogNodeType node_now,
    final CatalogVerificationReportBuilderType rb)
  {
    if (!node.getOwner().equals(node_now.getOwner())) {
      rb.addItemMetadataChanged(
        path,
        CatalogVerificationChangedMetadata.Field.OWNER,
        node.getOwner(),
        node_now.getOwner());
    }

    if (!node.getGroup().equals(node_now.getGroup())) {
      rb.addItemMetadataChanged(
        path,
        CatalogVerificationChangedMetadata.Field.GROUP,
        node.getGroup(),
        node_now.getGroup());
    }

    if (!node.getPermissions().equals(node_now.getPermissions())) {
      rb.addItemMetadataChanged(
        path,
        CatalogVerificationChangedMetadata.Field.PERMISSIONS,
        PosixFilePermissions.toString(node.getPermissions()),
        PosixFilePermissions.toString(node_now.getPermissions()));
    }
  }

  private static List<String> pathToStringList(final Path file)
  {
    final List<String> xs = new ArrayList<>(16);
    final Iterator<Path> iter = file.iterator();
    while (iter.hasNext()) {
      xs.add(iter.next().toString());
    }
    return xs;
  }

  private static Path stringListToPath(
    final Path root,
    final List<String> file)
  {
    Path p = root;
    final Iterator<String> iter = file.iterator();
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

    final PosixFileAttributeView posix_view = Files.getFileAttributeView(
      file, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

    if (posix_view != null) {
      final PosixFileAttributes attribs = posix_view.readAttributes();
      size = BigInteger.valueOf(attribs.size());
      owner = attribs.owner().getName();
      group = attribs.group().getName();
      perms = attribs.permissions();
      a_time = attribs.lastAccessTime().toInstant();
      m_time = attribs.lastModifiedTime().toInstant();
      c_time = attribs.creationTime().toInstant();
    } else {
      final BasicFileAttributeView basic_view = Files.getFileAttributeView(
        file, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

      if (basic_view != null) {
        final BasicFileAttributes attribs = basic_view.readAttributes();
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

    CatalogFilesystemReader.LOG.debug("hashing {}", file);
    final CatalogFileHash hash = CatalogFileHash.fromFile(file);
    return new CatalogFileNode(
      size,
      perms,
      owner,
      group,
      id_pool.updateAndGet(x -> x.add(BigInteger.ONE)),
      a_time,
      c_time,
      m_time,
      Optional.of(hash));
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

    final PosixFileAttributeView posix_view = Files.getFileAttributeView(
      root, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

    if (posix_view != null) {
      final PosixFileAttributes attribs = posix_view.readAttributes();
      owner = attribs.owner().getName();
      group = attribs.group().getName();
      perms = attribs.permissions();
      a_time = attribs.lastAccessTime().toInstant();
      m_time = attribs.lastModifiedTime().toInstant();
      c_time = attribs.creationTime().toInstant();
    } else {
      final BasicFileAttributeView basic_view = Files.getFileAttributeView(
        root, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

      if (basic_view != null) {
        final BasicFileAttributes attribs = basic_view.readAttributes();
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

    return new CatalogDirectoryNode(
      perms,
      owner,
      group,
      id_pool.updateAndGet(x -> x.add(BigInteger.ONE)),
      a_time,
      c_time,
      m_time);
  }
}
