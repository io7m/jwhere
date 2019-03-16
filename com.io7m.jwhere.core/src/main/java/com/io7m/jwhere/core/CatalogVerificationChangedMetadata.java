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

import java.util.Objects;

import java.nio.file.Path;

/**
 * An error indicating that a metadata field has changed.
 */

public final class CatalogVerificationChangedMetadata
  implements CatalogVerificationReportItemErrorType
{
  private final Path   path;
  private final Field  field;
  private final String value_then;
  private final String value_now;

  /**
   * Construct an error.
   *
   * @param in_path       The path
   * @param in_field      The field
   * @param in_value_then The old field value
   * @param in_value_now  The new field value
   */

  public CatalogVerificationChangedMetadata(
    final Path in_path,
    final Field in_field,
    final String in_value_then,
    final String in_value_now)
  {
    this.path = Objects.requireNonNull(in_path, "in_path");
    this.field = Objects.requireNonNull(in_field, "in_field");
    this.value_then = Objects.requireNonNull(in_value_then, "in_value_then");
    this.value_now = Objects.requireNonNull(in_value_now, "in_value_now");
  }

  @Override public Path path()
  {
    return this.path;
  }

  @Override public String show()
  {
    return String.format(
      "%s value was %s but is now %s",
      this.field,
      this.value_then,
      this.value_now);
  }

  /**
   * @return The field that changed
   */

  public Field getField()
  {
    return this.field;
  }

  /**
   * The possible metadata fields.
   */

  public enum Field
  {
    /**
     * The modification time.
     */

    MODIFICATION_TIME("Modification time"),

    /**
     * The access time.
     */

    ACCESS_TIME("Access time"),

    /**
     * The creation time.
     */

    CREATION_TIME("Creation time"),

    /**
     * The file owner.
     */

    OWNER("Owner"),

    /**
     * The file group.
     */

    GROUP("Group"),

    /**
     * The file permissions.
     */

    PERMISSIONS("Permissions");

    private final String name;

    Field(final String in_name)
    {
      this.name = in_name;
    }

    /**
     * @return The name of the field
     */

    public String getName()
    {
      return this.name;
    }
  }
}
