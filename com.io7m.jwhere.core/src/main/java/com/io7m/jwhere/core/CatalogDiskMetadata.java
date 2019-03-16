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

import java.math.BigInteger;
import java.util.Objects;

/**
 * Disk metadata.
 */

public final class CatalogDiskMetadata
{
  private final CatalogDiskName disk_name;
  private final String fs_type;
  private final CatalogDiskID index;
  private final BigInteger size;

  /**
   * Disk metadata.
   *
   * @param in_disk_name The disk name
   * @param in_fs_type   The disk filesystem type
   * @param in_index     The disk ID
   * @param in_size      The disk size
   */

  public CatalogDiskMetadata(
    final CatalogDiskName in_disk_name,
    final String in_fs_type,
    final CatalogDiskID in_index,
    final BigInteger in_size)
  {
    this.disk_name = Objects.requireNonNull(in_disk_name, "in_disk_name");
    this.fs_type = Objects.requireNonNull(in_fs_type, "in_fs_type");
    this.index = Objects.requireNonNull(in_index, "in_index");
    this.size = Objects.requireNonNull(in_size, "in_size");
  }

  /**
   * @return The name of the disk
   */

  public CatalogDiskName getDiskName()
  {
    return this.disk_name;
  }

  /**
   * @return The disk filesystem type
   */

  public String getFilesystemType()
  {
    return this.fs_type;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || !Objects.equals(this.getClass(), o.getClass())) {
      return false;
    }

    final var that = (CatalogDiskMetadata) o;
    return this.disk_name.equals(that.disk_name)
      && this.fs_type.equals(that.fs_type)
      && this.index.equals(that.index)
      && this.getSize().equals(that.getSize());
  }

  @Override
  public String toString()
  {
    final var sb = new StringBuilder("CatalogDiskMetadata{");
    sb.append("disk_name=").append(this.disk_name);
    sb.append(", fs_type='").append(this.fs_type).append('\'');
    sb.append(", index=").append(this.index);
    sb.append(", size=").append(this.size);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public int hashCode()
  {
    var result = this.disk_name.hashCode();
    result = 31 * result + this.fs_type.hashCode();
    result = 31 * result + this.index.hashCode();
    result = 31 * result + this.getSize().hashCode();
    return result;
  }

  /**
   * @return The disk ID
   */

  public CatalogDiskID getDiskID()
  {
    return this.index;
  }

  /**
   * @return The disk size
   */

  public BigInteger getSize()
  {
    return this.size;
  }
}
