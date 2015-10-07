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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.io7m.jwhere.core.CatalogDirectoryEntry;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogFileHash;
import com.io7m.jwhere.core.CatalogFileNode;
import com.io7m.jwhere.core.CatalogFilesystemReader;
import com.io7m.jwhere.core.CatalogJSONSerializer;
import com.io7m.jwhere.core.CatalogJSONSerializerType;
import com.io7m.jwhere.core.CatalogNodeType;
import net.java.quickcheck.Generator;
import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
import org.jgrapht.graph.UnmodifiableGraph;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class CatalogFilesystemReaderContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CatalogFilesystemReaderContract.class);
  }

  @Rule public ExpectedException expected = ExpectedException.none();

  private static void dumpDisk(final CatalogDisk d)
    throws JsonProcessingException
  {
    final CatalogJSONSerializerType jp = CatalogJSONSerializer.newSerializer();
    final ObjectMapper jom = new ObjectMapper();
    final ObjectWriter jw = jom.writerWithDefaultPrettyPrinter();
    CatalogFilesystemReaderContract.LOG.debug(
      "{}", jw.writeValueAsString(jp.serializeDisk(d)));
  }

  protected abstract FileSystem getFileSystem();

  @Test public final void testEmpty()
    throws Exception
  {
    try (final FileSystem fs = this.getFileSystem()) {
      final Path root = fs.getRootDirectories().iterator().next();

      final CatalogDisk disk =
        CatalogFilesystemReader.newDisk("test", BigInteger.ZERO, root);

      Assert.assertEquals("test", disk.getDiskName());
      Assert.assertEquals(BigInteger.ZERO, disk.getArchiveIndex());

      final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry>
        disk_root = disk.getFilesystemGraph();
      final Set<CatalogDirectoryEntry> children =
        disk_root.outgoingEdgesOf(disk.getFilesystemRoot());

      Assert.assertEquals(0L, (long) children.size());
      CatalogFilesystemReaderContract.dumpDisk(disk);
    }
  }

  @Test public final void testNotDirectory()
    throws Exception
  {
    try (final FileSystem fs = this.getFileSystem()) {
      final Path root = fs.getRootDirectories().iterator().next();

      Files.write(
        root.resolve("file0.txt"),
        "Hello file0".getBytes(StandardCharsets.UTF_8));

      final CatalogDisk disk =
        CatalogFilesystemReader.newDisk("test", BigInteger.ZERO, root);

      this.expected.expect(NotDirectoryException.class);

      disk.getNodeForPath(Arrays.asList("file0.txt", "other"));
    }
  }

  @Test public final void testNonexistent0()
    throws Exception
  {
    try (final FileSystem fs = this.getFileSystem()) {
      final Path root = fs.getRootDirectories().iterator().next();

      final CatalogDisk disk =
        CatalogFilesystemReader.newDisk("test", BigInteger.ZERO, root);

      final Optional<CatalogNodeType> r =
        disk.getNodeForPath(Arrays.asList("nonexistent", "other"));
      Assert.assertFalse(r.isPresent());
    }
  }

  @Test public final void testNonexistent1()
    throws Exception
  {
    try (final FileSystem fs = this.getFileSystem()) {
      final Path root = fs.getRootDirectories().iterator().next();

      final Path base = root.resolve("subdir");
      Files.createDirectory(base);

      final CatalogDisk disk =
        CatalogFilesystemReader.newDisk("test", BigInteger.ZERO, root);

      final Optional<CatalogNodeType> r =
        disk.getNodeForPath(Arrays.asList("subdir", "nonexistent"));
      Assert.assertFalse(r.isPresent());
    }
  }

  @Test public final void testFiles0()
    throws Exception
  {
    try (final FileSystem fs = this.getFileSystem()) {
      final Path root = fs.getRootDirectories().iterator().next();

      Files.write(
        root.resolve("file0.txt"),
        "Hello file0".getBytes(StandardCharsets.UTF_8));
      Files.write(
        root.resolve("file1.txt"),
        "Hello file1".getBytes(StandardCharsets.UTF_8));
      Files.write(
        root.resolve("file2.txt"),
        "Hello file2".getBytes(StandardCharsets.UTF_8));

      final CatalogDisk disk =
        CatalogFilesystemReader.newDisk("test", BigInteger.ZERO, root);

      Assert.assertEquals("test", disk.getDiskName());
      Assert.assertEquals(BigInteger.ZERO, disk.getArchiveIndex());

      final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry>
        disk_root = disk.getFilesystemGraph();
      final Set<CatalogDirectoryEntry> children =
        disk_root.outgoingEdgesOf(disk.getFilesystemRoot());

      Assert.assertEquals(3L, (long) children.size());

      CatalogFileNode file0 = null;
      CatalogFileNode file1 = null;
      CatalogFileNode file2 = null;

      for (final CatalogDirectoryEntry e : children) {
        Assert.assertEquals(CatalogFileNode.class, e.getTarget().getClass());
        final CatalogFileNode t = (CatalogFileNode) e.getTarget();
        final Optional<CatalogFileHash> ho = t.getHash();
        Assert.assertTrue(ho.isPresent());
        final CatalogFileHash h = ho.get();

        Assert.assertEquals("SHA-256", h.getAlgorithm());

        if ("file0.txt".equals(e.getName())) {
          file0 = t;
          Assert.assertEquals(
            h.getValue(),
            ("c3f64406d73acc591c90b5f4616175ce26c96f1f15cba9ef57de05b2e77526f0"
             + "").toUpperCase());
        }

        if ("file1.txt".equals(e.getName())) {
          file1 = t;
          Assert.assertEquals(
            h.getValue(),
            ("610c2c8120e3c8c6ed71714853d4d1b1add2ec1e1e8b85477049324f9d0d855e"
             + "").toUpperCase());
        }

        if ("file2.txt".equals(e.getName())) {
          file2 = t;
          Assert.assertEquals(
            h.getValue(),
            ("e0fe5b86a8eafeab4ead9f8ca5c7aae07e6ad10cffbdd63228df4d4beb2db15d"
             + "").toUpperCase());
        }
      }

      Assert.assertNotNull(file0);
      Assert.assertNotNull(file1);
      Assert.assertNotNull(file2);

      final Optional<CatalogNodeType> file0r_opt =
        disk.getNodeForPath(Collections.singletonList("file0.txt"));
      final Optional<CatalogNodeType> file1r_opt =
        disk.getNodeForPath(Collections.singletonList("file1.txt"));
      final Optional<CatalogNodeType> file2r_opt =
        disk.getNodeForPath(Collections.singletonList("file2.txt"));

      Assert.assertTrue(file0r_opt.isPresent());
      Assert.assertTrue(file1r_opt.isPresent());
      Assert.assertTrue(file2r_opt.isPresent());

      final CatalogNodeType file0r = file0r_opt.get();
      final CatalogNodeType file1r = file1r_opt.get();
      final CatalogNodeType file2r = file2r_opt.get();

      Assert.assertEquals(file0, file0r);
      Assert.assertEquals(file1, file1r);
      Assert.assertEquals(file2, file2r);

      CatalogFilesystemReaderContract.dumpDisk(disk);
    }
  }

  @Test public final void testNodePaths()
    throws Exception
  {
    final Generator<List<String>> pg = PathListGenerator.getDefault();

    try (final FileSystem fs = CatalogFilesystemReaderContract.this
      .getFileSystem()) {
      final Path root = fs.getRootDirectories().iterator().next();
      final List<List<String>> paths = new ArrayList<>(100);

      QuickCheck.forAll(
        100, pg, new AbstractCharacteristic<List<String>>()
        {
          @Override protected void doSpecify(final List<String> p)
            throws Throwable
          {
            final Path p0 =
              p.stream().reduce(root, Path::resolve, Path::resolve);

            p.add("file.txt");
            paths.add(new ArrayList<>(p));

            Files.createDirectories(p0);
            Files.write(
              p0.resolve("file.txt"),
              "Hello file0".getBytes(StandardCharsets.UTF_8));
          }
        });

      final CatalogDisk disk =
        CatalogFilesystemReader.newDisk("test", BigInteger.ZERO, root);

      for (final List<String> p : paths) {
        final Optional<CatalogNodeType> node_opt = disk.getNodeForPath(p);
        Assert.assertTrue(node_opt.isPresent());
        final CatalogNodeType node = node_opt.get();
        Assert.assertEquals(CatalogFileNode.class, node.getClass());
        final List<String> q = disk.getPathForNode(node);
        Assert.assertEquals(p, q);
      }
    }
  }
}
