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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * A command to list disks in a catalog.
 */

@Parameters(commandDescription = "List disks within a catalog")
public final class CommandListDisks extends CommandRoot
{
  private static final Logger LOG = LoggerFactory.getLogger(CommandListDisks.class);

  // CHECKSTYLE:OFF

  @Parameter(
    names = "--catalog",
    required = true,
    description = "The path to a catalog file")
  Path path;

  // CHECKSTYLE:ON

  /**
   * Construct a command.
   */

  public CommandListDisks()
  {

  }

  @Override
  public Void call()
    throws Exception
  {
    super.call();

    final var catalog = Catalogs.loadCatalog(this.path);
    final var disks = catalog.getDisks();
    final var iter = disks.keySet().iterator();
    while (iter.hasNext()) {
      final var index = iter.next();
      final var disk = disks.get(index);
      final var meta = disk.getMeta();

      System.out.printf(
        "[%s] %-32s %s %s\n",
        index.value(),
        meta.getDiskName().value(),
        meta.getSize(),
        meta.getFilesystemType());
    }

    return null;
  }
}
