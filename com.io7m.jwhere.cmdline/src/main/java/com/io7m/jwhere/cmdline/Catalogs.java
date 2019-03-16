/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> http://io7m.com
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
import com.io7m.jwhere.core.CatalogJSONParser;
import com.io7m.jwhere.core.CatalogJSONSerializer;
import com.io7m.jwhere.core.CatalogSaveSpecification;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Convenience functions for loading and saving catalogs.
 */

public final class Catalogs
{
  private Catalogs()
  {

  }

  /**
   * Load a catalog.
   *
   * @param path The path
   *
   * @return A catalog
   *
   * @throws Exception On errors
   */

  public static Catalog loadCatalog(final Path path)
    throws Exception
  {
    Objects.requireNonNull(path, "path");
    final var parser = CatalogJSONParser.newParser();
    return parser.parseCatalogFromPath(path);
  }

  /**
   * Save a catalog.
   *
   * @param catalog          The catalog
   * @param catalog_compress The compression to use
   * @param path             The output path
   *
   * @throws Exception On errors
   */

  public static void saveCatalog(
    final Catalog catalog,
    final CatalogCompress catalog_compress,
    final Path path)
    throws Exception
  {
    Objects.requireNonNull(catalog, "catalog");
    Objects.requireNonNull(catalog_compress, "catalog_compress");
    Objects.requireNonNull(path, "path");

    final var save_spec =
      CatalogSaveSpecification.builder()
        .setCompress(catalog_compress)
        .setPath(path)
        .build();

    final var serializer = CatalogJSONSerializer.newSerializer();
    serializer.serializeCatalogToPath(catalog, save_spec);
  }
}
