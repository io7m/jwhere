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
import com.io7m.jwhere.search.CatalogSearchSpecification;
import com.io7m.jwhere.search.CatalogSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * A command to list disks in a catalog.
 */

@Parameters(commandDescription = "Search for entries within a catalog")
public final class CommandSearch extends CommandRoot
{
  private static final Logger LOG = LoggerFactory.getLogger(CommandSearch.class);

  // CHECKSTYLE:OFF

  @Parameter(
    names = "--catalog",
    required = true,
    description = "The path to a catalog file")
  Path path;

  @Parameter(
    names = "--pattern",
    required = true,
    converter = PatternConverter.class,
    description = "A regular expression that will be matched against filenames")
  Pattern pattern;

  // CHECKSTYLE:ON

  /**
   * Construct a command.
   */

  public CommandSearch()
  {

  }

  @Override
  public Void call()
    throws Exception
  {
    super.call();

    final var catalog = Catalogs.loadCatalog(this.path);
    final var searcher = CatalogSearcher.create();

    final var results =
      searcher.search(
        catalog,
        CatalogSearchSpecification.builder()
          .setFileNamePattern(this.pattern)
          .build());

    for (final var result : results) {
      final var disk = result.disk();
      final var node = result.node();

      System.out.printf(
        "[%s] %s: %s\n",
        disk.getMeta().getDiskID().value(),
        disk.getMeta().getDiskName().value(),
        String.join("/", disk.getPathForNode(node)));
    }

    return null;
  }
}
