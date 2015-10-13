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

package com.io7m.jwhere.core;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * The type of JSON parsers.
 */

public interface CatalogJSONParserType
{
  /**
   * Parse a catalog from the given path. Some intelligence is used: If the path
   * appears to be a compressed catalog, an attempt will be made to open it as
   * such.
   *
   * @param p The path
   *
   * @return A catalog
   *
   * @throws CatalogJSONParseException          On parsing or validation errors
   * @throws CatalogNodeException               On malformed disk errors
   * @throws CatalogDiskDuplicateIndexException Iff two parsed disks have the
   *                                            same ID
   * @throws IOException                        On I/O errors
   */

  Catalog parseCatalogFromPath(Path p)
    throws
    CatalogJSONParseException,
    CatalogNodeException,
    CatalogDiskDuplicateIndexException,
    IOException;

  /**
   * Parse a catalog from the given path using the given compression setting.
   *
   * @param p           The path
   * @param compression The compression method used to compress the target
   *                    catalog
   *
   * @return A catalog
   *
   * @throws CatalogJSONParseException          On parsing or validation errors
   * @throws CatalogNodeException               On malformed disk errors
   * @throws CatalogDiskDuplicateIndexException Iff two parsed disks have the
   *                                            same ID
   * @throws IOException                        On I/O errors
   */

  Catalog parseCatalogFromPathWithCompression(
    Path p,
    CatalogSaveSpecification.Compress compression)
    throws
    CatalogJSONParseException,
    CatalogNodeException,
    CatalogDiskDuplicateIndexException,
    IOException;

  /**
   * Parse a catalog from the given input stream.
   *
   * @param is An input stream
   *
   * @return A catalog
   *
   * @throws CatalogJSONParseException          On parsing or validation errors
   * @throws CatalogNodeException               On malformed disk errors
   * @throws CatalogDiskDuplicateIndexException Iff two parsed disks have the
   *                                            same ID
   * @throws IOException                        On I/O errors
   */

  Catalog parseCatalogFromStream(InputStream is)
    throws
    CatalogJSONParseException,
    CatalogNodeException,
    CatalogDiskDuplicateIndexException,
    IOException;

  /**
   * Parse a catalog from the given JSON node.
   *
   * @param c A JSON object
   *
   * @return A catalog
   *
   * @throws CatalogJSONParseException          On parsing or validation errors
   * @throws CatalogNodeException               On malformed disk errors
   * @throws CatalogDiskDuplicateIndexException Iff two parsed disks have the
   *                                            same ID
   */

  Catalog parseCatalog(ObjectNode c)
    throws
    CatalogJSONParseException,
    CatalogNodeException,
    CatalogDiskDuplicateIndexException;

  /**
   * Parse a disk from the given JSON node.
   *
   * @param c A JSON object
   *
   * @return A disk
   *
   * @throws CatalogJSONParseException On parsing or validation errors
   * @throws CatalogNodeException      On malformed disk errors
   */

  CatalogDisk parseDisk(ObjectNode c)
    throws CatalogJSONParseException, CatalogNodeException;
}
