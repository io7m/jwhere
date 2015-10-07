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

import com.io7m.jnull.NullCheck;
import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.IntegerGenerator;

import java.util.ArrayList;
import java.util.List;

public final class PathListGenerator implements Generator<List<String>>
{
  private final Generator<Integer> length_gen;
  private final FileNameGenerator  name_gen;

  public PathListGenerator(
    final Generator<Integer> in_length_gen,
    final FileNameGenerator in_name_gen)
  {
    this.length_gen = NullCheck.notNull(in_length_gen);
    this.name_gen = NullCheck.notNull(in_name_gen);
  }

  public static PathListGenerator getDefault()
  {
    return new PathListGenerator(
      new IntegerGenerator(1, 10), new FileNameGenerator());
  }

  @Override public List<String> next()
  {
    final int length = this.length_gen.next().intValue();
    final List<String> list = new ArrayList<>(length);
    for (int index = 0; index < length; ++index) {
      list.add(this.name_gen.next());
    }
    return list;
  }
}
