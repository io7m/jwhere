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

import com.io7m.jnull.NullCheck;
import net.jcip.annotations.Immutable;
import org.valid4j.Assertive;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A report indicating whether or not a catalog could be verified.
 */

@Immutable public final class CatalogVerificationReport
{
  private final SortedMap<Path, Set<CatalogVerificationReportItemErrorType>>
                                errors;
  private final SortedSet<Path> ok;

  private CatalogVerificationReport(
    final SortedMap<Path, Set<CatalogVerificationReportItemErrorType>>
      in_errors,
    final SortedSet<Path> in_ok)
  {
    this.errors =
      Collections.unmodifiableSortedMap(NullCheck.notNull(in_errors));
    this.ok = Collections.unmodifiableSortedSet(NullCheck.notNull(in_ok));
  }

  /**
   * @return A new mutable builder for building reports
   */

  public static CatalogVerificationReportBuilderType newBuilder()
  {
    return new Builder();
  }

  /**
   * Show the results of the report.
   *
   * @param report      The report
   * @param only_errors {@code true} iff successful results should not be shown
   * @param out         The output stream
   */

  public static void showReport(
    final CatalogVerificationReport report,
    final boolean only_errors,
    final PrintStream out)
  {
    if (!only_errors) {
      final SortedSet<Path> ok = report.getPathsWithoutErrors();
      for (final Path q : ok) {
        out.print("ok : /");
        out.print(q);
        out.println(" : verified");
      }
    }

    final SortedMap<Path, Set<CatalogVerificationReportItemErrorType>> errors =
      report.getErrors();
    for (final Path q : errors.keySet()) {
      final Set<CatalogVerificationReportItemErrorType> es = errors.get(q);
      for (final CatalogVerificationReportItemErrorType e : es) {
        out.print("error : /");
        out.print(q);
        out.print(" : ");
        out.println(e.show());
      }
    }
  }

  /**
   * @return A read-only map consisting of the paths that resulted in errors
   */

  public SortedMap<Path, Set<CatalogVerificationReportItemErrorType>>
  getErrors()
  {
    return this.errors;
  }

  /**
   * @return A read-only set of the paths that did not have errors
   */

  public SortedSet<Path> getPathsWithoutErrors()
  {
    return this.ok;
  }

  /**
   * A value indicating whether or not access times should be ignored.
   */

  public enum IgnoreAccessTime
  {
    /**
     * Ignore access time changes.
     */

    IGNORE_ACCESS_TIME,

    /**
     * Do not ignore access time changes.
     */

    DO_NOT_IGNORE_ACCESS_TIME
  }

  /**
   * Report settings.
   */

  public static final class Settings
  {
    private final IgnoreAccessTime atime;

    /**
     * Construct report settings.
     *
     * @param in_atime Whether or not to ignore access time changes
     */

    public Settings(final IgnoreAccessTime in_atime)
    {
      this.atime = NullCheck.notNull(in_atime);
    }

    /**
     * @return The current access time setting
     */

    public IgnoreAccessTime getIgnoreAccessTime()
    {
      return this.atime;
    }
  }

  private static final class Builder
    implements CatalogVerificationReportBuilderType
  {
    private final SortedMap<Path, Set<CatalogVerificationReportItemErrorType>>
                                  errors;
    private final SortedSet<Path> ok;
    private final boolean         finished;

    Builder()
    {
      this.errors = new TreeMap<>();
      this.ok = new TreeSet<>();
      this.finished = false;
    }

    @Override public void addItemOK(final Path path)
    {
      NullCheck.notNull(path);

      Assertive.require(!this.finished, "Builders cannot be reused");
      Assertive.require(
        !this.errors.containsKey(path),
        "An error is already logged for this path");

      this.ok.add(path);
    }

    @Override public CatalogVerificationReport build()
    {
      return new CatalogVerificationReport(this.errors, this.ok);
    }

    @Override public boolean pathIsReferenced(final Path path)
    {
      NullCheck.notNull(path);
      return this.ok.contains(path) || this.errors.containsKey(path);
    }

    @Override public void addItemUncatalogued(final Path path)
    {
      NullCheck.notNull(path);
      Assertive.require(!this.finished, "Builders cannot be reused");
      this.putError(path, new CatalogVerificationUncataloguedItem(path));
    }

    private void putError(
      final Path path,
      final CatalogVerificationReportItemErrorType e)
    {
      Assertive.require(
        !this.ok.contains(path),
        "This path has already been marked as correct");

      final Set<CatalogVerificationReportItemErrorType> existing;
      if (this.errors.containsKey(path)) {
        existing = this.errors.get(path);
      } else {
        existing = new HashSet<>();
      }

      existing.add(e);
      this.errors.put(path, existing);
    }

    @Override public void addItemChangedType(
      final Path path,
      final CatalogNodeType node,
      final CatalogNodeType node_now)
    {
      NullCheck.notNull(path);
      NullCheck.notNull(node);
      NullCheck.notNull(node_now);

      Assertive.require(!this.finished, "Builders cannot be reused");

      this.putError(
        path, new CatalogVerificationChangedType(path, node, node_now));
    }

    @Override public void addItemMetadataChanged(
      final Path path,
      final CatalogVerificationChangedMetadata.Field field,
      final String value_then,
      final String value_now)
    {
      NullCheck.notNull(path);
      NullCheck.notNull(field);
      NullCheck.notNull(value_then);
      NullCheck.notNull(value_now);

      Assertive.require(!this.finished, "Builders cannot be reused");

      this.putError(
        path, new CatalogVerificationChangedMetadata(
          path, field, value_then, value_now));
    }

    @Override public void addItemHashChanged(
      final Path path,
      final CatalogFileHash hash_then,
      final CatalogFileHash hash_now)
    {
      NullCheck.notNull(path);
      NullCheck.notNull(hash_then);
      NullCheck.notNull(hash_now);

      Assertive.require(!this.finished, "Builders cannot be reused");

      this.putError(
        path, new CatalogVerificationChangedHash(path, hash_then, hash_now));
    }

    @Override public void addItemDisappeared(final Path path)
    {
      NullCheck.notNull(path);

      Assertive.require(!this.finished, "Builders cannot be reused");

      this.putError(
        path, new CatalogVerificationVanishedItem(path));
    }
  }
}
