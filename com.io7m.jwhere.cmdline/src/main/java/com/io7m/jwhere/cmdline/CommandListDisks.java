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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.io7m.jwhere.core.CatalogDiskDuplicateIDException;
import com.io7m.jwhere.core.CatalogJSONParseException;
import com.io7m.jwhere.core.CatalogJSONParser;
import com.io7m.jwhere.core.CatalogNodeException;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * A command to list disks in a catalog.
 */

@Command(name = "list-disks", description = "List disks in a catalog")
public final class CommandListDisks extends CommandBase
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CommandListDisks.class);
  }

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

  public CommandListDisks()
  {

  }

  @Override
  public void run()
  {
    super.setup();

    var status = 0;

    try {
      final var p = CatalogJSONParser.newParser();
      final var file = new File(this.catalog).toPath();
      final var jom = new ObjectMapper();

      LOG.debug("Opening {}", file);

      final var c = CommandBase.openCatalogForReading(p, file);
      final var dm = c.getDisks();
      final var iter = dm.keySet().iterator();
      while (iter.hasNext()) {
        final var index = iter.next();
        final var disk = dm.get(index);
        final var meta = disk.getMeta();

        System.out.printf(
          "[%s] %-32s %s %s\n",
          index,
          meta.getDiskName(),
          meta.getSize(),
          meta.getFilesystemType());
      }

    } catch (final CatalogNodeException | CatalogDiskDuplicateIDException e) {
      LOG.error(
        "Catalog error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        LOG.error("Exception trace: ", e);
      }
      status = 1;
    } catch (final CatalogJSONParseException e) {
      LOG.error(
        "JSON parse error: {}: {}", e.getClass(), e.getMessage());
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
