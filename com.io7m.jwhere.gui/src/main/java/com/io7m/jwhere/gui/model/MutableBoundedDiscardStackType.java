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

import java.util.Optional;

/**
 * The type of mutable bounded stacks that discard the oldest elements on
 * overflow and always contain at least one element.
 *
 * @param <T> The type of elements
 */

public interface MutableBoundedDiscardStackType<T>
{
  /**
   * Clear the stack.
   */

  void clear();

  /**
   * @return The top element of the stack, without removing it from the stack
   */

  Optional<T> peek();

  /**
   * @return The top element of the stack, removing it from the stack
   */

  Optional<T> pop();

  /**
   * Push an element onto the stack, discarding the oldest element on the stack
   * if the resulting stack would be larger than the bound.
   *
   * @param x The new element
   */

  void push(T x);

  /**
   * @return The current size of the stack
   */

  int size();
}
