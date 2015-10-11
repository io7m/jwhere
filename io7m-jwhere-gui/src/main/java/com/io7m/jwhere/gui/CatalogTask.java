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

package com.io7m.jwhere.gui;

import com.io7m.jnull.NullCheck;

import java.util.concurrent.Future;

public final class CatalogTask
{
  private final Long      id;
  private final String    name;
  private final Future<?> future;

  CatalogTask(
    final Future<?> in_future,
    final Long in_id,
    final String in_name)
  {
    this.future = NullCheck.notNull(in_future);
    this.id = NullCheck.notNull(in_id);
    this.name = NullCheck.notNull(in_name);
  }

  public Future<?> getFuture()
  {
    return this.future;
  }

  public Long getID()
  {
    return this.id;
  }

  public String getName()
  {
    return this.name;
  }

  @Override public String toString()
  {
    final StringBuilder sb = new StringBuilder("Task{");
    sb.append("id=").append(this.id);
    sb.append(", name='").append(this.name).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
