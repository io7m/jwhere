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

import com.io7m.jwhere.core.CatalogDirectoryEntry;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskBuilderType;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskName;
import com.io7m.jwhere.core.CatalogFileNode;
import com.io7m.jwhere.core.CatalogNodeDuplicateDirectoryEntryException;
import com.io7m.jwhere.core.CatalogNodeDuplicateException;
import com.io7m.jwhere.core.CatalogNodeType;
import net.java.quickcheck.Generator;
import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
import org.jgrapht.graph.UnmodifiableGraph;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.valid4j.exceptions.RequireViolation;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class CatalogDiskTest
{
  @Rule public ExpectedException expected = ExpectedException.none();

  @Test public void testEmpty()
  {
    final Clock c = new ConstantClock(Instant.ofEpochSecond(1000L));

    final CatalogDirectoryNode root = this.getRoot(c);

    final CatalogDiskBuilderType db = CatalogDisk.newDiskBuilder(
      root,
      new CatalogDiskName("example"),
      "iso9660",
      new CatalogDiskID(BigInteger.ZERO),
      BigInteger.ONE);

    final CatalogDisk cd = db.build();
    final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> g =
      cd.getFilesystemGraph();

    Assert.assertEquals(1L, (long) g.vertexSet().size());
    Assert.assertTrue(g.containsVertex(root));
  }

  private CatalogDirectoryNode getRoot(final Clock c)
  {
    return new CatalogDirectoryNode(
      new HashSet<>(1),
      "root",
      "root",
      BigInteger.valueOf(0L),
      c.instant(),
      c.instant(),
      c.instant());
  }

  @Test public void testBuilderReuse()
  {
    final Clock c = new ConstantClock(Instant.ofEpochSecond(1000L));

    final CatalogDirectoryNode root = this.getRoot(c);

    final CatalogDiskBuilderType db = CatalogDisk.newDiskBuilder(
      root,
      new CatalogDiskName("example"),
      "iso9660",
      new CatalogDiskID(BigInteger.ZERO),
      BigInteger.ONE);

    db.build();

    this.expected.expect(RequireViolation.class);
    this.expected.expectMessage("Builders cannot be reused");
    db.build();
  }

  @Test public void testDuplicateDirectoryEntry()
    throws Exception
  {
    final Clock c = new ConstantClock(Instant.ofEpochSecond(1000L));

    final CatalogDirectoryNode root = this.getRoot(c);

    final CatalogFileNode file0 = new CatalogFileNode(
      BigInteger.valueOf(120L),
      new HashSet<>(1),
      "root",
      "root",
      BigInteger.valueOf(1L),
      c.instant(),
      c.instant(),
      c.instant(),
      Optional.empty());

    final CatalogFileNode file1 = new CatalogFileNode(
      BigInteger.valueOf(130L),
      new HashSet<>(1),
      "root",
      "root",
      BigInteger.valueOf(2L),
      c.instant(),
      c.instant(),
      c.instant(),
      Optional.empty());

    final CatalogDiskBuilderType db = CatalogDisk.newDiskBuilder(
      root,
      new CatalogDiskName("example"),
      "iso9660",
      new CatalogDiskID(BigInteger.ZERO),
      BigInteger.ONE);

    db.addNode(root, "file0.txt", file0);

    this.expected.expect(CatalogNodeDuplicateDirectoryEntryException.class);
    db.addNode(root, "file0.txt", file1);
  }

  @Test public void testDuplicate()
    throws Exception
  {
    final Clock c = new ConstantClock(Instant.ofEpochSecond(1000L));

    final CatalogDirectoryNode root = this.getRoot(c);

    final CatalogDirectoryNode d0 = new CatalogDirectoryNode(
      new HashSet<>(1),
      "root",
      "root",
      BigInteger.valueOf(1L),
      c.instant(),
      c.instant(),
      c.instant());

    final CatalogDiskBuilderType db = CatalogDisk.newDiskBuilder(
      root,
      new CatalogDiskName("example"),
      "iso9660",
      new CatalogDiskID(BigInteger.ZERO),
      BigInteger.ONE);

    db.addNode(root, "d0", d0);

    this.expected.expect(CatalogNodeDuplicateException.class);
    db.addNode(root, "d1", d0);
  }

  @Test public void testPathForNodeMissing()
    throws Exception
  {
    final Clock c = new ConstantClock(Instant.ofEpochSecond(1000L));

    final CatalogDirectoryNode root = this.getRoot(c);

    final CatalogDirectoryNode d0 = new CatalogDirectoryNode(
      new HashSet<>(1),
      "root",
      "root",
      BigInteger.valueOf(1L),
      c.instant(),
      c.instant(),
      c.instant());

    final CatalogDirectoryNode d1 = new CatalogDirectoryNode(
      new HashSet<>(1),
      "root",
      "root",
      BigInteger.valueOf(2L),
      c.instant(),
      c.instant(),
      c.instant());

    final CatalogDiskBuilderType db = CatalogDisk.newDiskBuilder(
      root,
      new CatalogDiskName("example"),
      "iso9660",
      new CatalogDiskID(BigInteger.ZERO),
      BigInteger.ONE);

    db.addNode(root, "d0", d0);

    final CatalogDisk d = db.build();

    this.expected.expect(NoSuchElementException.class);
    d.getPathForNode(d1);
  }

  @Test public void testNodeForPathMissing()
    throws Exception
  {
    final Clock c = new ConstantClock(Instant.ofEpochSecond(1000L));

    final CatalogDirectoryNode root = this.getRoot(c);

    final CatalogDirectoryNode d0 = new CatalogDirectoryNode(
      new HashSet<>(1),
      "root",
      "root",
      BigInteger.valueOf(1L),
      c.instant(),
      c.instant(),
      c.instant());

    final CatalogDiskBuilderType db = CatalogDisk.newDiskBuilder(
      root,
      new CatalogDiskName("example"),
      "iso9660",
      new CatalogDiskID(BigInteger.ZERO),
      BigInteger.ONE);
    final CatalogDisk d = db.build();

    final Optional<CatalogNodeType> r_opt =
      d.getNodeForPath(Collections.singletonList("nonexistent"));
    Assert.assertFalse(r_opt.isPresent());
  }

  @Test public void testEqualsCases()
  {
    final Generator<CatalogDisk> gen = CatalogDiskGenerator.getDefault();

    QuickCheck.forAllVerbose(
      5, gen, new AbstractCharacteristic<CatalogDisk>()
      {
        @Override protected void doSpecify(final CatalogDisk cd)
          throws Throwable
        {
          final CatalogDisk ce = gen.next();
          final CatalogDisk cf = CatalogDisk.fromDisk(cd);
          final CatalogDisk cg = CatalogDisk.fromDisk(cd);

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
}