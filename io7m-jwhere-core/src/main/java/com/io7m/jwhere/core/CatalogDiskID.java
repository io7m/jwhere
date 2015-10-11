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
import com.io7m.jnull.Nullable;

import java.math.BigInteger;

/**
 * A unique (within a single catalog) identifier for a disk.
 */

public final class CatalogDiskID implements Comparable<CatalogDiskID>
{
  private final BigInteger value;

  /**
   * Construct a value.
   *
   * @param in_value The value
   */

  public CatalogDiskID(final BigInteger in_value)
  {
    this.value = NullCheck.notNull(in_value);
  }

  /**
   * @return The actual value
   */

  public BigInteger getValue()
  {
    return this.value;
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final CatalogDiskID other = (CatalogDiskID) o;
    return this.value.equals(other.value);
  }

  @Override public String toString()
  {
    return this.value.toString();
  }

  @Override public int hashCode()
  {
    return this.value.hashCode();
  }

  @Override public int compareTo(final @Nullable CatalogDiskID o)
  {
    return this.getValue().compareTo(NullCheck.notNull(o).getValue());
  }
}
