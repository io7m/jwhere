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

import com.io7m.jwhere.core.CatalogFileHash;
import com.io7m.jwhere.core.CatalogFileHashes;
import net.java.quickcheck.Generator;
import net.java.quickcheck.QuickCheck;
import net.java.quickcheck.characteristic.AbstractCharacteristic;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public abstract class CatalogFileHashContract
{
  @Rule public ExpectedException expected = ExpectedException.none();

  protected abstract FileSystem getFileSystem();

  @Test public final void testFromFileDirectory()
    throws Exception
  {
    try (FileSystem fs = this.getFileSystem()) {
      final Path p = fs.getPath("xyz");
      Files.createDirectory(p);

      this.expected.expect(FileSystemException.class);
      this.expected.expectMessage(new StringContains("is a directory"));
      CatalogFileHashes.fromFile(p);
    }
  }

  @Test public final void testFromFileNonexistent()
    throws Exception
  {
    try (FileSystem fs = this.getFileSystem()) {
      final Path p = fs.getPath("nonexistent");
      this.expected.expect(NoSuchFileException.class);
      this.expected.expectMessage(new StringContains("nonexistent"));
      CatalogFileHashes.fromFile(p);
    }
  }

  @Test public final void testFromFileHashCorrect()
    throws Exception
  {
    try (FileSystem fs = this.getFileSystem()) {
      final Path p = fs.getPath("hello.txt");
      Files.write(
        p, "Hello".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);

      final CatalogFileHash h = CatalogFileHashes.fromFile(p);
      Assert.assertEquals(
        "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969"
          .toUpperCase(),
        h.value());
      Assert.assertEquals("SHA-256", h.algorithm());
    }
  }

  @Test public final void testEqualsCases()
  {
    final Generator<CatalogFileHash> gen =
      CatalogFileHashGenerator.getDefault();

    QuickCheck.forAllVerbose(
      gen, new AbstractCharacteristic<CatalogFileHash>()
      {
        @Override protected void doSpecify(final CatalogFileHash cd)
          throws Throwable
        {
          final CatalogFileHash ce = gen.next();
          final CatalogFileHash cf =
            CatalogFileHash.copyOf(cd);
          final CatalogFileHash cg =
            CatalogFileHash.copyOf(cd);

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
