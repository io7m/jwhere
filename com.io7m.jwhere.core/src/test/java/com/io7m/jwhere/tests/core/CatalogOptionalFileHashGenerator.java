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

package com.io7m.jwhere.tests.core;

import java.util.Objects;
import com.io7m.jwhere.core.CatalogFileHash;
import net.java.quickcheck.Generator;

import java.util.Optional;

public final class CatalogOptionalFileHashGenerator
  implements Generator<Optional<CatalogFileHash>>
{
  private final Generator<CatalogFileHash> hash_gen;
  private final Generator<Boolean>         type_gen;

  public CatalogOptionalFileHashGenerator(
    final Generator<CatalogFileHash> in_hash_gen,
    final Generator<Boolean> in_type_gen)
  {
    this.hash_gen = Objects.requireNonNull(in_hash_gen, "in_hash_gen");
    this.type_gen = Objects.requireNonNull(in_type_gen, "in_type_gen");
  }

  @Override public Optional<CatalogFileHash> next()
  {
    if (this.type_gen.next().booleanValue()) {
      return Optional.of(this.hash_gen.next());
    }
    return Optional.empty();
  }
}
