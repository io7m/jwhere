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

package com.io7m.jwhere.gui.model;

import java.util.Objects;
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskID;
import net.jcip.annotations.Immutable;
import org.valid4j.Assertive;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The current immutable catalog state.
 */

@Immutable public final class CatalogState
{
  private final Catalog             catalog;
  private final List<CatalogDiskID> catalog_disks;

  private CatalogState(final Catalog c)
  {
    this.catalog = Objects.requireNonNull(c, "c");

    final SortedMap<CatalogDiskID, CatalogDisk> disks = c.getDisks();
    final List<CatalogDiskID> rows = new ArrayList<>(disks.size());
    rows.addAll(disks.keySet());
    this.catalog_disks = rows;
  }

  /**
   * @return A new empty catalog state
   */

  public static CatalogState newEmpty()
  {
    return new CatalogState(new Catalog(new TreeMap<>()));
  }

  /**
   * Construct a new catalog state with catalog {@code c}.
   *
   * @param c The new catalog
   *
   * @return A new catalog state
   */

  public static CatalogState newWithCatalog(final Catalog c)
  {
    return new CatalogState(c);
  }

  /**
   * @return The catalog
   */

  public Catalog getCatalog()
  {
    return this.catalog;
  }

  /**
   * @return The number of disks in the catalog
   */

  public int getCatalogDiskCount()
  {
    return this.catalog_disks.size();
  }

  /**
   * @param row The disk position
   *
   * @return The disk at position {@code row} in the catalog
   */

  public CatalogDiskID getCatalogDiskAt(final int row)
  {
    Assertive.ensure(row >= 0, "Row must be non-negative");
    Assertive.ensure(row < this.getCatalogDiskCount());
    return this.catalog_disks.get(row);
  }
}
