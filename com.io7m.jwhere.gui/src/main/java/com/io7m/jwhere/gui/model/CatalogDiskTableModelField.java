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

package com.io7m.jwhere.gui.model;

import java.time.Instant;
import java.util.Objects;

enum CatalogDiskTableModelField
{
  NAME("Name", DirectoryEntryType.class),
  SIZE("Size", SizeBytes.class),
  CREATION_TIME("Creation Time", Instant.class),
  MODIFICATION_TIME("Modification Time", Instant.class),
  ACCESS_TIME("Access Time", Instant.class),
  OWNER("Owner", String.class),
  GROUP("Group", String.class),
  PERMISSIONS("Permissions", String.class),
  HASH("Hash", String.class);

  private final String name;
  private final Class<?> type;

  CatalogDiskTableModelField(
    final String in_name,
    final Class<?> in_type)
  {
    this.name = Objects.requireNonNull(in_name, "in_name");
    this.type = Objects.requireNonNull(in_type, "in_type");
  }

  public Class<?> getType()
  {
    return this.type;
  }

  public String getName()
  {
    return this.name;
  }
}
