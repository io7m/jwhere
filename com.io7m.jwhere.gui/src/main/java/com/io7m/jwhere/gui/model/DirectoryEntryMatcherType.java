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

/**
 * Match the type of directory entries.
 *
 * @param <A> The type of returned values
 * @param <E> The type of raised exceptions
 */

public interface DirectoryEntryMatcherType<A, E extends Exception>
{
  /**
   * Match an entry.
   *
   * @param n The entry
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onFile(DirectoryEntryFile n)
    throws E;

  /**
   * Match an entry.
   *
   * @param n The entry
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onDirectory(DirectoryEntryDirectory n)
    throws E;

  /**
   * Match an entry.
   *
   * @param n The entry
   *
   * @return A value of {@code A}
   *
   * @throws E If required
   */

  A onUp(DirectoryEntryUp n)
    throws E;
}
