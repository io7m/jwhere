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

package com.io7m.jwhere.tests.gwhere;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.io7m.jwhere.core.CatalogDirectoryEntry;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskName;
import com.io7m.jwhere.core.CatalogFileNode;
import com.io7m.jwhere.core.CatalogJSONSerializer;
import com.io7m.jwhere.core.CatalogNodeType;
import com.io7m.jwhere.gwhere.GWhereExpectedDirectoryException;
import com.io7m.jwhere.gwhere.GWhereParserType;
import com.io7m.jwhere.gwhere.GWhereUnexpectedEOFException;
import com.io7m.jwhere.gwhere.GWhereUnreadablePermissionsException;
import com.io7m.jwhere.gwhere.GWhereUnreadableRootDirectoryException;
import org.jgrapht.graph.AsUnmodifiableGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public abstract class GWhereParserContract<P extends GWhereParserType>
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(GWhereParserContract.class);
  }

  @Rule public ExpectedException expected = ExpectedException.none();

  /**
   * Graph edges may not be named "." or ".." and any non-root node has exactly one incoming edge.
   */

  private static void checkEdgeInvariants(
    final CatalogDirectoryNode root,
    final AsUnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> g)
  {
    final var di =
      new DepthFirstIterator<>(g);
    while (di.hasNext()) {
      final var k = di.next();
      final var ie = g.incomingEdgesOf(k);
      if (k.equals(root)) {
        Assert.assertEquals(0L, (long) ie.size());
      } else {
        Assert.assertEquals(1L, (long) ie.size());
        final var ee = ie.iterator().next();
        Assert.assertNotEquals(".", ee.getName());
        Assert.assertNotEquals("..", ee.getName());
      }
    }
  }

  private static void dumpDisk(final CatalogDisk d)
    throws JsonProcessingException
  {
    final var jp = CatalogJSONSerializer.newSerializer();
    final var jom = new ObjectMapper();
    final var jw = jom.writerWithDefaultPrettyPrinter();
    LOG.debug(
      "{}", jw.writeValueAsString(jp.serializeDisk(d)));
  }

  protected abstract P getParser(String file);

  @Test
  public final void testParserEmpty()
    throws Exception
  {
    this.expected.expect(GWhereUnexpectedEOFException.class);

    final var p = this.getParser("empty.ctg");
    p.parseDisk();
  }

  @Test
  public final void testParserBadRoot()
    throws Exception
  {
    this.expected.expect(GWhereExpectedDirectoryException.class);

    final var p = this.getParser("bad-root-file.ctg");
    p.parseDisk();
  }

  @Test
  public final void testParserBadRootNamed()
    throws Exception
  {
    this.expected.expect(GWhereUnreadableRootDirectoryException.class);

    final var p = this.getParser("bad-root-named.ctg");
    p.parseDisk();
  }

  @Test
  public final void testParserBadRootMissing()
    throws Exception
  {
    this.expected.expect(GWhereUnexpectedEOFException.class);

    final var p = this.getParser("bad-root-missing.ctg");
    p.parseDisk();
  }

  @Test
  public final void testParserBadRootPermissions0()
    throws Exception
  {
    this.expected.expect(GWhereUnreadablePermissionsException.class);

    final var p = this.getParser("bad-root-perms-0.ctg");
    p.parseDisk();
  }

  @Test
  public final void testParserBadRootPermissions1()
    throws Exception
  {
    this.expected.expect(GWhereUnreadablePermissionsException.class);

    final var p = this.getParser("bad-root-perms-1.ctg");
    p.parseDisk();
  }

  @Test
  public final void testParserBadRootPermissions2()
    throws Exception
  {
    this.expected.expect(GWhereUnreadablePermissionsException.class);

    final var p = this.getParser("bad-root-perms-2.ctg");
    p.parseDisk();
  }

  @Test
  public final void testParserSimpleOne()
    throws Exception
  {
    final var p = this.getParser("one.ctg");
    final var d = p.parseDisk();

    final var meta = d.getMeta();
    Assert.assertEquals(CatalogDiskName.of("soma"), meta.getDiskName());
    Assert.assertEquals("iso9660", meta.getFilesystemType());
    Assert.assertEquals(
      CatalogDiskID.of(BigInteger.valueOf(25L)), meta.getDiskID());
    Assert.assertEquals(BigInteger.valueOf(4386893824L), meta.getSize());

    final var root = d.getFilesystemRoot();
    Assert.assertEquals("root", root.owner());
    Assert.assertEquals("root", root.group());
    Assert.assertEquals(BigInteger.valueOf(1472L), root.id());

    final Set<PosixFilePermission> perms =
      EnumSet.noneOf(PosixFilePermission.class);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.GROUP_EXECUTE);
    perms.add(PosixFilePermission.GROUP_READ);
    perms.add(PosixFilePermission.OTHERS_EXECUTE);
    perms.add(PosixFilePermission.OTHERS_READ);
    Assert.assertEquals(perms, root.permissions());

    Assert.assertEquals(
      Instant.parse("2008-12-07T11:48:09Z"), root.accessTime());
    Assert.assertEquals(
      Instant.parse("2008-12-07T11:48:09Z"), root.modificationTime());
    Assert.assertEquals(
      Instant.parse("2008-12-07T11:48:09Z"), root.creationTime());
  }

  @Test
  public final void testParserReal0()
    throws Exception
  {
    final var p = this.getParser("real-0.ctg");
    final var d = p.parseDisk();

    final var meta = d.getMeta();
    Assert.assertEquals(CatalogDiskName.of("data2"), meta.getDiskName());
    Assert.assertEquals("iso9660", meta.getFilesystemType());
    Assert.assertEquals(
      CatalogDiskID.of(BigInteger.valueOf(179L)), meta.getDiskID());
    Assert.assertEquals(BigInteger.valueOf(4411650048L), meta.getSize());

    final var root = d.getFilesystemRoot();
    Assert.assertEquals("root", root.owner());
    Assert.assertEquals("root", root.group());
    Assert.assertEquals(BigInteger.valueOf(1472L), root.id());

    final Set<PosixFilePermission> perms =
      EnumSet.noneOf(PosixFilePermission.class);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.GROUP_EXECUTE);
    perms.add(PosixFilePermission.GROUP_READ);
    perms.add(PosixFilePermission.OTHERS_EXECUTE);
    perms.add(PosixFilePermission.OTHERS_READ);
    Assert.assertEquals(perms, root.permissions());

    Assert.assertEquals(
      Instant.parse("2013-07-31T22:22:17Z"), root.accessTime());
    Assert.assertEquals(
      Instant.parse("2013-07-31T22:22:17Z"), root.modificationTime());
    Assert.assertEquals(
      Instant.parse("2013-07-31T22:22:17Z"), root.creationTime());

    final var g =
      d.getFilesystemGraph();
    checkEdgeInvariants(root, g);

    final var files = g.outgoingEdgesOf(root);
    Assert.assertEquals(9L, (long) files.size());

    for (final var e : files) {
      final var f = g.getEdgeTarget(e);
      Assert.assertEquals(CatalogFileNode.class, f.getClass());
    }
  }

  @Test
  public final void testParserReal1()
    throws Exception
  {
    final var p = this.getParser("real-1.ctg");
    final var d = p.parseDisk();

    final var meta = d.getMeta();
    Assert.assertEquals(CatalogDiskName.of("archite_2"), meta.getDiskName());
    Assert.assertEquals("iso9660", meta.getFilesystemType());
    Assert.assertEquals(
      CatalogDiskID.of(BigInteger.valueOf(174L)), meta.getDiskID());
    Assert.assertEquals(BigInteger.valueOf(4681787392L), meta.getSize());

    final var root = d.getFilesystemRoot();
    Assert.assertEquals("root", root.owner());
    Assert.assertEquals("root", root.group());
    Assert.assertEquals(BigInteger.valueOf(1472L), root.id());

    final Set<PosixFilePermission> perms =
      EnumSet.noneOf(PosixFilePermission.class);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.GROUP_EXECUTE);
    perms.add(PosixFilePermission.GROUP_READ);
    perms.add(PosixFilePermission.OTHERS_EXECUTE);
    perms.add(PosixFilePermission.OTHERS_READ);
    Assert.assertEquals(perms, root.permissions());

    Assert.assertEquals(
      Instant.parse("2012-08-04T22:56:20Z"), root.accessTime());
    Assert.assertEquals(
      Instant.parse("2012-08-04T22:56:20Z"), root.modificationTime());
    Assert.assertEquals(
      Instant.parse("2012-08-04T22:56:20Z"), root.creationTime());

    final var g =
      d.getFilesystemGraph();

    Assert.assertEquals(26L, (long) g.vertexSet().size());
    checkEdgeInvariants(root, g);

    final var files = g.outgoingEdgesOf(root);
    Assert.assertEquals(3L, (long) files.size());

    final Set<String> names = new HashSet<>(3);
    for (final var e : files) {
      names.add(e.getName());

      if ("catalog.gz".equals(e.getName())) {
        final var file = e.getTarget();
        Assert.assertEquals(CatalogFileNode.class, file.getClass());
        final var ffile = (CatalogFileNode) file;
        Assert.assertEquals("root", ffile.owner());
        Assert.assertEquals("root", ffile.group());
        Assert.assertEquals(BigInteger.valueOf(417761L), ffile.size());
        Assert.assertEquals(BigInteger.valueOf(1474L), ffile.id());
      }

      if ("arch".equals(e.getName())) {
        final var file = e.getTarget();
        Assert.assertEquals(CatalogDirectoryNode.class, file.getClass());
        Assert.assertEquals("root", file.owner());
        Assert.assertEquals("root", file.group());
        Assert.assertEquals(BigInteger.valueOf(1728L), file.id());
      }

      if ("vls".equals(e.getName())) {
        final var file = e.getTarget();
        Assert.assertEquals(CatalogDirectoryNode.class, file.getClass());
        Assert.assertEquals("root", file.owner());
        Assert.assertEquals("root", file.group());
        Assert.assertEquals(BigInteger.valueOf(1536L), file.id());
      }
    }

    Assert.assertEquals(3L, (long) names.size());
    Assert.assertTrue(names.contains("catalog.gz"));
    Assert.assertTrue(names.contains("arch"));
    Assert.assertTrue(names.contains("vls"));

    dumpDisk(d);
  }

  @Test
  public final void testParserReal2()
    throws Exception
  {
    final var p = this.getParser("real-2.ctg");
    final var d = p.parseDisk();

    final var meta = d.getMeta();
    Assert.assertEquals(CatalogDiskName.of("dk"), meta.getDiskName());
    Assert.assertEquals("iso9660", meta.getFilesystemType());
    Assert.assertEquals(
      CatalogDiskID.of(BigInteger.valueOf(10L)), meta.getDiskID());
    Assert.assertEquals(BigInteger.valueOf(10000L), meta.getSize());

    final var root = d.getFilesystemRoot();
    Assert.assertEquals("root", root.owner());
    Assert.assertEquals("root", root.group());
    Assert.assertEquals(BigInteger.valueOf(1472L), root.id());

    final Set<PosixFilePermission> perms =
      EnumSet.noneOf(PosixFilePermission.class);
    perms.add(PosixFilePermission.OWNER_EXECUTE);
    perms.add(PosixFilePermission.OWNER_READ);
    perms.add(PosixFilePermission.GROUP_EXECUTE);
    perms.add(PosixFilePermission.GROUP_READ);
    perms.add(PosixFilePermission.OTHERS_EXECUTE);
    perms.add(PosixFilePermission.OTHERS_READ);
    Assert.assertEquals(perms, root.permissions());

    Assert.assertEquals(
      Instant.parse("2012-08-04T22:56:20Z"), root.accessTime());
    Assert.assertEquals(
      Instant.parse("2012-08-04T22:56:20Z"), root.modificationTime());
    Assert.assertEquals(
      Instant.parse("2012-08-04T22:56:20Z"), root.creationTime());

    final var g =
      d.getFilesystemGraph();

    Assert.assertEquals(3L, (long) g.vertexSet().size());
    checkEdgeInvariants(root, g);

    final var files = g.outgoingEdgesOf(root);
    Assert.assertEquals(2L, (long) files.size());

    final Set<String> names = new HashSet<>(2);
    for (final var e : files) {
      names.add(e.getName());

      if ("catalog.gz".equals(e.getName())) {
        final var file = e.getTarget();
        Assert.assertEquals(CatalogFileNode.class, file.getClass());
        final var ffile = (CatalogFileNode) file;
        Assert.assertEquals("root", ffile.owner());
        Assert.assertEquals("root", ffile.group());
        Assert.assertEquals(BigInteger.valueOf(417761L), ffile.size());
        Assert.assertEquals(BigInteger.valueOf(1474L), ffile.id());
      }

      if ("empty".equals(e.getName())) {
        final var file = e.getTarget();
        Assert.assertEquals(CatalogDirectoryNode.class, file.getClass());
        Assert.assertEquals("root", file.owner());
        Assert.assertEquals("root", file.group());
        Assert.assertEquals(BigInteger.valueOf(1728L), file.id());
      }
    }

    Assert.assertEquals(2L, (long) names.size());
    Assert.assertTrue(names.contains("catalog.gz"));
    Assert.assertTrue(names.contains("empty"));
  }

  @Test
  public final void testParserCatalogReal0()
    throws Exception
  {
    final var p = this.getParser("archive-real-0.ctg");
    final var c = p.parseCatalog();

    final var disks = c.getDisks();
    Assert.assertEquals(3L, (long) disks.size());

    Assert.assertTrue(
      disks.containsKey(CatalogDiskID.of(BigInteger.valueOf(179L))));
    Assert.assertTrue(
      disks.containsKey(CatalogDiskID.of(BigInteger.valueOf(180L))));
    Assert.assertTrue(
      disks.containsKey(CatalogDiskID.of(BigInteger.valueOf(181L))));
  }
}
