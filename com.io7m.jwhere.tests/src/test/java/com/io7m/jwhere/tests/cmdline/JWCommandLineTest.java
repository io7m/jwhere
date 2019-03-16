/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> http://io7m.com
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


package com.io7m.jwhere.tests.cmdline;

import com.io7m.jwhere.cmdline.Catalogs;
import com.io7m.jwhere.cmdline.Main;
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogCompress;
import com.io7m.jwhere.core.CatalogDiskID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.file.Files;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class JWCommandLineTest
{
  @Test
  public void testNoArguments()
  {
    final var main = new Main(new String[]{

    });
    main.run();

    Assertions.assertEquals(1, main.exitCode());
  }

  @Test
  public void testInitNoArgs()
  {
    final var main = new Main(new String[]{
      "init"
    });
    main.run();

    Assertions.assertEquals(1, main.exitCode());
  }

  @Test
  public void testInitOK()
    throws Exception
  {
    final var file = Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file);

    final var main = new Main(new String[]{
      "init",
      "--catalog",
      file.toString()
    });
    main.run();
    Assertions.assertEquals(0, main.exitCode());
    Assertions.assertTrue(Files.isRegularFile(file));
  }

  @Test
  public void testInitAlreadyExists()
    throws Exception
  {
    final var file = Files.createTempFile("jwcmd", ".jcz");

    final var main = new Main(new String[]{
      "init",
      "--catalog",
      file.toString()
    });
    main.run();
    Assertions.assertEquals(1, main.exitCode());
  }

  @Test
  public void testAddDiskOK()
    throws Exception
  {
    final var directory =
      Files.createTempDirectory("jwcmd");
    final var file0 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file0);
    final var file1 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file1);

    final var catalog0 = new Catalog(new TreeMap<>());
    Catalogs.saveCatalog(catalog0, CatalogCompress.COMPRESS_GZIP, file0);

    final var main = new Main(new String[]{
      "add-disk",
      "--catalog-input",
      file0.toString(),
      "--catalog-output",
      file1.toString(),
      "--disk-root",
      directory.toString(),
      "--disk-name",
      "ABCD",
      "--disk-id",
      "2"
    });
    main.run();

    Assertions.assertEquals(0, main.exitCode());
    Assertions.assertTrue(Files.isRegularFile(file0));
    Assertions.assertTrue(Files.isRegularFile(file1));

    final var catalog1 = Catalogs.loadCatalog(file1);
    Assertions.assertTrue(catalog1.getDisks().containsKey(CatalogDiskID.of(BigInteger.valueOf(2L))));
  }

  @Test
  public void testAddDiskDuplicate()
    throws Exception
  {
    final var directory =
      Files.createTempDirectory("jwcmd");
    final var file0 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file0);
    final var file1 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file1);
    final var file2 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file2);

    final var catalog0 = new Catalog(new TreeMap<>());
    Catalogs.saveCatalog(catalog0, CatalogCompress.COMPRESS_GZIP, file0);

    {
      final var main = new Main(new String[]{
        "add-disk",
        "--catalog-input",
        file0.toString(),
        "--catalog-output",
        file1.toString(),
        "--disk-root",
        directory.toString(),
        "--disk-name",
        "ABCD",
        "--disk-id",
        "2"
      });
      main.run();
      Assertions.assertEquals(0, main.exitCode());
    }

    {
      final var main = new Main(new String[]{
        "add-disk",
        "--catalog-input",
        file1.toString(),
        "--catalog-output",
        file2.toString(),
        "--disk-root",
        directory.toString(),
        "--disk-name",
        "ABCD",
        "--disk-id",
        "2"
      });
      main.run();
      Assertions.assertEquals(1, main.exitCode());
    }
  }

  @Test
  public void testListDisksOK()
    throws Exception
  {
    final var directory =
      Files.createTempDirectory("jwcmd");
    final var file0 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file0);
    final var file1 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file1);

    final var catalog0 = new Catalog(new TreeMap<>());
    Catalogs.saveCatalog(catalog0, CatalogCompress.COMPRESS_GZIP, file0);

    {
      final var main = new Main(new String[]{
        "add-disk",
        "--catalog-input",
        file0.toString(),
        "--catalog-output",
        file1.toString(),
        "--disk-root",
        directory.toString(),
        "--disk-name",
        "ABCD",
        "--disk-id",
        "2"
      });
      main.run();
      Assertions.assertEquals(0, main.exitCode());
    }

    {
      final var main = new Main(new String[]{
        "list-disks",
        "--catalog",
        file0.toString()
      });
      main.run();
      Assertions.assertEquals(0, main.exitCode());
    }
  }

  @Test
  public void testListAddedDisksOK()
    throws Exception
  {
    final var file0 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file0);

    final var catalog0 = new Catalog(new TreeMap<>());
    Catalogs.saveCatalog(catalog0, CatalogCompress.COMPRESS_GZIP, file0);

    final var main = new Main(new String[]{
      "list-disks",
      "--catalog",
      file0.toString()
    });
    main.run();

    Assertions.assertEquals(0, main.exitCode());
  }

  @Test
  public void testVerifyDiskOK()
    throws Exception
  {
    final var directory =
      Files.createTempDirectory("jwcmd");
    final var file0 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file0);
    final var file1 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file1);

    final var catalog0 = new Catalog(new TreeMap<>());
    Catalogs.saveCatalog(catalog0, CatalogCompress.COMPRESS_GZIP, file0);

    {
      final var main = new Main(new String[]{
        "add-disk",
        "--catalog-input",
        file0.toString(),
        "--catalog-output",
        file1.toString(),
        "--disk-root",
        directory.toString(),
        "--disk-name",
        "ABCD",
        "--disk-id",
        "2"
      });
      main.run();
      Assertions.assertEquals(0, main.exitCode());
    }

    {
      final var main = new Main(new String[]{
        "verify-disk",
        "--catalog",
        file1.toString(),
        "--disk-root",
        directory.toString(),
        "--disk-id",
        "2",
      });
      main.run();
      Assertions.assertEquals(0, main.exitCode());
    }
  }

  @Test
  public void testRemoveDiskNonexistent()
    throws Exception
  {
    final var file0 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file0);
    final var file1 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file1);

    final var catalog0 = new Catalog(new TreeMap<>());
    Catalogs.saveCatalog(catalog0, CatalogCompress.COMPRESS_GZIP, file0);

    final var main = new Main(new String[]{
      "remove-disk",
      "--catalog-input",
      file0.toString(),
      "--catalog-output",
      file1.toString(),
      "--disk-id",
      "0"
    });
    main.run();

    Assertions.assertEquals(1, main.exitCode());
  }

  @Test
  public void testRemoveDiskOK()
    throws Exception
  {
    final var directory =
      Files.createTempDirectory("jwcmd");
    final var file0 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file0);
    final var file1 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file1);
    final var file2 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file2);

    final var catalog0 = new Catalog(new TreeMap<>());
    Catalogs.saveCatalog(catalog0, CatalogCompress.COMPRESS_GZIP, file0);

    {
      final var main = new Main(new String[]{
        "add-disk",
        "--catalog-input",
        file0.toString(),
        "--catalog-output",
        file1.toString(),
        "--disk-root",
        directory.toString(),
        "--disk-name",
        "ABCD",
        "--disk-id",
        "2"
      });
      main.run();
      Assertions.assertEquals(0, main.exitCode());
    }

    {
      final var main = new Main(new String[]{
        "remove-disk",
        "--catalog-input",
        file1.toString(),
        "--catalog-output",
        file2.toString(),
        "--disk-id",
        "2"
      });
      main.run();
      Assertions.assertEquals(0, main.exitCode());
    }

    final var catalog1 = Catalogs.loadCatalog(file2);
    Assertions.assertTrue(catalog1.getDisks().isEmpty());
  }

  @Test
  public void testImportGWhere()
    throws Exception
  {
    final var file0 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file0);
    final var file1 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file1);

    try (var output = Files.newOutputStream(file1, WRITE, CREATE, TRUNCATE_EXISTING)) {
      try (var gzip = new GZIPOutputStream(output)) {
        try (var input = JWCommandLineTest.class.getResourceAsStream(
          "/com/io7m/jwhere/tests/gwhere/archive-real-0.ctg")) {
          input.transferTo(gzip);
        }
        gzip.finish();
      }
    }

    {
      final var main = new Main(new String[]{
        "import-gwhere",
        "--catalog",
        file0.toString(),
        "--gwhere-catalog",
        file1.toString()
      });
      main.run();
      Assertions.assertEquals(0, main.exitCode());
    }
  }

  @Test
  public void testSearchNonexistent()
    throws Exception
  {
    final var file0 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file0);

    {
      final var main = new Main(new String[]{
        "search",
        "--catalog",
        file0.toString(),
        "--pattern",
        ".*"
      });
      main.run();
      Assertions.assertEquals(1, main.exitCode());
    }
  }

  @Test
  public void testSearchBadPattern()
    throws Exception
  {
    final var file0 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file0);

    {
      final var main = new Main(new String[]{
        "search",
        "--catalog",
        file0.toString(),
        "--pattern",
        "("
      });
      main.run();
      Assertions.assertEquals(1, main.exitCode());
    }
  }

  @Test
  public void testSearchOK()
    throws Exception
  {
    final var file0 =
      Files.createTempFile("jwcmd", ".jcz");
    Files.delete(file0);

    final var catalog0 = new Catalog(new TreeMap<>());
    Catalogs.saveCatalog(catalog0, CatalogCompress.COMPRESS_GZIP, file0);

    {
      final var main = new Main(new String[]{
        "search",
        "--catalog",
        file0.toString(),
        "--pattern",
        ".*"
      });
      main.run();
      Assertions.assertEquals(0, main.exitCode());
    }
  }
}
