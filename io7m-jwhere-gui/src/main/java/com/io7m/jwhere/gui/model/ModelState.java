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

import com.io7m.jnull.NullCheck;
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskID;
import net.jcip.annotations.Immutable;
import org.valid4j.Assertive;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

@Immutable final class ModelState
{
  private final Catalog             catalog;
  private final List<CatalogDiskID> catalog_disks;
  private final BigInteger          revision;

  private ModelState(
    final Catalog in_catalog,
    final List<CatalogDiskID> in_catalog_disks,
    final BigInteger in_revision)
  {
    this.catalog = NullCheck.notNull(in_catalog);
    this.catalog_disks = NullCheck.notNull(in_catalog_disks);
    this.revision = NullCheck.notNull(in_revision);
  }

  static ModelState newEmpty(final BigInteger in_revision)
  {
    return new ModelState(
      new Catalog(new TreeMap<>()), new ArrayList<>(1), in_revision);
  }

  static ModelState newCatalog(
    final Catalog c,
    final BigInteger in_revision)
  {
    NullCheck.notNull(c);
    NullCheck.notNull(in_revision);

    final SortedMap<CatalogDiskID, CatalogDisk> disks = c.getDisks();
    final List<CatalogDiskID> rows = new ArrayList<>(disks.size());
    rows.addAll(disks.keySet());
    return new ModelState(c, rows, in_revision);
  }

  public Catalog getCatalog()
  {
    return this.catalog;
  }

  public BigInteger getRevision()
  {
    return this.revision;
  }

  ModelState withNewCatalog(
    final Catalog c)
  {
    NullCheck.notNull(c);
    return ModelState.newCatalog(c, this.revision.add(BigInteger.ONE));
  }

  public int getCatalogDiskCount()
  {
    return this.catalog_disks.size();
  }

  public CatalogDiskID getCatalogDiskAt(final int row)
  {
    Assertive.ensure(row >= 0, "Row must be non-negative");
    Assertive.ensure(row < this.getCatalogDiskCount());
    return this.catalog_disks.get(row);
  }
}
