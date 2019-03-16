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

import io.airlift.airline.Cli;
import io.airlift.airline.Help;
import io.airlift.airline.ParseArgumentsMissingException;
import io.airlift.airline.ParseArgumentsUnexpectedException;
import io.airlift.airline.ParseOptionMissingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * Main command line frontend.
 */

public final class Main
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(Main.class);
  }

  private Main()
  {

  }

  /**
   * Run the program.
   *
   * @param args Command line arguments.
   */

  public static void main(final String[] args)
  {
    run(args);
  }

  private static int run(final String[] args)
  {
    final Cli.CliBuilder<Runnable> builder = Cli.builder("jwhere");
    builder.withDescription("Disk cataloguing tool");
    builder.withDefaultCommand(Help.class);
    builder.withCommand(CommandInit.class);
    builder.withCommand(CommandListDisks.class);
    builder.withCommand(CommandAddDisk.class);
    builder.withCommand(CommandRemoveDisk.class);
    builder.withCommand(CommandVerifyDisk.class);
    builder.withCommand(CommandImportGWhere.class);
    builder.withCommand(Help.class);
    final var parser = builder.build();

    try {
      parser.parse(args).run();
      return 0;
    } catch (final ParseArgumentsMissingException
      | ParseOptionMissingException
      | ParseArgumentsUnexpectedException e) {
      LOG.error("Parse error: {}", e.getMessage());
      Help.help(parser.getMetadata(), Collections.emptyList());
      return 1;
    }
  }

}
