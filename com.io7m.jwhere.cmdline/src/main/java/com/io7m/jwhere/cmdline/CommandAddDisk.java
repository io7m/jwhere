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

import com.io7m.jwhere.core.CatalogCompress;
import com.io7m.jwhere.core.CatalogDiskDuplicateIDException;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskName;
import com.io7m.jwhere.core.CatalogException;
import com.io7m.jwhere.core.CatalogFilesystemReader;
import com.io7m.jwhere.core.CatalogJSONParseException;
import com.io7m.jwhere.core.CatalogJSONParser;
import com.io7m.jwhere.core.CatalogSaveSpecification;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

/**
 * A command to add a disk to a catalog.
 */

@Command(name = "add-disk",
  description = "Catalog a disk and add it to a catalog")
public final class CommandAddDisk extends CommandBase
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CommandAddDisk.class);
  }

  /**
   * The compression scheme to use for the catalog
   */

  @Option(name = "--catalog-compress",
    arity = 1,
    description = "The compression scheme to use for the catalog")
  private final CatalogCompress catalog_compress =
    CatalogCompress.COMPRESS_GZIP;

  /**
   * The path to the input catalog.
   */

  @Option(name = "--catalog-input",
    arity = 1,
    description = "The path to the input catalog file",
    required = true) private String catalog_in;
  /**
   * The path to the output catalog.
   */

  @Option(name = "--catalog-output",
    arity = 1,
    description = "The path to the output catalog file",
    required = true) private String catalog_out;
  /**
   * The filesystem root.
   */

  @Option(name = "--disk-root",
    arity = 1,
    description = "The path to a filesystem root",
    required = true) private String root;

  /**
   * The name of a disk.
   */

  @Option(name = "--disk-name",
    arity = 1,
    description = "The name of the disk",
    required = true) private String disk_name;

  /**
   * The ID of the disk to be created.
   */

  @Option(name = "--disk-id",
    arity = 1,
    description = "The ID of the disk",
    required = true) private BigInteger disk_index;

  /**
   * Construct a command.
   */

  public CommandAddDisk()
  {

  }

  @Override
  public void run()
  {
    super.setup();

    var status = 0;

    try {
      LOG.debug("Disk {}", this.disk_name);
      LOG.debug("Index {}", this.disk_index);
      LOG.debug("Root {}", this.root);
      LOG.debug("Catalog input {}", this.catalog_in);
      LOG.debug("Catalog output {}", this.catalog_out);

      final var p = CatalogJSONParser.newParser();
      final var catalog_in_path = new File(this.catalog_in).toPath();
      final var catalog_out_path = new File(this.catalog_out).toPath();
      final var root_path = new File(this.root).toPath();

      LOG.debug("Opening {}", catalog_in_path);
      final var c = CommandBase.openCatalogForReading(p, catalog_in_path);
      final var disks = c.getDisks();
      final var id = CatalogDiskID.of(this.disk_index);
      if (disks.containsKey(id)) {
        throw new CatalogDiskDuplicateIDException(
          String.format(
            "Catalog already contains a disk with index %s", id));
      }

      final var disk = CatalogFilesystemReader.newDisk(
        CatalogDiskName.of(this.disk_name), id, root_path);
      final var meta = disk.getMeta();

      disks.put(meta.getDiskID(), disk);
      CommandBase.writeCatalogToDisk(
        c,
        CatalogSaveSpecification.builder()
          .setCompress(this.catalog_compress)
          .setPath(catalog_out_path)
          .build());

    } catch (final CatalogJSONParseException e) {
      LOG.error(
        "JSON parse error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        LOG.error("Exception trace: ", e);
      }
      status = 1;
    } catch (final CatalogException e) {
      LOG.error(
        "Catalog error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        LOG.error("Exception trace: ", e);
      }
      status = 1;
    } catch (final IOException e) {
      LOG.error(
        "I/O error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        LOG.error("Exception trace: ", e);
      }
      status = 1;
    }

    System.exit(status);
  }
}
