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

import java.nio.file.Path;

/**
 * An error that indicates that a file or directory has disappeared more
 * recently than the catalog was created.
 */

public final class CatalogVerificationVanishedItem
  implements CatalogVerificationReportItemErrorType
{
  private final Path path;

  /**
   * Construct an error.
   *
   * @param in_path The path
   */

  public CatalogVerificationVanishedItem(final Path in_path)
  {
    this.path = NullCheck.notNull(in_path);
  }

  @Override public Path getPath()
  {
    return this.path;
  }

  @Override public String show()
  {
    return "Item no longer exists";
  }

  @Override public String toString()
  {
    final StringBuilder sb =
      new StringBuilder("CatalogVerificationVanishedItem{");
    sb.append("path=").append(this.path);
    sb.append('}');
    return sb.toString();
  }
}
