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

/**
 * Report settings.
 */

public final class CatalogVerificationReportSettings
{
  private final CatalogIgnoreAccessTime atime;

  /**
   * Construct report settings.
   *
   * @param in_atime Whether or not to ignore access time changes
   */

  public CatalogVerificationReportSettings(
    final CatalogIgnoreAccessTime in_atime)
  {
    this.atime = NullCheck.notNull(in_atime);
  }

  /**
   * @return The current access time setting
   */

  public CatalogIgnoreAccessTime getIgnoreAccessTime()
  {
    return this.atime;
  }
}
