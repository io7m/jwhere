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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDiskDuplicateIDException;
import com.io7m.jwhere.core.CatalogJSONParseException;
import com.io7m.jwhere.core.CatalogJSONParserType;
import com.io7m.jwhere.core.CatalogJSONSerializer;
import com.io7m.jwhere.core.CatalogJSONSerializerType;
import com.io7m.jwhere.core.CatalogNodeException;
import com.io7m.jwhere.core.CatalogSaveSpecification;
import io.airlift.airline.Option;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The base type of all commands.
 */

public abstract class CommandBase implements Runnable
{
  @Option(name = "--debug", description = "Enable debug logging")
  private boolean debug;

  @Option(name = "--wait-for-stdin",
    description = "Wait for a single byte on stdin before executing.")
  private boolean wait_profiler;

  protected static Catalog openCatalogForReading(
    final CatalogJSONParserType p,
    final Path file)
    throws
    IOException,
    CatalogJSONParseException,
    CatalogNodeException, CatalogDiskDuplicateIDException
  {
    return p.parseCatalogFromPath(file);
  }

  protected static void writeCatalogToDisk(
    final Catalog c,
    final CatalogSaveSpecification spec)
    throws IOException
  {
    final CatalogJSONSerializerType s = CatalogJSONSerializer.newSerializer();
    s.serializeCatalogToPath(c, spec);
  }

  protected final void setup()
  {
    this.configureLogLevel();

    if (this.wait_profiler) {
      try {
        System.in.read();
      } catch (final IOException e) {
        // Ignore
      }
    }
  }

  private void configureLogLevel()
  {
    final Logger root = (Logger) LoggerFactory.getLogger(
      org.slf4j.Logger.ROOT_LOGGER_NAME);
    if (this.debug) {
      root.setLevel(Level.DEBUG);
    }
  }

  protected final boolean isDebug()
  {
    return this.debug;
  }
}
