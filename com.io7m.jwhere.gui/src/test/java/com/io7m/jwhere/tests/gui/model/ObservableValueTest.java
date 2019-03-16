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

package com.io7m.jwhere.tests.gui.model;

import com.io7m.jwhere.gui.model.ObservableValue;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class ObservableValueTest
{
  @Rule public ExpectedException expected = ExpectedException.none();

  @Test public void testObservablePartial()
  {
    final ObservableValue<Integer> v = new ObservableValue<>(
      () -> {
        throw new IllegalStateException("ZOOM!");
      });

    this.expected.expect(IllegalStateException.class);
    this.expected.expectMessage("ZOOM!");
    v.broadcast();
  }

  @Test public void testObservableAddRemove()
  {
    final AtomicInteger x = new AtomicInteger(0);
    final AtomicInteger calls = new AtomicInteger(0);

    final ObservableValue<Integer> v =
      new ObservableValue<>(() -> Integer.valueOf(x.get()));

    final Consumer<Integer> c0 = (z) -> {
      final Integer k0 = Integer.valueOf(x.get());
      final Integer k1 = z;
      calls.incrementAndGet();
      Assert.assertEquals(k0, k1);
    };

    final Consumer<Integer> c1 = (z) -> {
      final Integer k0 = Integer.valueOf(x.get());
      final Integer k1 = z;
      calls.incrementAndGet();
      Assert.assertEquals(k0, k1);
    };

    final Consumer<Integer> c2 = (z) -> {
      final Integer k0 = Integer.valueOf(x.get());
      final Integer k1 = z;
      calls.incrementAndGet();
      Assert.assertEquals(k0, k1);
    };

    v.addObserver(c0);
    Assert.assertEquals(1L, (long) calls.get());
    v.addObserver(c1);
    Assert.assertEquals(2L, (long) calls.get());
    v.addObserver(c2);
    Assert.assertEquals(3L, (long) calls.get());

    v.broadcast();
    Assert.assertEquals(6L, (long) calls.get());

    v.removeObserver(c0);
    v.removeObserver(c1);
    v.removeObserver(c2);
    Assert.assertEquals(6L, (long) calls.get());
  }
}
