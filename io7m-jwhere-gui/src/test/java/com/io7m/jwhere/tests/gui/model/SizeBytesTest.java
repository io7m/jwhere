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

package com.io7m.jwhere.tests.gui.model;

import com.io7m.jwhere.gui.model.SizeBytes;
import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
import net.java.quickcheck.generator.support.LongGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

public final class SizeBytesTest
{
  @Test public void testEquals()
  {
    final LongGenerator lg = new LongGenerator();
    QuickCheck.forAll(
      lg, new AbstractCharacteristic<Long>()
      {
        @Override protected void doSpecify(final Long any)
          throws Throwable
        {
          final BigInteger v0 = BigInteger.valueOf(any.longValue());
          final BigInteger v1 = BigInteger.valueOf(any.longValue());
          final BigInteger v2 = BigInteger.valueOf(any.longValue());
          final BigInteger v3 = BigInteger.valueOf(lg.next().longValue());

          final SizeBytes s0 = new SizeBytes(v0);
          final SizeBytes s1 = new SizeBytes(v1);
          final SizeBytes s2 = new SizeBytes(v2);
          final SizeBytes s3 = new SizeBytes(v3);

          // Reflexive
          Assert.assertEquals(s0, s0);

          // Symmetric
          Assert.assertEquals(s0, s1);
          Assert.assertEquals(s1, s0);

          // Transitive
          Assert.assertEquals(s0, s1);
          Assert.assertEquals(s1, s2);
          Assert.assertEquals(s0, s2);

          Assert.assertEquals((long) s0.hashCode(), (long) s1.hashCode());
          Assert.assertEquals(s0.toString(), s1.toString());
          Assert.assertNotEquals(s0.toString(), s3.toString());

          Assert.assertNotEquals(s0, s3);
          Assert.assertNotEquals(s0, null);
          Assert.assertNotEquals(s0, Integer.valueOf(23));
        }
      });
  }

  @Test public void testValue()
  {
    final LongGenerator lg = new LongGenerator();
    QuickCheck.forAll(
      lg, new AbstractCharacteristic<Long>()
      {
        @Override protected void doSpecify(final Long any)
          throws Throwable
        {
          final BigInteger v0 = BigInteger.valueOf(any.longValue());
          final BigInteger v1 = BigInteger.valueOf(lg.next().longValue());

          final SizeBytes s0 = new SizeBytes(v0);
          final SizeBytes s1 = new SizeBytes(v1);

          Assert.assertEquals(s0.getValue(), v0);
          Assert.assertEquals(s1.getValue(), v1);

          Assert.assertNotEquals(s0.getValue(), v1);
          Assert.assertNotEquals(s1.getValue(), v0);
        }
      });
  }

  @Test public void testHumanString()
  {
    Assert.assertEquals(
      "1b", new SizeBytes(BigInteger.valueOf(1L)).toHumanString());
    Assert.assertEquals(
      "1.00kb", new SizeBytes(BigInteger.valueOf(1_000L)).toHumanString());
    Assert.assertEquals(
      "1.00mb", new SizeBytes(BigInteger.valueOf(1_000_000L)).toHumanString());
  }
}
