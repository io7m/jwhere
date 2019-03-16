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

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A typed observable value that broadcasts state changes to observers.
 *
 * @param <T> The type of value
 */

public final class ObservableValue<T>
{
  private final List<Consumer<T>> observers;
  private final Supplier<T>       supplier;

  /**
   * Construct an observable value that will evaluate {@code s} every time
   * {@link #broadcast()} is called.
   *
   * @param s The state function
   */

  public ObservableValue(final Supplier<T> s)
  {
    this.supplier = NullCheck.notNull(s);
    this.observers = new LinkedList<>();
  }

  /**
   * Notify all observers that the observed state has changed.
   */

  public void broadcast()
  {
    final T now = this.supplier.get();

    synchronized (this.observers) {
      for (int index = 0; index < this.observers.size(); ++index) {
        this.observers.get(index).accept(now);
      }
    }
  }

  /**
   * Add the observer {@code c} to the list of observers. The observer {@code c}
   * will immediately receive the current state value.
   *
   * @param c The new observer
   */

  public void addObserver(final Consumer<T> c)
  {
    synchronized (this.observers) {
      this.observers.add(NullCheck.notNull(c));
    }

    c.accept(this.supplier.get());
  }

  /**
   * Remove the observer {@code c} from the list of observers. Has no effect if
   * the observer is not in the list.
   *
   * @param c The observer
   */

  public void removeObserver(final Consumer<T> c)
  {
    synchronized (this.observers) {
      this.observers.remove(NullCheck.notNull(c));
    }
  }
}
