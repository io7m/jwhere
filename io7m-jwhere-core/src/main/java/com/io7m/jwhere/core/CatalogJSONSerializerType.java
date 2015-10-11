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
import java.io.OutputStream;

/**
 * The type of JSON serializers.
 */

public interface CatalogJSONSerializerType
{
  /**
   * Serialize the catalog to the given stream.
   *
   * @param c  The catalog
   * @param os The output stream
   *
   * @throws IOException On I/O errors
   */

  void serializeCatalogToStream(
    Catalog c,
    OutputStream os)
    throws IOException;

  /**
   * Serialize the given catalog to JSON.
   *
   * @param c The catalog
   *
   * @return A JSON object
   */

  ObjectNode serializeCatalog(Catalog c);

  /**
   * Serialize the given disk to JSON.
   *
   * @param d The disk
   *
   * @return A JSON object
   */

  ObjectNode serializeDisk(CatalogDisk d);
}
