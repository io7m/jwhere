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

import java.util.Objects;
import java.util.concurrent.Future;

/**
 * The type of cancellable catalog tasks.
 */

public final class CatalogTask
{
  private final Long id;
  private final String name;
  private final Future<?> future;

  CatalogTask(
    final Future<?> in_future,
    final Long in_id,
    final String in_name)
  {
    this.future = Objects.requireNonNull(in_future, "in_future");
    this.id = Objects.requireNonNull(in_id, "in_id");
    this.name = Objects.requireNonNull(in_name, "in_name");
  }

  /**
   * @return The future representing the task in progress
   */

  public Future<?> getFuture()
  {
    return this.future;
  }

  /**
   * @return The ID of the task
   */

  public Long getID()
  {
    return this.id;
  }

  /**
   * @return The name of the task
   */

  public String getName()
  {
    return this.name;
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder("Task{");
    sb.append("id=").append(this.id);
    sb.append(", name='").append(this.name).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
