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

import com.io7m.jnull.NullCheck;

/**
 * A directory entry in a given disk that corresponds to a file.
 */

public final class DirectoryEntryFile implements DirectoryEntryType
{
  private final String name;

  /**
   * Construct an entry.
   *
   * @param in_name The name of the entry
   */

  public DirectoryEntryFile(
    final String in_name)
  {
    this.name = NullCheck.notNull(in_name);
  }

  @Override public <A, E extends Exception> A matchEntry(
    final DirectoryEntryMatcherType<A, E> m)
    throws E
  {
    return m.onFile(this);
  }

  @Override public String getName()
  {
    return this.name;
  }
}
