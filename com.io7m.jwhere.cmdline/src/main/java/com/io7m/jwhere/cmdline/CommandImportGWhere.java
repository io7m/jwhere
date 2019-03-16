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
import com.io7m.jwhere.core.CatalogException;
import com.io7m.jwhere.core.CatalogJSONSerializer;
import com.io7m.jwhere.core.CatalogSaveSpecification;
import com.io7m.jwhere.gwhere.GWhereParser;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;

/**
 * A command to add a disk to a catalog.
 */

@Command(name = "import-gwhere",
  description = "Import a GWhere catalog")
public final class CommandImportGWhere extends CommandBase
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CommandImportGWhere.class);
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
   * The path to the output catalog.
   */

  @Option(name = "--catalog-output",
    arity = 1,
    description = "The path to the output catalog file",
    required = true) private String catalog_out;
  /**
   * The filesystem root.
   */

  @Option(name = "--gwhere-catalog",
    arity = 1,
    description = "The path to a GWhere root",
    required = true) private String gwhere;

  /**
   * Construct a command.
   */

  public CommandImportGWhere()
  {

  }

  @Override
  public void run()
  {
    super.setup();

    var status = 0;

    try {
      LOG.debug("Catalog output {}", this.catalog_out);
      LOG.debug("GWhere {}", this.gwhere);

      final var catalog_gw_path = new File(this.gwhere).toPath();
      final var catalog_out_path = new File(this.catalog_out).toPath();

      final var gp = catalog_gw_path;
      try (var is = Files.newInputStream(gp)) {
        try (var z = new GZIPInputStream(is)) {
          final var gwp = GWhereParser.newParser(z);
          final var c = gwp.parseCatalog();
          final var s =
            CatalogJSONSerializer.newSerializer();
          s.serializeCatalogToPath(
            c,
            CatalogSaveSpecification.builder()
              .setCompress(this.catalog_compress)
              .setPath(catalog_out_path)
              .build());
        }
      }

    } catch (final IOException e) {
      LOG.error(
        "I/O error: {}: {}", e.getClass(), e.getMessage());
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
    }

    System.exit(status);
  }
}
