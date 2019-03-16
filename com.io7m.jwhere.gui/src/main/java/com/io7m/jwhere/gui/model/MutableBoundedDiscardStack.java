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

import org.valid4j.Assertive;

import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A mutable bounded stack that discards the oldest elements on overflow and always contains at
 * least one element.
 *
 * @param <T> The type of elements
 */

public final class MutableBoundedDiscardStack<T>
  implements MutableBoundedDiscardStackType<T>
{
  private final Deque<T> stack;
  private final int bound;

  /**
   * Construct a stack.
   *
   * @param in_bound The maximum stack size (must be {@code >= 1})
   */

  public MutableBoundedDiscardStack(
    final int in_bound)
  {
    Assertive.require(
      in_bound >= 1, "Bound %d must be >= 1", Integer.valueOf(in_bound));

    this.stack = new ConcurrentLinkedDeque<>();
    this.bound = in_bound;
  }

  @Override
  public void clear()
  {
    this.stack.clear();
  }

  @Override
  public Optional<T> peek()
  {
    if (this.stack.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(this.stack.peek());
  }

  @Override
  public Optional<T> pop()
  {
    if (this.stack.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(this.stack.pop());
  }

  @Override
  public void push(final T x)
  {
    Objects.requireNonNull(x, "x");
    if (this.size() == this.bound) {
      this.stack.removeLast();
    }
    this.stack.push(x);
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final MutableBoundedDiscardStack<?> that =
      (MutableBoundedDiscardStack<?>) o;

    if (this.bound != that.bound) {
      return false;
    }

    {
      final Iterator<T> i0 = this.stack.iterator();
      final Iterator<?> i1 = that.stack.iterator();

      while (i0.hasNext()) {
        if (!i1.hasNext()) {
          return false;
        }
        final T x = i0.next();
        final Object y = i1.next();
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
    int result = this.hashStack();
    result = 31 * result + this.bound;
    return result;
  }

  private int hashStack()
  {
    int hash = 17;
    final Iterator<T> i0 = this.stack.iterator();
    while (i0.hasNext()) {
      final T x = i0.next();
      hash = hash + x.hashCode();
    }
    return hash;
  }

  @Override
  public int size()
  {
    return this.stack.size();
  }
}
