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
import com.io7m.jwhere.core.CatalogException;
import com.io7m.jwhere.core.CatalogJSONSerializer;
import com.io7m.jwhere.core.CatalogJSONSerializerType;
import com.io7m.jwhere.core.CatalogSaveSpecification;
import com.io7m.jwhere.gwhere.GWhereParser;
import com.io7m.jwhere.gwhere.GWhereParserType;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
  private final CatalogSaveSpecification.Compress catalog_compress =
    CatalogSaveSpecification.Compress.COMPRESS_GZIP;

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

  @Override public void run()
  {
    super.setup();

    int status = 0;

    try {
      CommandImportGWhere.LOG.debug("Catalog output {}", this.catalog_out);
      CommandImportGWhere.LOG.debug("GWhere {}", this.gwhere);

      final Path catalog_gw_path = new File(this.gwhere).toPath();
      final Path catalog_out_path = new File(this.catalog_out).toPath();

      final Path gp = catalog_gw_path;
      try (final InputStream is = Files.newInputStream(gp)) {
        try (final GZIPInputStream z = new GZIPInputStream(is)) {
          final GWhereParserType gwp = GWhereParser.newParser(z);
          final Catalog c = gwp.parseCatalog();
          final CatalogJSONSerializerType s =
            CatalogJSONSerializer.newSerializer();
          s.serializeCatalogToPath(
            c, new CatalogSaveSpecification(
              this.catalog_compress, catalog_out_path));
        }
      }

    } catch (final IOException e) {
      CommandImportGWhere.LOG.error(
        "I/O error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        CommandImportGWhere.LOG.error("Exception trace: ", e);
      }
      status = 1;
    } catch (final CatalogException e) {
      CommandImportGWhere.LOG.error(
        "Catalog error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        CommandImportGWhere.LOG.error("Exception trace: ", e);
      }
      status = 1;
    }

    System.exit(status);
  }
}
