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

package com.io7m.jwhere.tests.core;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskID;
import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.IntegerGenerator;

import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public final class CatalogGenerator implements Generator<Catalog>
{
  private final Generator<CatalogDisk> disk_gen;
  private final Generator<Integer> count_gen;

  public CatalogGenerator(
    final Generator<Integer> in_count_gen,
    final Generator<CatalogDisk> in_disk_gen)
  {
    this.count_gen = Objects.requireNonNull(in_count_gen, "in_count_gen");
    this.disk_gen = Objects.requireNonNull(in_disk_gen, "in_disk_gen");
  }

  public static CatalogGenerator getDefault()
  {
    return new CatalogGenerator(
      new IntegerGenerator(1, 8), CatalogDiskGenerator.getDefault());
  }

  @Override
  public Catalog next()
  {
    final SortedMap<CatalogDiskID, CatalogDisk> disks = new TreeMap<>();

    final var count = this.count_gen.next().intValue();
    for (var index = 0; index < count; ++index) {
      final var d = this.disk_gen.next();
      final var meta = d.getMeta();
      final var disk_index = meta.getDiskID();
      Preconditions.checkPrecondition(
        disks.containsKey(disk_index) == false,
        "Disk map must contain disk index");
      disks.put(disk_index, d);
    }

    return new Catalog(disks);
  }
}
