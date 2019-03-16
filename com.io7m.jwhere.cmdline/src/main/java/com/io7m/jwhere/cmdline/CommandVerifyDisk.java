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

import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogException;
import com.io7m.jwhere.core.CatalogFilesystemReader;
import com.io7m.jwhere.core.CatalogIgnoreAccessTime;
import com.io7m.jwhere.core.CatalogJSONParseException;
import com.io7m.jwhere.core.CatalogJSONParser;
import com.io7m.jwhere.core.CatalogVerificationListenerType;
import com.io7m.jwhere.core.CatalogVerificationReportItemErrorType;
import com.io7m.jwhere.core.CatalogVerificationReportItemOKType;
import com.io7m.jwhere.core.CatalogVerificationReportSettings;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A command to verify a disk in the catalog.
 */

@Command(name = "verify-disk",
  description = "Verify a disk in a catalog")
public final class CommandVerifyDisk extends CommandBase
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CommandVerifyDisk.class);
  }

  /**
   * The path to the input catalog.
   */

  @Option(name = "--catalog",
    arity = 1,
    description = "The path to the input catalog file",
    required = true) private String catalog_in;

  /**
   * The filesystem root.
   */

  @Option(name = "--disk-root",
    arity = 1,
    description = "The path to a filesystem root",
    required = true) private String root;

  /**
   * The ID of the disk to be verified.
   */

  @Option(name = "--disk-id",
    arity = 1,
    description = "The ID of the disk",
    required = true) private BigInteger disk_index;

  /**
   * Only show errors.
   */

  @Option(name = "--errors-only",
    description = "Only show errors") private boolean only_errors;

  /**
   * Construct a command.
   */

  public CommandVerifyDisk()
  {

  }

  @Override
  public void run()
  {
    super.setup();

    final var status = new AtomicInteger(0);

    try {
      LOG.debug("Index {}", this.disk_index);
      LOG.debug("Root {}", this.root);
      LOG.debug("Catalog input {}", this.catalog_in);

      final var p = CatalogJSONParser.newParser();
      final var catalog_in_path = new File(this.catalog_in).toPath();
      final var root_path = new File(this.root).toPath();

      LOG.debug("Opening {}", catalog_in_path);
      final var c = CommandBase.openCatalogForReading(p, catalog_in_path);
      final var id = CatalogDiskID.of(this.disk_index);
      final var disks = c.getDisks();
      if (!disks.containsKey(id)) {
        throw new NoSuchElementException(this.disk_index.toString());
      }

      final var disk = disks.get(id);
      final var settings =
        CatalogVerificationReportSettings.builder()
          .setIgnoreAccessTime(CatalogIgnoreAccessTime.IGNORE_ACCESS_TIME)
          .build();

      CatalogFilesystemReader.verifyDisk(
        disk, settings, root_path, new VerificationListener(status));

    } catch (final NoSuchElementException e) {
      LOG.error(
        "No such disk with index: {}", e.getMessage());
      if (this.isDebug()) {
        LOG.error("Exception trace: ", e);
      }
      status.set(1);
    } catch (final CatalogJSONParseException e) {
      LOG.error(
        "JSON parse error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        LOG.error("Exception trace: ", e);
      }
      status.set(1);
    } catch (final CatalogException e) {
      LOG.error(
        "Catalog error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        LOG.error("Exception trace: ", e);
      }
      status.set(1);
    } catch (final IOException e) {
      LOG.error(
        "I/O error: {}: {}", e.getClass(), e.getMessage());
      if (this.isDebug()) {
        LOG.error("Exception trace: ", e);
      }
      status.set(1);
    }

    System.exit(status.get());
  }

  private static final class VerificationListener implements CatalogVerificationListenerType
  {
    private final AtomicInteger status;

    VerificationListener(final AtomicInteger in_status)
    {
      this.status = Objects.requireNonNull(in_status, "status");
    }

    @Override
    public void onItemVerified(
      final
      CatalogVerificationReportItemOKType ok)
    {
      System.out.printf("%s | OK | %s\n", ok.path(), ok.show());
    }

    @Override
    public void onItemError(
      final
      CatalogVerificationReportItemErrorType error)
    {
      System.out.printf(
        "%s | FAILED | %s\n",
        error.path(),
        error.show());
      this.status.set(2);
    }

    @Override
    public void onCompleted()
    {
      // Nothing
    }
  }
}
