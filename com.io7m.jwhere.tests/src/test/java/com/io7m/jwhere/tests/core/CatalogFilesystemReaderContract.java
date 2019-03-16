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
import com.gs.collections.api.multimap.MutableMultimap;
import com.gs.collections.impl.multimap.bag.HashBagMultimap;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskName;
import com.io7m.jwhere.core.CatalogFileNode;
import com.io7m.jwhere.core.CatalogFilesystemReader;
import com.io7m.jwhere.core.CatalogIgnoreAccessTime;
import com.io7m.jwhere.core.CatalogJSONSerializer;
import com.io7m.jwhere.core.CatalogVerificationChangedHash;
import com.io7m.jwhere.core.CatalogVerificationChangedMetadata;
import com.io7m.jwhere.core.CatalogVerificationChangedType;
import com.io7m.jwhere.core.CatalogVerificationListenerType;
import com.io7m.jwhere.core.CatalogVerificationMetadataField;
import com.io7m.jwhere.core.CatalogVerificationReportItemErrorType;
import com.io7m.jwhere.core.CatalogVerificationReportItemOKType;
import com.io7m.jwhere.core.CatalogVerificationReportSettings;
import com.io7m.jwhere.core.CatalogVerificationUncataloguedItem;
import com.io7m.jwhere.core.CatalogVerificationVanishedItem;
import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
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
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    final var jp = CatalogJSONSerializer.newSerializer();
    final var jom = new ObjectMapper();
    final var jw = jom.writerWithDefaultPrettyPrinter();
    LOG.debug(
      "{}", jw.writeValueAsString(jp.serializeDisk(d)));
  }

  private static List<String> pathToStringList(final Path file)
  {
    final List<String> xs = new ArrayList<>(16);
    final var iter = file.iterator();
    while (iter.hasNext()) {
      xs.add(iter.next().toString());
    }
    return xs;
  }

  private static boolean isTypeChangedError(
    final CatalogVerificationReportItemErrorType x)
  {
    return x instanceof CatalogVerificationChangedType;
  }

  private static boolean isFileVanishedError(
    final CatalogVerificationReportItemErrorType x)
  {
    return x instanceof CatalogVerificationVanishedItem;
  }

  private static boolean isFileUncataloguedError(
    final CatalogVerificationReportItemErrorType x)
  {
    return x instanceof CatalogVerificationUncataloguedItem;
  }

  private static boolean isCreationTimeError(
    final CatalogVerificationReportItemErrorType x)
  {
    final var v =
      CatalogVerificationMetadataField.CREATION_TIME;
    return isErrorWithField(x, v);
  }

  private static boolean isErrorWithField(
    final CatalogVerificationReportItemErrorType x,
    final CatalogVerificationMetadataField v)
  {
    if (x instanceof CatalogVerificationChangedMetadata) {
      final var xc =
        (CatalogVerificationChangedMetadata) x;
      return v.equals(xc.field());
    } else {
      return false;
    }
  }

  private static boolean isModificationTimeError(
    final CatalogVerificationReportItemErrorType x)
  {
    final var v =
      CatalogVerificationMetadataField.MODIFICATION_TIME;
    return isErrorWithField(x, v);
  }

  private static boolean isAccessTimeError(
    final CatalogVerificationReportItemErrorType x)
  {
    return isErrorWithField(
      x, CatalogVerificationMetadataField.ACCESS_TIME);
  }

  protected abstract FileSystem getFileSystem();

  protected abstract CatalogFilesystemProfile getFilesystemProfile();

  @Test
  public final void testEmpty()
    throws Exception
  {
    try (final var fs = this.getFileSystem()) {
      final var root = fs.getRootDirectories().iterator().next();

      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of("test"), CatalogDiskID.of(BigInteger.ZERO), root);

      final var meta = disk.getMeta();
      Assert.assertEquals(CatalogDiskName.of("test"), meta.getDiskName());
      Assert.assertEquals(CatalogDiskID.of(BigInteger.ZERO), meta.getDiskID());

      final var
        disk_root = disk.getFilesystemGraph();
      final var children =
        disk_root.outgoingEdgesOf(disk.getFilesystemRoot());

      Assert.assertEquals(0L, (long) children.size());
      dumpDisk(disk);
    }
  }

  @Test
  public final void testNotDirectory()
    throws Exception
  {
    try (final var fs = this.getFileSystem()) {
      final var root = fs.getRootDirectories().iterator().next();

      Files.write(
        root.resolve("file0.txt"),
        "Hello file0".getBytes(StandardCharsets.UTF_8));

      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of("test"), CatalogDiskID.of(BigInteger.ZERO), root);

      this.expected.expect(NotDirectoryException.class);

      disk.getNodeForPath(Arrays.asList("file0.txt", "other"));
    }
  }

  @Test
  public final void testNonexistent0()
    throws Exception
  {
    try (final var fs = this.getFileSystem()) {
      final var root = fs.getRootDirectories().iterator().next();

      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of("test"), CatalogDiskID.of(BigInteger.ZERO), root);

      final var r =
        disk.getNodeForPath(Arrays.asList("nonexistent", "other"));
      Assert.assertFalse(r.isPresent());
    }
  }

  @Test
  public final void testNonexistent1()
    throws Exception
  {
    try (final var fs = this.getFileSystem()) {
      final var root = fs.getRootDirectories().iterator().next();

      final var base = root.resolve("subdir");
      Files.createDirectory(base);

      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of("test"), CatalogDiskID.of(BigInteger.ZERO), root);

      final var r =
        disk.getNodeForPath(Arrays.asList("subdir", "nonexistent"));
      Assert.assertFalse(r.isPresent());
    }
  }

  @Test
  public final void testFiles0()
    throws Exception
  {
    try (final var fs = this.getFileSystem()) {
      final var root = fs.getRootDirectories().iterator().next();

      Files.write(
        root.resolve("file0.txt"),
        "Hello file0".getBytes(StandardCharsets.UTF_8));
      Files.write(
        root.resolve("file1.txt"),
        "Hello file1".getBytes(StandardCharsets.UTF_8));
      Files.write(
        root.resolve("file2.txt"),
        "Hello file2".getBytes(StandardCharsets.UTF_8));

      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of("test"), CatalogDiskID.of(BigInteger.ZERO), root);

      final var meta = disk.getMeta();
      Assert.assertEquals(CatalogDiskName.of("test"), meta.getDiskName());
      Assert.assertEquals(CatalogDiskID.of(BigInteger.ZERO), meta.getDiskID());

      final var
        disk_root = disk.getFilesystemGraph();
      final var children =
        disk_root.outgoingEdgesOf(disk.getFilesystemRoot());

      Assert.assertEquals(3L, (long) children.size());

      CatalogFileNode file0 = null;
      CatalogFileNode file1 = null;
      CatalogFileNode file2 = null;

      for (final var e : children) {
        Assert.assertEquals(CatalogFileNode.class, e.getTarget().getClass());
        final var t = (CatalogFileNode) e.getTarget();
        final var ho = t.hash();
        Assert.assertTrue(ho.isPresent());
        final var h = ho.get();

        Assert.assertEquals("SHA-256", h.algorithm());

        if ("file0.txt".equals(e.getName())) {
          file0 = t;
          Assert.assertEquals(
            h.value(),
            ("c3f64406d73acc591c90b5f4616175ce26c96f1f15cba9ef57de05b2e77526f0"
              + "").toUpperCase());
        }

        if ("file1.txt".equals(e.getName())) {
          file1 = t;
          Assert.assertEquals(
            h.value(),
            ("610c2c8120e3c8c6ed71714853d4d1b1add2ec1e1e8b85477049324f9d0d855e"
              + "").toUpperCase());
        }

        if ("file2.txt".equals(e.getName())) {
          file2 = t;
          Assert.assertEquals(
            h.value(),
            ("e0fe5b86a8eafeab4ead9f8ca5c7aae07e6ad10cffbdd63228df4d4beb2db15d"
              + "").toUpperCase());
        }
      }

      Assert.assertNotNull(file0);
      Assert.assertNotNull(file1);
      Assert.assertNotNull(file2);

      final var file0r_opt =
        disk.getNodeForPath(Collections.singletonList("file0.txt"));
      final var file1r_opt =
        disk.getNodeForPath(Collections.singletonList("file1.txt"));
      final var file2r_opt =
        disk.getNodeForPath(Collections.singletonList("file2.txt"));

      Assert.assertTrue(file0r_opt.isPresent());
      Assert.assertTrue(file1r_opt.isPresent());
      Assert.assertTrue(file2r_opt.isPresent());

      final var file0r = file0r_opt.get();
      final var file1r = file1r_opt.get();
      final var file2r = file2r_opt.get();

      Assert.assertEquals(file0, file0r);
      Assert.assertEquals(file1, file1r);
      Assert.assertEquals(file2, file2r);

      dumpDisk(disk);
    }
  }

  @Test
  public final void testHashes()
    throws Exception
  {
    final var fs_gen =
      new CatalogTestFilesystemGenerator(this.getFilesystemProfile());

    QuickCheck.forAll(
      10, fs_gen, new AbstractCharacteristic<>()
      {
        @Override
        protected void doSpecify(final CatalogTestFilesystem tfs)
          throws Throwable
        {
          try (tfs) {
            final var fs = tfs.getFilesystem();
            final var hashes = tfs.getHashes();

            final var root = fs.getRootDirectories().iterator().next();
            final var disk = CatalogFilesystemReader.newDisk(
              CatalogDiskName.of("test"),
              CatalogDiskID.of(BigInteger.ZERO),
              root);

            for (final var p : hashes.keySet()) {
              final var hash = hashes.get(p);
              final var ps =
                pathToStringList(p);
              final var node_opt =
                disk.getNodeForPath(ps);
              Assert.assertTrue(node_opt.isPresent());
              final var node = node_opt.get();
              Assert.assertEquals(CatalogFileNode.class, node.getClass());
              final var node_file = (CatalogFileNode) node;

              final var node_file_hash_opt = node_file.hash();
              Assert.assertTrue(node_file_hash_opt.isPresent());
              final var node_file_hash = node_file_hash_opt.get();
              Assert.assertEquals(hash, node_file_hash);
            }
          }
        }
      });
  }

  @Test
  public final void testNodePaths()
    throws Exception
  {
    final var fs_gen =
      new CatalogTestFilesystemGenerator(this.getFilesystemProfile());

    QuickCheck.forAll(
      10, fs_gen, new AbstractCharacteristic<>()
      {
        @Override
        protected void doSpecify(final CatalogTestFilesystem tfs)
          throws Throwable
        {
          try (tfs) {
            final var fs = tfs.getFilesystem();
            final var hashes = tfs.getHashes();

            final var root = fs.getRootDirectories().iterator().next();
            final var disk = CatalogFilesystemReader.newDisk(
              CatalogDiskName.of("test"),
              CatalogDiskID.of(BigInteger.ZERO),
              root);

            for (final var p : hashes.keySet()) {
              final var hash = hashes.get(p);
              final var p0 =
                pathToStringList(p);
              final var node_opt =
                disk.getNodeForPath(p0);
              Assert.assertTrue(node_opt.isPresent());
              final var node = node_opt.get();
              final var p1 = disk.getPathForNode(node);
              Assert.assertEquals(p0, p1);
            }
          }
        }
      });
  }

  @Test
  public final void testVerification()
    throws Exception
  {
    final var fs_gen =
      new CatalogTestFilesystemGenerator(this.getFilesystemProfile());

    final var settings =
      CatalogVerificationReportSettings.builder()
        .setIgnoreAccessTime(CatalogIgnoreAccessTime.IGNORE_ACCESS_TIME)
        .build();

    QuickCheck.forAll(
      100, fs_gen, new AbstractCharacteristic<>()
      {
        @Override
        protected void doSpecify(final CatalogTestFilesystem tfs)
          throws Throwable
        {
          try (tfs) {
            final var fs = tfs.getFilesystem();
            final var hashes = tfs.getHashes();
            final var directories = tfs.getDirectories();

            final var root = fs.getRootDirectories().iterator().next();
            final var disk = CatalogFilesystemReader.newDisk(
              CatalogDiskName.of("test"),
              CatalogDiskID.of(BigInteger.ZERO),
              root);

            final var listener = new CheckedListener();

            CatalogFilesystemReader.verifyDisk(
              disk, settings, root, listener);

            final var expected_size = hashes.size() + directories.size();
            Assert.assertTrue(listener.errors.isEmpty());
            Assert.assertEquals(
              (long) expected_size, (long) listener.valids.size());

          }
        }
      });
  }

  @Test
  public final void testVerificationEmpty()
    throws Exception
  {
    final var settings =
      CatalogVerificationReportSettings.builder()
        .setIgnoreAccessTime(CatalogIgnoreAccessTime.IGNORE_ACCESS_TIME)
        .build();

    try (final var fs = this.getFileSystem()) {
      final var root = fs.getRootDirectories().iterator().next();
      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of("test"), CatalogDiskID.of(BigInteger.ZERO), root);

      final var listener = new CheckedListener();

      CatalogFilesystemReader.verifyDisk(
        disk, settings, root, listener);

      Assert.assertTrue(listener.errors.isEmpty());
      Assert.assertEquals(1L, (long) listener.valids.size());
    }
  }

  @Test
  public final void testVerificationTimeAccessChanged()
    throws Exception
  {
    final var settings =
      CatalogVerificationReportSettings.builder()
        .setIgnoreAccessTime(CatalogIgnoreAccessTime.DO_NOT_IGNORE_ACCESS_TIME)
        .build();

    try (final var fs = this.getFileSystem()) {
      final var root = fs.getRootDirectories().iterator().next();
      final var file = root.relativize(root.resolve("file.txt"));
      Files.createFile(file);

      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of("test"), CatalogDiskID.of(BigInteger.ZERO), root);

      Thread.sleep(1000L);
      Files.readAllBytes(file);

      final var listener = new CheckedListener();

      CatalogFilesystemReader.verifyDisk(
        disk, settings, root, listener);

      Assert.assertEquals(1L, (long) listener.errors.size());
      Assert.assertTrue(listener.errors.containsKey(file));

      final var
        file_errors = listener.errors.get(file);
      final var file_wanted_errors =
        file_errors.stream()
          .filter(CatalogFilesystemReaderContract::isAccessTimeError)
          .collect(Collectors.toSet());

      Assert.assertEquals(1L, (long) file_wanted_errors.size());
      Assert.assertEquals(1L, (long) listener.valids.size());
    }
  }

  @Test
  public final void testVerificationTimeModificationChanged()
    throws Exception
  {
    final var settings =
      CatalogVerificationReportSettings.builder()
        .setIgnoreAccessTime(CatalogIgnoreAccessTime.IGNORE_ACCESS_TIME)
        .build();

    try (final var fs = this.getFileSystem()) {
      final var root = fs.getRootDirectories().iterator().next();
      final var file = root.relativize(root.resolve("file.txt"));
      Files.createFile(file);

      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of("test"), CatalogDiskID.of(BigInteger.ZERO), root);

      Files.setLastModifiedTime(file, FileTime.from(Instant.ofEpochMilli(1L)));

      final var listener = new CheckedListener();

      CatalogFilesystemReader.verifyDisk(
        disk, settings, root, listener);

      Assert.assertEquals(1L, (long) listener.errors.size());
      Assert.assertTrue(listener.errors.containsKey(file));

      final var
        file_errors = listener.errors.get(file);
      final var file_wanted_errors =
        file_errors.stream()
          .filter(CatalogFilesystemReaderContract::isModificationTimeError)
          .collect(Collectors.toSet());

      Assert.assertEquals(1L, (long) file_wanted_errors.size());
      Assert.assertEquals(1L, (long) listener.valids.size());
    }
  }

  @Test
  public final void testVerificationFileTypeChanged()
    throws Exception
  {
    final var settings =
      CatalogVerificationReportSettings.builder()
        .setIgnoreAccessTime(CatalogIgnoreAccessTime.IGNORE_ACCESS_TIME)
        .build();

    try (final var fs = this.getFileSystem()) {
      final var root = fs.getRootDirectories().iterator().next();
      final var file = root.relativize(root.resolve("file.txt"));
      Files.createFile(file);

      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of("test"), CatalogDiskID.of(BigInteger.ZERO), root);

      Thread.sleep(1000L);
      Files.delete(file);
      Files.createDirectories(file);
      Thread.sleep(1000L);

      final var listener = new CheckedListener();

      CatalogFilesystemReader.verifyDisk(
        disk, settings, root, listener);

      Assert.assertTrue(listener.errors.containsKey(file));

      final var
        file_errors = listener.errors.get(file);
      final var file_wanted_errors =
        file_errors.stream()
          .filter(CatalogFilesystemReaderContract::isTypeChangedError)
          .collect(Collectors.toSet());

      Assert.assertEquals(1L, (long) file_wanted_errors.size());
      Assert.assertEquals(0L, (long) listener.valids.size());
    }
  }

  @Test
  public final void testVerificationFileVanished()
    throws Exception
  {
    final var settings =
      CatalogVerificationReportSettings.builder()
        .setIgnoreAccessTime(CatalogIgnoreAccessTime.IGNORE_ACCESS_TIME)
        .build();

    try (final var fs = this.getFileSystem()) {
      final var root = fs.getRootDirectories().iterator().next();
      final var file = root.relativize(root.resolve("file.txt"));
      Files.createFile(file);

      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of("test"), CatalogDiskID.of(BigInteger.ZERO), root);

      Thread.sleep(1000L);
      Files.delete(file);
      Thread.sleep(1000L);

      final var listener = new CheckedListener();

      CatalogFilesystemReader.verifyDisk(
        disk, settings, root, listener);

      Assert.assertEquals(2L, (long) listener.errors.size());
      Assert.assertTrue(listener.errors.containsKey(file));

      final var
        file_errors = listener.errors.get(file);
      final var file_wanted_errors =
        file_errors.stream()
          .filter(CatalogFilesystemReaderContract::isFileVanishedError)
          .collect(Collectors.toSet());

      Assert.assertEquals(1L, (long) file_wanted_errors.size());
      Assert.assertEquals(0L, (long) listener.valids.size());
    }
  }

  @Test
  public final void testVerificationFileUncatalogued()
    throws Exception
  {
    final var settings =
      CatalogVerificationReportSettings.builder()
        .setIgnoreAccessTime(CatalogIgnoreAccessTime.IGNORE_ACCESS_TIME)
        .build();

    try (final var fs = this.getFileSystem()) {
      final var root = fs.getRootDirectories().iterator().next();
      final var file = root.relativize(root.resolve("file.txt"));
      final var file_more = root.relativize(root.resolve("file_more.txt"));
      Files.createFile(file);

      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of("test"), CatalogDiskID.of(BigInteger.ZERO), root);

      Thread.sleep(1000L);
      Files.createFile(file_more);
      Thread.sleep(1000L);

      final var listener = new CheckedListener();

      CatalogFilesystemReader.verifyDisk(
        disk, settings, root, listener);

      Assert.assertEquals(2L, (long) listener.errors.size());
      Assert.assertTrue(listener.errors.containsKey(file_more));

      final var
        file_errors = listener.errors.get(file_more);
      final var file_wanted_errors =
        file_errors.stream()
          .filter(CatalogFilesystemReaderContract::isFileUncataloguedError)
          .collect(Collectors.toSet());

      Assert.assertEquals(1L, (long) file_wanted_errors.size());
      Assert.assertEquals(1L, (long) listener.valids.size());
    }
  }

  @Test
  public final void testVerificationTimeCreationChanged()
    throws Exception
  {
    final var settings =
      CatalogVerificationReportSettings.builder()
        .setIgnoreAccessTime(CatalogIgnoreAccessTime.IGNORE_ACCESS_TIME)
        .build();

    try (final var fs = this.getFileSystem()) {
      final var root = fs.getRootDirectories().iterator().next();
      final var file = root.relativize(root.resolve("file.txt"));
      Files.createFile(file);

      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of("test"), CatalogDiskID.of(BigInteger.ZERO), root);

      Thread.sleep(1000L);
      Files.delete(file);
      Files.createFile(file);

      final var listener = new CheckedListener();

      CatalogFilesystemReader.verifyDisk(
        disk, settings, root, listener);

      Assert.assertTrue(listener.errors.containsKey(file));

      final var
        file_errors = listener.errors.get(file);
      final var file_wanted_errors =
        file_errors.stream()
          .filter(CatalogFilesystemReaderContract::isCreationTimeError)
          .collect(Collectors.toSet());

      Assert.assertEquals(1L, (long) file_wanted_errors.size());
      Assert.assertEquals(0L, (long) listener.valids.size());
    }
  }

  @Test
  public final void testVerificationHashChanged()
    throws Exception
  {
    final var settings =
      CatalogVerificationReportSettings.builder()
        .setIgnoreAccessTime(CatalogIgnoreAccessTime.DO_NOT_IGNORE_ACCESS_TIME)
        .build();

    try (final var fs = this.getFileSystem()) {
      final var root = fs.getRootDirectories().iterator().next();
      final var file = root.relativize(root.resolve("file.txt"));
      Files.createFile(file);

      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of("test"), CatalogDiskID.of(BigInteger.ZERO), root);

      Files.write(file, "Hello".getBytes(StandardCharsets.UTF_8));
      Files.setLastModifiedTime(file, FileTime.from(Instant.ofEpochSecond(1L)));

      final var listener = new CheckedListener();

      CatalogFilesystemReader.verifyDisk(
        disk, settings, root, listener);

      Assert.assertTrue(listener.errors.containsKey(file));

      final var
        file_errors = listener.errors.get(file);
      final var file_wanted_errors =
        file_errors.stream()
          .filter(x -> x instanceof CatalogVerificationChangedHash)
          .collect(Collectors.toSet());

      Assert.assertEquals(1L, (long) file_wanted_errors.size());
      Assert.assertEquals(1L, (long) listener.valids.size());
    }
  }

  private static final class CheckedListener
    implements CatalogVerificationListenerType
  {
    final MutableMultimap<Path, CatalogVerificationReportItemErrorType> errors;
    final MutableMultimap<Path, CatalogVerificationReportItemOKType> valids;
    boolean completed;

    CheckedListener()
    {
      this.errors = new HashBagMultimap<>();
      this.valids = new HashBagMultimap<>();
    }

    @Override
    public void onItemVerified(final CatalogVerificationReportItemOKType ok)
    {
      LOG.info(
        "ok: {} | {}", ok.path(), ok.show());
      this.valids.put(ok.path(), ok);
    }

    @Override
    public void onItemError(
      final CatalogVerificationReportItemErrorType error)
    {
      LOG.error(
        "error: {} | {}", error.path(), error.show());
      this.errors.put(error.path(), error);
    }

    @Override
    public void onCompleted()
    {
      this.completed = true;
    }
  }
}
