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


package com.io7m.jwhere.core;

/**
 * The possible metadata fields.
 */

public enum CatalogVerificationMetadataField
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

  CatalogVerificationMetadataField(final String in_name)
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
