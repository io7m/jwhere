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
 * An error indicating that the hash of a file has changed.
 */

public final class CatalogVerificationChangedHash
  implements CatalogVerificationReportItemErrorType
{
  private final Path            path;
  private final CatalogFileHash hash_then;
  private final CatalogFileHash hash_now;

  /**
   * Construct an error.
   *
   * @param in_path      The path of the file
   * @param in_hash_then The old hash value
   * @param in_hash_now  The new hash value
   */

  public CatalogVerificationChangedHash(
    final Path in_path,
    final CatalogFileHash in_hash_then,
    final CatalogFileHash in_hash_now)
  {
    this.path = Objects.requireNonNull(in_path, "in_path");
    this.hash_then = Objects.requireNonNull(in_hash_then, "in_hash_then");
    this.hash_now = Objects.requireNonNull(in_hash_now, "in_hash_now");
  }

  @Override public Path getPath()
  {
    return this.path;
  }

  @Override public String show()
  {
    return String.format(
      "Hash value was %s but is now %s", this.hash_then, this.hash_now);
  }
}
