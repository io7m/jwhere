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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class FileNameCharacterGenerator implements Generator<Character>
{
  private final Set<Integer> disallowed_codepoints;

  public FileNameCharacterGenerator(final Set<Integer> in_disallowed_codepoints)
  {
    this.disallowed_codepoints = NullCheck.notNull(in_disallowed_codepoints);
  }

  public static FileNameCharacterGenerator getDefault()
  {
    final Set<Integer> dc = new HashSet<>(10);
    dc.add(Integer.valueOf(0));
    dc.add(Integer.valueOf((int) '\\'));
    dc.add(Integer.valueOf((int) '/'));
    dc.add(Integer.valueOf((int) ':'));
    dc.add(Integer.valueOf((int) '*'));
    dc.add(Integer.valueOf((int) '?'));
    dc.add(Integer.valueOf((int) '"'));
    dc.add(Integer.valueOf((int) '<'));
    dc.add(Integer.valueOf((int) '>'));
    dc.add(Integer.valueOf((int) '|'));
    return new FileNameCharacterGenerator(dc);
  }

  @Override public Character next()
  {
    final Random r = new Random();
    while (true) {
      final int c = r.nextInt(Character.MAX_VALUE);
      if (Character.isValidCodePoint(c) && !this.disallowed_codepoints.contains(
        Integer.valueOf(c))) {
        return Character.valueOf((char) c);
      }
    }
  }
}
