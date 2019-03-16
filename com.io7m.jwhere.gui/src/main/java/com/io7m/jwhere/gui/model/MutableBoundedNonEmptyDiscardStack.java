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

import com.io7m.jaffirm.core.Preconditions;

import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A mutable bounded stack that discards the oldest elements on overflow and always contains at
 * least one element.
 *
 * @param <T> The type of elements
 */

public final class MutableBoundedNonEmptyDiscardStack<T>
  implements MutableBoundedNonEmptyDiscardStackType<T>
{
  private final Deque<T> stack;
  private final int bound;
  private T top;

  /**
   * Construct a stack.
   *
   * @param in_top   The initial element
   * @param in_bound The maximum stack size (must be {@code > 1})
   */

  public MutableBoundedNonEmptyDiscardStack(
    final T in_top,
    final int in_bound)
  {
    Preconditions.checkPreconditionV(
      in_bound > 1, "Bound %d must be > 1", Integer.valueOf(in_bound));

    this.stack = new ConcurrentLinkedDeque<>();
    this.top = Objects.requireNonNull(in_top, "in_top");
    this.bound = in_bound;
  }

  @Override
  public void clear(final T x)
  {
    this.top = Objects.requireNonNull(x, "x");
    this.stack.clear();
  }

  @Override
  public T peek()
  {
    return this.top;
  }

  @Override
  public T pop()
  {
    Preconditions.checkPreconditionV(
      this.size() > 1,
      "Stack must have more than one element to pop and remain non-empty");

    final var old_top = this.top;
    this.top = this.stack.pop();
    return old_top;
  }

  @Override
  public void push(final T x)
  {
    Objects.requireNonNull(x, "x");
    if (this.size() == this.bound) {
      this.stack.removeLast();
    }
    this.stack.push(this.top);
    this.top = x;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || !Objects.equals(this.getClass(), o.getClass())) {
      return false;
    }

    final var that =
      (MutableBoundedNonEmptyDiscardStack<?>) o;

    if (this.bound != that.bound) {
      return false;
    }
    if (!this.top.equals(that.top)) {
      return false;
    }

    {
      final var i0 = this.stack.iterator();
      final var i1 = that.stack.iterator();

      while (i0.hasNext()) {
        if (!i1.hasNext()) {
          return false;
        }
        final var x = i0.next();
        final var y = i1.next();
        if (!x.equals(y)) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    var result = this.hashStack();
    result = 31 * result + this.bound;
    result = 31 * result + this.top.hashCode();
    return result;
  }

  private int hashStack()
  {
    var hash = 17;
    final var i0 = this.stack.iterator();
    while (i0.hasNext()) {
      final var x = i0.next();
      hash = hash + x.hashCode();
    }
    return hash;
  }

  @Override
  public int size()
  {
    return 1 + this.stack.size();
  }
}
