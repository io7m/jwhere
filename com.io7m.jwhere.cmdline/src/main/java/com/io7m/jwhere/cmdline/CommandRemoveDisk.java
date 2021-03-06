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

package com.io7m.jwhere.cmdline;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.jwhere.core.CatalogCompress;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskNonexistentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.file.Path;

/**
 * A command to remove a disk from a catalog.
 */

@Parameters(commandDescription = "Remove a disk from a catalog")
public final class CommandRemoveDisk extends CommandRoot
{
  private static final Logger LOG = LoggerFactory.getLogger(CommandRemoveDisk.class);

  // CHECKSTYLE:OFF

  /**
   * The compression scheme to use for the catalog
   */

  @Parameter(
    names = "--catalog-compress",
    description = "The compression scheme to use for the catalog")
  CatalogCompress catalog_compress = CatalogCompress.COMPRESS_GZIP;

  /**
   * The path to the input catalog.
   */

  @Parameter(
    names = "--catalog-input",
    description = "The path to the input catalog file",
    required = true)
  Path catalog_in;

  /**
   * The path to the output catalog.
   */

  @Parameter(
    names = "--catalog-output",
    description = "The path to the output catalog file",
    required = true)
  Path catalog_out;

  /**
   * The ID of the disk to be created.
   */

  @Parameter(
    names = "--disk-id",
    description = "The ID of the disk",
    converter = BigIntegerConverter.class,
    required = true)
  BigInteger disk_index;

  // CHECKSTYLE:ON

  /**
   * Construct a command.
   */

  public CommandRemoveDisk()
  {

  }

  @Override
  public Void call()
    throws Exception
  {
    super.call();

    final var catalog = Catalogs.loadCatalog(this.catalog_in);

    final var disks = catalog.getDisks();
    final var id = CatalogDiskID.of(this.disk_index);
    if (!disks.containsKey(id)) {
      throw new CatalogDiskNonexistentException(
        String.format("Catalog does not contain a disk with index %s", id));
    }

    disks.remove(id);
    Catalogs.saveCatalog(catalog, this.catalog_compress, this.catalog_out);
    return null;
  }
}
