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
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogFileNode;
import com.io7m.jwhere.core.CatalogNodeType;
import net.java.quickcheck.Generator;

import java.util.Random;

public final class CatalogNodeGenerator implements Generator<CatalogNodeType>
{
  private final Generator<Boolean>              type_gen;
  private final Generator<CatalogFileNode>      file_gen;
  private final Generator<CatalogDirectoryNode> dir_gen;

  public CatalogNodeGenerator(
    final Generator<Boolean> in_type_gen,
    final Generator<CatalogFileNode> in_file_gen,
    final Generator<CatalogDirectoryNode> in_dir_gen)
  {
    this.type_gen = Objects.requireNonNull(in_type_gen, "in_type_gen");
    this.file_gen = Objects.requireNonNull(in_file_gen, "in_file_gen");
    this.dir_gen = Objects.requireNonNull(in_dir_gen, "in_dir_gen");
  }

  public static Generator<CatalogNodeType> getDefault()
  {
    final Random r = new Random();
    final Generator<Boolean> bool_gen = () -> Boolean.valueOf(r.nextBoolean());
    return new CatalogNodeGenerator(
      bool_gen,
      CatalogFileNodeGenerator.getDefault(),
      CatalogDirectoryNodeGenerator.getDefault());
  }

  @Override public CatalogNodeType next()
  {
    if (this.type_gen.next().booleanValue()) {
      return this.file_gen.next();
    } else {
      return this.dir_gen.next();
    }
  }
}
