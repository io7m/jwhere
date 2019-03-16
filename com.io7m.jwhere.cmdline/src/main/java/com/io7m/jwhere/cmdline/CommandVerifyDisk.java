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
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogFilesystemReader;
import com.io7m.jwhere.core.CatalogIgnoreAccessTime;
import com.io7m.jwhere.core.CatalogVerificationListenerType;
import com.io7m.jwhere.core.CatalogVerificationReportItemErrorType;
import com.io7m.jwhere.core.CatalogVerificationReportItemOKType;
import com.io7m.jwhere.core.CatalogVerificationReportSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;

/**
 * A command to list disks in a catalog.
 */

@Parameters(commandDescription = "Verify a disk within a catalog")
public final class CommandVerifyDisk extends CommandRoot
{
  private static final Logger LOG = LoggerFactory.getLogger(CommandVerifyDisk.class);

  // CHECKSTYLE:OFF

  @Parameter(
    names = "--catalog",
    required = true,
    description = "The path to a catalog file")
  Path path;

  /**
   * The filesystem root.
   */

  @Parameter(
    names = "--disk-root",
    description = "The path to a filesystem root",
    required = true)
  private Path root;

  /**
   * The ID of the disk to be verified.
   */

  @Parameter(
    names = "--disk-id",
    description = "The ID of the disk",
    converter = BigIntegerConverter.class,
    required = true)
  private BigInteger disk_index;

  /**
   * Only show errors.
   */

  @Parameter(
    names = "--errors-only",
    description = "Only show errors")
  private boolean only_errors;

  // CHECKSTYLE:ON

  /**
   * Construct a command.
   */

  public CommandVerifyDisk()
  {

  }

  @Override
  public Void call()
    throws Exception
  {
    super.call();

    final var catalog = Catalogs.loadCatalog(this.path);
    final var id = CatalogDiskID.of(this.disk_index);
    final var disks = catalog.getDisks();
    if (!disks.containsKey(id)) {
      throw new FileNotFoundException("No such disk: " + this.disk_index.toString());
    }

    final var disk = disks.get(id);
    final var settings =
      CatalogVerificationReportSettings.builder()
        .setIgnoreAccessTime(CatalogIgnoreAccessTime.IGNORE_ACCESS_TIME)
        .build();

    final var verifier = new VerificationListener();
    CatalogFilesystemReader.verifyDisk(disk, settings, this.root, verifier);

    if (verifier.failed) {
      throw new IOException("One or more files failed verification");
    }

    return null;
  }

  private static final class VerificationListener implements CatalogVerificationListenerType
  {
    private boolean failed;

    VerificationListener()
    {

    }

    @Override
    public void onItemVerified(
      final CatalogVerificationReportItemOKType ok)
    {
      System.out.printf("%s | OK | %s\n", ok.path(), ok.show());
    }

    @Override
    public void onItemError(
      final CatalogVerificationReportItemErrorType error)
    {
      System.out.printf(
        "%s | FAILED | %s\n",
        error.path(),
        error.show());
      this.failed = true;
    }

    @Override
    public void onCompleted()
    {
      // Nothing
    }
  }
}
