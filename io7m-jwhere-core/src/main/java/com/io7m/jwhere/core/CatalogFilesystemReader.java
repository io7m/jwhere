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

import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valid4j.Assertive;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.DirectoryStream;
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
import java.security.DigestException;
import java.time.Instant;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
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
   * @param index     The disk archive number
   * @param root      The root directory
   *
   * @return A new disk
   *
   * @throws IOException      On I/O errors
   * @throws CatalogException On other catalog-related errors
   */

  public static CatalogDisk newDisk(
    final String disk_name,
    final BigInteger index,
    final Path root)
    throws IOException, CatalogException
  {
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
      try (final DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
        final Iterator<Path> iter = ds.iterator();
        if (iter.hasNext()) {
          final Path next = iter.next();
          CatalogFilesystemReader.LOG.debug("starting: {}", next);

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
                  }

                  if (fn != null) {
                    final CatalogDirectoryNode current = dirs.peek();
                    final CatalogDirectoryNode new_dir =
                      CatalogFilesystemReader.onDirectory(id_pool, dir);

                    final String name = fn.toString();
                    db.addNode(current, name, new_dir);
                    dirs.push(new_dir);
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
                } catch (final CatalogNodeException | DigestException e) {
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
                dirs.pop();
                return FileVisitResult.CONTINUE;
              }
            });
        }
      }

      return db.build();
    } catch (final IOException e) {
      if (e.getCause() instanceof CatalogException) {
        throw (CatalogException) e.getCause();
      } else {
        throw e;
      }
    }
  }

  private static CatalogFileNode onFile(
    final AtomicReference<BigInteger> id_pool,
    final Path file)
    throws IOException, DigestException
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
