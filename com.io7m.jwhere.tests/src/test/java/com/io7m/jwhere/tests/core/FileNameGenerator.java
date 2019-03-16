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

import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.support.IntegerGenerator;
import net.java.quickcheck.generator.support.StringGenerator;
import net.java.quickcheck.generator.support.VetoableGenerator;

public final class FileNameGenerator implements Generator<String>
{
  private final Generator<String> text_gen;
  private final IntegerGenerator length_gen;
  private final VetoableGenerator<String> veto_gen;
  private final Generator<String> text_noslash_gen;

  public FileNameGenerator()
  {
    this.length_gen = new IntegerGenerator(1, 128);
    this.text_gen = new StringGenerator(
      this.length_gen,
      FileNameCharacterGenerator.getDefault());
    this.text_noslash_gen =
      () -> FileNameGenerator.this.text_gen.next().replaceAll("/", "");

    this.veto_gen = new VetoableGenerator<String>(
      FileNameGenerator.this.text_noslash_gen)
    {
      @Override
      protected boolean tryValue(final String value)
      {
        return value.isEmpty() == false;
      }
    };
  }

  @Override
  public String next()
  {
    return this.veto_gen.next();
  }
}
