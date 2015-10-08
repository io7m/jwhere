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

import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskDuplicateIndexException;
import com.io7m.jwhere.core.CatalogException;
import com.io7m.jwhere.core.CatalogFilesystemReader;
import com.io7m.jwhere.core.CatalogJSONParseException;
import com.io7m.jwhere.core.CatalogJSONParser;
import com.io7m.jwhere.core.CatalogJSONParserType;
import com.io7m.jwhere.core.CatalogNodeException;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.SortedMap;

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
   * The archive number of the disk to be created.
   */

  @Option(name = "--disk-index",
          arity = 1,
          description = "The archive number of the disk",
          required = true) private BigInteger disk_index;

  /**
   * Construct a command.
   */

  public CommandAddDisk()
  {

  }

  @Override public void run()
  {
    this.configureLogLevel();

    try {
      CommandAddDisk.LOG.debug("Disk {}", this.disk_name);
      CommandAddDisk.LOG.debug("Index {}", this.disk_index);
      CommandAddDisk.LOG.debug("Root {}", this.root);
      CommandAddDisk.LOG.debug("Catalog input {}", this.catalog_in);
      CommandAddDisk.LOG.debug("Catalog output {}", this.catalog_out);

      final CatalogJSONParserType p = CatalogJSONParser.newParser();
      final Path catalog_in_path = new File(this.catalog_in).toPath();
      final Path catalog_out_path = new File(this.catalog_out).toPath();
      final Path root_path = new File(this.root).toPath();

      CommandAddDisk.LOG.debug("Opening {}", catalog_in_path);
      final Catalog c = CommandBase.openCatalogForReading(p, catalog_in_path);
      final SortedMap<BigInteger, CatalogDisk> disks = c.getDisks();
      if (disks.containsKey(this.disk_index)) {
        throw new CatalogDiskDuplicateIndexException(
          String.format(
            "Catalog already contains a disk with index %s", this.disk_index));
      }

      final CatalogDisk disk = CatalogFilesystemReader.newDisk(
        this.disk_name, this.disk_index, root_path);
      disks.put(disk.getArchiveIndex(), disk);
      CommandBase.writeCatalogToDisk(c, catalog_out_path);

    } catch (final CatalogNodeException | CatalogDiskDuplicateIndexException
      e) {
      CommandAddDisk.LOG.error(
        "Catalog error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        CommandAddDisk.LOG.error("Exception trace: ", e);
      }
    } catch (final CatalogJSONParseException e) {
      CommandAddDisk.LOG.error(
        "JSON parse error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        CommandAddDisk.LOG.error("Exception trace: ", e);
      }
    } catch (final IOException e) {
      CommandAddDisk.LOG.error(
        "I/O error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        CommandAddDisk.LOG.error("Exception trace: ", e);
      }
    } catch (final CatalogException e) {
      CommandAddDisk.LOG.error(
        "Catalog error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        CommandAddDisk.LOG.error("Exception trace: ", e);
      }
    }
  }
}
