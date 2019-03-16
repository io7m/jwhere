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

import com.io7m.jnull.NullCheck;

import java.nio.file.Path;

/**
 * Where to save a catalog and how.
 */

public final class CatalogSaveSpecification
{
  private final Path     path;
  private final Compress compression;

  @Override public String toString()
  {
    final StringBuilder sb = new StringBuilder("CatalogSaveSpecification{");
    sb.append("compression=").append(this.compression);
    sb.append(", path=").append(this.path);
    sb.append('}');
    return sb.toString();
  }

  /**
   * Construct a specification.
   *
   * @param in_compression The compression setting
   * @param in_path        The path
   */

  public CatalogSaveSpecification(
    final Compress in_compression,
    final Path in_path)
  {
    this.compression = NullCheck.notNull(in_compression);
    this.path = NullCheck.notNull(in_path);
  }

  /**
   * @return The compression setting for the file
   */

  public Compress getCompression()
  {
    return this.compression;
  }

  /**
   * @return The file path
   */

  public Path getPath()
  {
    return this.path;
  }

  /**
   * Compression specification.
   */

  public enum Compress
  {
    /**
     * The output will not be compressed.
     */

    COMPRESS_NONE,

    /**
     * The output will be compressed with GZip.
     */

    COMPRESS_GZIP
  }
}
