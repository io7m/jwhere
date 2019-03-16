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

import com.io7m.jwhere.core.Catalog;
import net.java.quickcheck.Generator;
import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Test;

import java.util.SortedMap;
import java.util.TreeMap;

public final class CatalogTest
{
  @Test
  public void testEqualsCases()
  {
    final Generator<Catalog> gen = CatalogGenerator.getDefault();

    QuickCheck.forAllVerbose(
      5, gen, new AbstractCharacteristic<>()
      {
        @Override
        protected void doSpecify(final Catalog cd)
          throws Throwable
        {
          final var ce = gen.next();
          final var cf = Catalog.fromCatalog(cd);
          final var cg = Catalog.fromCatalog(cd);

          // Reflexivity
          Assert.assertEquals(cd, cd);

          // Transitivity
          Assert.assertEquals(cd, cf);
          Assert.assertEquals(cf, cg);
          Assert.assertEquals(cd, cg);

          // Symmetry
          Assert.assertEquals(cd, cf);
          Assert.assertEquals(cf, cd);

          // Cases
          Assert.assertNotEquals(cd, null);
          Assert.assertNotEquals(cd, Integer.valueOf(23));
          Assert.assertNotEquals(cd, ce);

          Assert.assertNotEquals(cd.toString(), ce.toString());
          Assert.assertEquals((long) cd.hashCode(), (long) cf.hashCode());
        }
      });
  }

  @Test
  public void testEquals()
  {
    final SortedMap<Integer, Integer> map0 = new TreeMap<>();
    map0.put(Integer.valueOf(23), Integer.valueOf(24));
    final SortedMap<Integer, Integer> map1 = new TreeMap<>();
    map1.put(Integer.valueOf(19), Integer.valueOf(27));

    EqualsVerifier.forClass(Catalog.class)
      .withNonnullFields("disks")
      .withPrefabValues(SortedMap.class, map0, map1)
      .verify();
  }
}
