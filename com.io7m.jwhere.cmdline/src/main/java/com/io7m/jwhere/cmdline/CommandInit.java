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
import com.io7m.jwhere.core.CatalogCompress;
import com.io7m.jwhere.core.CatalogSaveSpecification;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.TreeMap;

/**
 * A command to initialize a catalog.
 */

@Command(name = "init", description = "Initialize a catalog")
public final class CommandInit extends CommandBase
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CommandInit.class);
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
   * The path to the catalog.
   */

  @Option(name = "--catalog",
    arity = 1,
    description = "The path to a catalog file",
    required = true) private String catalog;

  /**
   * Construct a command.
   */

  public CommandInit()
  {

  }

  @Override
  public void run()
  {
    super.setup();

    var status = 0;

    try {
      LOG.debug("Initializing {}", this.catalog);

      final var c = new Catalog(new TreeMap<>());
      final var p = new File(this.catalog).toPath();

      if (Files.notExists(p, LinkOption.NOFOLLOW_LINKS)) {
        CommandBase.writeCatalogToDisk(
          c, CatalogSaveSpecification.builder()
            .setCompress(this.catalog_compress)
            .setPath(p)
            .build());
      } else {
        throw new FileAlreadyExistsException(this.catalog);
      }

    } catch (final FileAlreadyExistsException e) {
      LOG.error(
        "File already exists: {}", e.getMessage());
      if (this.isDebug()) {
        LOG.error("Exception trace: ", e);
      }
      status = 1;
    } catch (final IOException e) {
      LOG.error("I/O error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        LOG.error("Exception trace: ", e);
      }
      status = 1;
    }

    System.exit(status);
  }
}
