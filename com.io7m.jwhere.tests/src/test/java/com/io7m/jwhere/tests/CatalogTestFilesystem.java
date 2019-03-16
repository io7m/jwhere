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

package com.io7m.jwhere.tests;

import java.util.Objects;
import com.io7m.jwhere.core.CatalogFileHash;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.SortedMap;
import java.util.SortedSet;

public final class CatalogTestFilesystem implements Closeable
{
  private final FileSystem                       filesystem;
  private final SortedMap<Path, CatalogFileHash> hashes;
  private final SortedSet<Path>                  directories;

  public CatalogTestFilesystem(
    final FileSystem in_filesystem,
    final SortedMap<Path, CatalogFileHash> in_hashes,
    final SortedSet<Path> in_directories)
  {
    this.filesystem = Objects.requireNonNull(in_filesystem, "in_filesystem");
    this.hashes = Objects.requireNonNull(in_hashes, "in_hashes");
    this.directories = Objects.requireNonNull(in_directories, "in_directories");
  }

  public SortedSet<Path> getDirectories()
  {
    return this.directories;
  }

  public FileSystem getFilesystem()
  {
    return this.filesystem;
  }

  public SortedMap<Path, CatalogFileHash> getHashes()
  {
    return this.hashes;
  }

  @Override public void close()
    throws IOException
  {
    this.filesystem.close();
  }
}
