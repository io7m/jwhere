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

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A set of catalogued disks.
 */

public final class Catalog
{
  private final SortedMap<CatalogDiskID, CatalogDisk> disks;

  /**
   * Construct a catalog.
   *
   * @param in_disks The disks, by archive index
   */

  public Catalog(final SortedMap<CatalogDiskID, CatalogDisk> in_disks)
  {
    this.disks = NullCheck.notNull(in_disks);
  }

  /**
   * Produce a copy of the given catalog.
   *
   * @param c The catalog
   *
   * @return A new catalog
   */

  public static Catalog fromCatalog(final Catalog c)
  {
    NullCheck.notNull(c);
    return new Catalog(new TreeMap<>(c.disks));
  }

  @Override public String toString()
  {
    final StringBuilder sb = new StringBuilder("Catalog{");
    sb.append("disks=").append(this.disks);
    sb.append('}');
    return sb.toString();
  }

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final Catalog catalog = (Catalog) o;
    return this.getDisks().equals(catalog.getDisks());
  }

  @Override public int hashCode()
  {
    return this.getDisks().hashCode();
  }

  /**
   * @return The set of disks in the catalog
   */

  public SortedMap<CatalogDiskID, CatalogDisk> getDisks()
  {
    return this.disks;
  }
}
