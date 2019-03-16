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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Main command line frontend.
 */

public final class Main implements Runnable
{
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private final Map<String, CommandType> commands;
  private final JCommander commander;
  private final String[] args;
  private int exit_code;

  /**
   * Construct a new main program.
   *
   * @param in_args Command-line arguments
   */

  public Main(final String[] in_args)
  {
    this.args = Objects.requireNonNull(in_args, "args");

    final var r = new CommandRoot();
    final var cmd_init = new CommandInit();
    final var cmd_list_disks = new CommandListDisks();
    final var cmd_verify_disk = new CommandVerifyDisk();
    final var cmd_import_gwhere = new CommandImportGWhere();
    final var cmd_add_disk = new CommandAddDisk();
    final var cmd_remove_disk = new CommandRemoveDisk();

    this.commands = new HashMap<>(8);
    this.commands.put("init", cmd_init);
    this.commands.put("list-disks", cmd_list_disks);
    this.commands.put("verify-disk", cmd_verify_disk);
    this.commands.put("import-gwhere", cmd_import_gwhere);
    this.commands.put("add-disk", cmd_add_disk);
    this.commands.put("remove-disk", cmd_remove_disk);

    this.commander = new JCommander(r);
    this.commander.setProgramName("jwhere");
    this.commander.addCommand("init", cmd_init);
    this.commander.addCommand("list-disks", cmd_list_disks);
    this.commander.addCommand("verify-disk", cmd_verify_disk);
    this.commander.addCommand("import-gwhere", cmd_import_gwhere);
    this.commander.addCommand("add-disk", cmd_add_disk);
    this.commander.addCommand("remove-disk", cmd_remove_disk);
  }

  /**
   * The main entry point.
   *
   * @param args Command line arguments
   */

  public static void main(final String[] args)
  {
    final var cm = new Main(args);
    cm.run();
    System.exit(cm.exitCode());
  }

  /**
   * @return The program exit code
   */

  public int exitCode()
  {
    return this.exit_code;
  }

  @Override
  public void run()
  {
    try {
      this.commander.parse(this.args);

      final var cmd = this.commander.getParsedCommand();
      if (cmd == null) {
        final var sb = new StringBuilder(128);
        this.commander.usage(sb);
        LOG.info("Arguments required.\n{}", sb.toString());
        this.exit_code = 1;
        return;
      }

      final var command = this.commands.get(cmd);
      command.call();
    } catch (final ParameterException e) {
      final var sb = new StringBuilder(128);
      this.commander.usage(sb);
      LOG.error("{}\n{}", e.getMessage(), sb.toString());
      this.exit_code = 1;
    } catch (final Exception e) {
      LOG.error("{}", e.getMessage(), e);
      this.exit_code = 1;
    }
  }
}
