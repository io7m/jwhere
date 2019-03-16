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

package com.io7m.jwhere.tests.search;

import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogJSONParser;
import com.io7m.jwhere.search.CatalogSearchSpecification;
import com.io7m.jwhere.search.CatalogSearcherType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public abstract class CatalogSearcherContract
{
  protected abstract CatalogSearcherType searcher();

  @Test
  public final void testSearchSimple()
    throws Exception
  {
    final var catalog = parseResource("basic.jcz");
    final var searcher = this.searcher();

    final var results =
      searcher.search(catalog, CatalogSearchSpecification.builder()
        .setFileNamePattern(Pattern.compile("vesa.c32"))
        .build());

    Assertions.assertEquals(1, results.size());
    final var r0 = results.get(0);
    Assertions.assertEquals(
      "arch/boot/syslinux/vesa.c32",
      String.join("/", r0.disk().getPathForNode(r0.node())));
  }

  @Test
  public final void testNone()
    throws Exception
  {
    final var catalog = parseResource("basic.jcz");
    final var searcher = this.searcher();

    final var results =
      searcher.search(catalog, CatalogSearchSpecification.builder()
        .setFileNamePattern(Pattern.compile(""))
        .build());

    Assertions.assertEquals(0, results.size());
  }

  @Test
  public final void testAll()
    throws Exception
  {
    final var catalog = parseResource("basic.jcz");
    final var searcher = this.searcher();

    final var results =
      searcher.search(catalog, CatalogSearchSpecification.builder()
        .setFileNamePattern(Pattern.compile(".*"))
        .build());

    Assertions.assertEquals(138, results.size());
  }

  private static Catalog parseResource(final String file)
    throws Exception
  {
    final var parser = CatalogJSONParser.newParser();
    final Catalog catalog;

    try (final var stream = CatalogSearcherContract.class.getResourceAsStream(
      "/com/io7m/jwhere/tests/" + file)) {
      try (final var gzip = new GZIPInputStream(stream)) {
        catalog = parser.parseCatalogFromStream(gzip);
      }
    }
    return catalog;
  }
}
