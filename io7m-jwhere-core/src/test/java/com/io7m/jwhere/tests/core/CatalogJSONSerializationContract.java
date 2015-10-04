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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogJSONParserType;
import com.io7m.jwhere.core.CatalogJSONSerializerType;
import net.java.quickcheck.Generator;
import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CatalogJSONSerializationContract<S extends
  CatalogJSONSerializerType, P extends CatalogJSONParserType>
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CatalogJSONSerializationContract.class);
  }

  protected abstract P getParser();

  protected abstract S getSerializer();

  @Test public final void testSerializationRoundTrip()
  {
    final S s = this.getSerializer();
    final P p = this.getParser();
    final Generator<CatalogDisk> g = CatalogDiskGenerator.getDefault();

    QuickCheck.forAll(
      10, g, new AbstractCharacteristic<CatalogDisk>()
      {
        @Override protected void doSpecify(final CatalogDisk d0)
          throws Throwable
        {
          final ObjectNode s0 = s.serializeDisk(d0);
          final CatalogDisk d1 = p.parseDisk(s0);
          Assert.assertEquals(d0, d1);
          final ObjectNode s1 = s.serializeDisk(d1);
          final CatalogDisk d2 = p.parseDisk(s1);
          Assert.assertEquals(d0, d2);
        }
      });
  }
}
