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


package com.io7m.jwhere.search;

import com.io7m.jwhere.core.Catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * The default implementation of the {@link CatalogSearcherType} interface.
 */

public final class CatalogSearcher implements CatalogSearcherType
{
  private CatalogSearcher()
  {

  }

  /**
   * Create a new searcher.
   *
   * @return A new searcher
   */

  public static CatalogSearcherType create()
  {
    return new CatalogSearcher();
  }

  @Override
  public List<CatalogSearchResult> search(
    final Catalog catalog,
    final CatalogSearchSpecification search)
  {
    Objects.requireNonNull(catalog, "catalog");
    Objects.requireNonNull(search, "search");

    try {
      return this.search(catalog, search, new CompletableFuture<>()).get();
    } catch (final InterruptedException | ExecutionException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public CompletableFuture<List<CatalogSearchResult>> search(
    final Catalog catalog,
    final CatalogSearchSpecification search,
    final CompletableFuture<List<CatalogSearchResult>> future)
  {
    Objects.requireNonNull(catalog, "catalog");
    Objects.requireNonNull(search, "search");
    Objects.requireNonNull(future, "future");

    final var results = new ArrayList<CatalogSearchResult>();
    final var disks = catalog.getDisks();
    for (final var disk : disks.values()) {
      if (future.isCancelled()) {
        break;
      }

      final var graph = disk.getFilesystemGraph();
      final var vertices = graph.vertexSet();
      for (final var node : vertices) {
        if (future.isCancelled()) {
          break;
        }

        final var path = disk.getPathForNode(node);
        if (path.isEmpty()) {
          continue;
        }

        final var last = path.get(path.size() - 1);
        final var matcher = search.fileNamePattern().matcher(last);
        if (matcher.matches()) {
          results.add(CatalogSearchResult.of(disk, node));
        }
      }
    }

    future.complete(results);
    return future;
  }
}
