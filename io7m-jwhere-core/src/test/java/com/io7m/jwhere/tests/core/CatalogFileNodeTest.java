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

import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogFileNode;
import com.io7m.jwhere.core.CatalogNodeMatcherType;
import net.java.quickcheck.Generator;
import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.valid4j.exceptions.RequireViolation;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;

public final class CatalogFileNodeTest
{
  @Rule public ExpectedException expected = ExpectedException.none();

  @Test public void testMatching()
  {
    final Generator<CatalogFileNode> gen =
      CatalogFileNodeGenerator.getDefault();
    QuickCheck.forAll(
      gen, new AbstractCharacteristic<CatalogFileNode>()
      {
        @Override protected void doSpecify(final CatalogFileNode cd)
          throws Throwable
        {
          final Boolean r = cd.matchNode(
            new CatalogNodeMatcherType<Boolean, RuntimeException>()
            {
              @Override public Boolean onFile(final CatalogFileNode f)
                throws RuntimeException
              {
                return Boolean.TRUE;
              }

              @Override public Boolean onDirectory(final CatalogDirectoryNode d)
                throws RuntimeException
              {
                throw new UnreachableCodeException();
              }
            });

          Assert.assertTrue(r.booleanValue());
        }
      });
  }

  @Test public void testEqualsCases()
  {
    final Generator<CatalogFileNode> gen =
      CatalogFileNodeGenerator.getDefault();
    QuickCheck.forAll(
      gen, new AbstractCharacteristic<CatalogFileNode>()
      {
        @Override protected void doSpecify(final CatalogFileNode cd)
          throws Throwable
        {
          final CatalogFileNode ce = gen.next();
          final CatalogFileNode cf = new CatalogFileNode(
            cd.getSize(),
            cd.getPermissions(),
            cd.getOwner(),
            cd.getGroup(),
            cd.getID(),
            cd.getAccessTime(),
            cd.getCreationTime(),
            cd.getModificationTime(),
            cd.getHash());
          final CatalogFileNode cg = new CatalogFileNode(
            cd.getSize(),
            cd.getPermissions(),
            cd.getOwner(),
            cd.getGroup(),
            cd.getID(),
            cd.getAccessTime(),
            cd.getCreationTime(),
            cd.getModificationTime(),
            cd.getHash());

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

  @Test public void testBadOwnerEmpty()
  {
    this.expected.expect(RequireViolation.class);
    this.expected.expectMessage("Owner name cannot be empty");

    final Clock c = new ConstantClock(Instant.ofEpochSecond(1000L));
    new CatalogFileNode(
      BigInteger.TEN, new HashSet<>(1), "", "root", BigInteger.valueOf(
      0L), c.instant(), c.instant(), c.instant(), Optional.empty());
  }

  @Test public void testGroupNameEmpty()
  {
    this.expected.expect(RequireViolation.class);
    this.expected.expectMessage("Group name cannot be empty");

    final Clock c = new ConstantClock(Instant.ofEpochSecond(1000L));
    new CatalogFileNode(
      BigInteger.TEN, new HashSet<>(1), "root", "", BigInteger.valueOf(
      0L), c.instant(), c.instant(), c.instant(), Optional.empty());
  }
}