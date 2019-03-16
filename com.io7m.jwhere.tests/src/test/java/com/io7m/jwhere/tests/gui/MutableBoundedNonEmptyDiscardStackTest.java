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

package com.io7m.jwhere.tests.gui;

import com.io7m.jwhere.gui.model.MutableBoundedNonEmptyDiscardStack;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.valid4j.exceptions.RequireViolation;

public final class MutableBoundedNonEmptyDiscardStackTest
{
  @Rule public ExpectedException expected = ExpectedException.none();

  @Test
  public void testLowBound()
  {
    this.expected.expect(RequireViolation.class);
    new MutableBoundedNonEmptyDiscardStack<>(new Object(), 1);
  }

  @Test
  public void testPushPop()
  {
    final MutableBoundedNonEmptyDiscardStack<Integer> stack =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);

    stack.push(Integer.valueOf(1));
    stack.push(Integer.valueOf(2));
    stack.push(Integer.valueOf(3));
    stack.push(Integer.valueOf(4));

    Assert.assertEquals(5L, (long) stack.size());
    Assert.assertEquals(Integer.valueOf(4), stack.pop());
    Assert.assertEquals(4L, (long) stack.size());
    Assert.assertEquals(Integer.valueOf(3), stack.pop());
    Assert.assertEquals(3L, (long) stack.size());
    Assert.assertEquals(Integer.valueOf(2), stack.pop());
    Assert.assertEquals(2L, (long) stack.size());
    Assert.assertEquals(Integer.valueOf(1), stack.pop());
    Assert.assertEquals(1L, (long) stack.size());

    Assert.assertEquals(Integer.valueOf(0), stack.peek());
    Assert.assertEquals(1L, (long) stack.size());
  }

  @Test
  public void testPushPopOverflow()
  {
    final MutableBoundedNonEmptyDiscardStack<Integer> stack =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);

    stack.push(Integer.valueOf(1));
    stack.push(Integer.valueOf(2));
    stack.push(Integer.valueOf(3));
    stack.push(Integer.valueOf(4));
    stack.push(Integer.valueOf(5));
    stack.push(Integer.valueOf(6));
    stack.push(Integer.valueOf(7));
    stack.push(Integer.valueOf(8));
    stack.push(Integer.valueOf(9));
    stack.push(Integer.valueOf(10));

    Assert.assertEquals(5L, (long) stack.size());
    Assert.assertEquals(Integer.valueOf(10), stack.pop());
    Assert.assertEquals(4L, (long) stack.size());
    Assert.assertEquals(Integer.valueOf(9), stack.pop());
    Assert.assertEquals(3L, (long) stack.size());
    Assert.assertEquals(Integer.valueOf(8), stack.pop());
    Assert.assertEquals(2L, (long) stack.size());
    Assert.assertEquals(Integer.valueOf(7), stack.pop());
    Assert.assertEquals(1L, (long) stack.size());

    Assert.assertEquals(Integer.valueOf(6), stack.peek());
    Assert.assertEquals(1L, (long) stack.size());
  }

  @Test
  public void testClear()
  {
    final MutableBoundedNonEmptyDiscardStack<Integer> stack =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);

    stack.push(Integer.valueOf(1));
    stack.push(Integer.valueOf(2));
    stack.push(Integer.valueOf(3));
    stack.push(Integer.valueOf(4));
    stack.push(Integer.valueOf(5));

    Assert.assertEquals(5L, (long) stack.size());

    stack.clear(Integer.valueOf(23));
    Assert.assertEquals(1L, (long) stack.size());
    Assert.assertEquals(23L, stack.peek().longValue());
  }

  @Test
  public void testEquals()
  {
    final MutableBoundedNonEmptyDiscardStack<Integer> s0 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);

    s0.push(Integer.valueOf(1));
    s0.push(Integer.valueOf(2));
    s0.push(Integer.valueOf(3));
    s0.push(Integer.valueOf(4));

    final MutableBoundedNonEmptyDiscardStack<Integer> s1 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);

    s1.push(Integer.valueOf(1));
    s1.push(Integer.valueOf(2));
    s1.push(Integer.valueOf(3));
    s1.push(Integer.valueOf(4));

    final MutableBoundedNonEmptyDiscardStack<Integer> s2 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);

    final MutableBoundedNonEmptyDiscardStack<Integer> s3 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);

    s3.push(Integer.valueOf(1));
    s3.push(Integer.valueOf(2));
    s3.push(Integer.valueOf(3));
    s3.push(Integer.valueOf(4));

    // Reflexive
    Assert.assertEquals(s0, s0);

    // Symmetric
    Assert.assertEquals(s0, s1);
    Assert.assertEquals(s1, s0);

    // Transitive
    Assert.assertEquals(s0, s1);
    Assert.assertEquals(s1, s3);
    Assert.assertEquals(s0, s3);

    Assert.assertNotEquals(s0, s2);
    Assert.assertNotEquals(s0, null);
    Assert.assertNotEquals(s0, Integer.valueOf(23));

    Assert.assertEquals((long) s0.hashCode(), (long) s1.hashCode());
  }

  @Test
  public void testEqualsNot_0()
  {
    final MutableBoundedNonEmptyDiscardStack<Integer> s0 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);
    final MutableBoundedNonEmptyDiscardStack<Integer> s1 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 4);

    Assert.assertNotEquals(s0, s1);
  }

  @Test
  public void testEqualsNot_1()
  {
    final MutableBoundedNonEmptyDiscardStack<Integer> s0 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);
    final MutableBoundedNonEmptyDiscardStack<Integer> s1 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);

    s0.push(Integer.valueOf(0));
    s1.push(Integer.valueOf(1));

    Assert.assertNotEquals(s0, s1);
  }

  @Test
  public void testEqualsNot_2()
  {
    final MutableBoundedNonEmptyDiscardStack<Integer> s0 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);
    final MutableBoundedNonEmptyDiscardStack<Integer> s1 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);

    s0.push(Integer.valueOf(0));

    Assert.assertNotEquals(s0, s1);
  }

  @Test
  public void testEqualsNot_3()
  {
    final MutableBoundedNonEmptyDiscardStack<Integer> s0 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);
    final MutableBoundedNonEmptyDiscardStack<Integer> s1 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);

    s0.push(Integer.valueOf(0));
    s0.push(Integer.valueOf(0));

    s1.push(Integer.valueOf(0));

    Assert.assertNotEquals(s0, s1);
  }

  @Test
  public void testEqualsNot_4()
  {
    final MutableBoundedNonEmptyDiscardStack<Integer> s0 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(0), 5);
    final MutableBoundedNonEmptyDiscardStack<Integer> s1 =
      new MutableBoundedNonEmptyDiscardStack<>(Integer.valueOf(1), 5);

    s0.push(Integer.valueOf(0));
    s1.push(Integer.valueOf(0));

    Assert.assertNotEquals(s0, s1);
  }
}
