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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDiskDuplicateIndexException;
import com.io7m.jwhere.core.CatalogJSONParseException;
import com.io7m.jwhere.core.CatalogJSONParserType;
import com.io7m.jwhere.core.CatalogJSONParserUtilities;
import com.io7m.jwhere.core.CatalogJSONSerializer;
import com.io7m.jwhere.core.CatalogJSONSerializerType;
import com.io7m.jwhere.core.CatalogNodeException;
import io.airlift.airline.Option;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * The base type of all commands.
 */

public abstract class CommandBase implements Runnable
{
  @Option(name = "--debug", description = "Enable debug logging")
  private boolean debug;

  protected static Catalog openCatalogForReading(
    final CatalogJSONParserType p,
    final Path file)
    throws
    IOException,
    CatalogJSONParseException,
    CatalogNodeException,
    CatalogDiskDuplicateIndexException
  {
    final ObjectMapper jom = new ObjectMapper();
    try (InputStream is = Files.newInputStream(
      file, StandardOpenOption.READ)) {
      final JsonNode j = jom.readTree(is);
      final ObjectNode jo = CatalogJSONParserUtilities.checkObject(null, j);
      return p.parseCatalog(jo);
    }
  }

  protected static void writeCatalogToDisk(
    final Catalog c,
    final Path p)
    throws IOException
  {
    final CatalogJSONSerializerType s = CatalogJSONSerializer.newSerializer();
    final ObjectNode j = s.serializeCatalog(c);
    final ObjectMapper jom = new ObjectMapper();
    final ObjectWriter jw = jom.writerWithDefaultPrettyPrinter();

    try (final OutputStream os = Files.newOutputStream(
      p,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING)) {
      jw.writeValue(os, j);
    }
  }

  protected final void configureLogLevel()
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
