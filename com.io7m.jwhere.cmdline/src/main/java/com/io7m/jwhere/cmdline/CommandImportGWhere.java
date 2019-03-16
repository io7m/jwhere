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
import com.io7m.jwhere.gwhere.GWhereParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/**
 * A command to import a GWhere catalog.
 */

@Parameters(commandDescription = "Initialize a catalog")
public final class CommandImportGWhere extends CommandRoot
{
  private static final Logger LOG = LoggerFactory.getLogger(CommandImportGWhere.class);

  // CHECKSTYLE:OFF

  @Parameter(
    names = "--catalog",
    required = true,
    description = "The path to a catalog file")
  Path path;

  @Parameter(
    names = "--compress",
    required = false,
    description = "The compression scheme to use for the catalog")
  CatalogCompress catalog_compress = CatalogCompress.COMPRESS_GZIP;

  @Parameter(
    names = "--gwhere-catalog",
    description = "The path to a GWhere catalog",
    required = true)
  Path gwhere;

  // CHECKSTYLE:ON

  /**
   * Construct a command.
   */

  public CommandImportGWhere()
  {

  }

  @Override
  public Void call()
    throws Exception
  {
    super.call();

    try (var stream = Files.newInputStream(this.gwhere)) {
      try (var gzip_stream = new GZIPInputStream(stream)) {
        final var gwhere_parser = GWhereParser.newParser(gzip_stream);
        final var catalog = gwhere_parser.parseCatalog();
        Catalogs.saveCatalog(catalog, this.catalog_compress, this.path);
      }
    }

    return null;
  }

}
