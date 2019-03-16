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

import com.io7m.jwhere.gui.model.RedoAvailable;
import com.io7m.jwhere.gui.model.Revisions;
import com.io7m.jwhere.gui.model.UndoAvailable;
import com.io7m.jwhere.gui.model.UnsavedChanges;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class RevisionsTest
{
  @Test
  public void testEmpty()
  {
    final Revisions<Integer> r = new Revisions<>(Integer.valueOf(0), 5);

    Assert.assertEquals(UndoAvailable.UNDO_UNAVAILABLE, r.hasUndo());
    Assert.assertEquals(RedoAvailable.REDO_UNAVAILABLE, r.hasRedo());
    Assert.assertEquals(
      UnsavedChanges.NO_UNSAVED_CHANGES, r.hasUnsavedChanges());
    Assert.assertEquals(Integer.valueOf(0), r.getCurrentValue());
    Assert.assertEquals(BigInteger.ZERO, r.getCurrentRevision());
  }

  @Test
  public void testNewUndo()
  {
    final Revisions<Integer> r = new Revisions<>(Integer.valueOf(0), 5);

    Assert.assertEquals(UndoAvailable.UNDO_UNAVAILABLE, r.hasUndo());
    Assert.assertEquals(RedoAvailable.REDO_UNAVAILABLE, r.hasRedo());
    Assert.assertEquals(
      UnsavedChanges.NO_UNSAVED_CHANGES, r.hasUnsavedChanges());
    Assert.assertEquals(Integer.valueOf(0), r.getCurrentValue());
    Assert.assertEquals(BigInteger.ZERO, r.getCurrentRevision());

    r.newRevision(Integer.valueOf(10));

    Assert.assertEquals(UndoAvailable.UNDO_AVAILABLE, r.hasUndo());
    Assert.assertEquals(RedoAvailable.REDO_UNAVAILABLE, r.hasRedo());
    Assert.assertEquals(
      UnsavedChanges.UNSAVED_CHANGES, r.hasUnsavedChanges());
    Assert.assertEquals(Integer.valueOf(10), r.getCurrentValue());
    Assert.assertEquals(BigInteger.ONE, r.getCurrentRevision());

    r.undo();

    Assert.assertEquals(UndoAvailable.UNDO_UNAVAILABLE, r.hasUndo());
    Assert.assertEquals(RedoAvailable.REDO_AVAILABLE, r.hasRedo());
    Assert.assertEquals(
      UnsavedChanges.NO_UNSAVED_CHANGES, r.hasUnsavedChanges());
    Assert.assertEquals(Integer.valueOf(0), r.getCurrentValue());
    Assert.assertEquals(BigInteger.ZERO, r.getCurrentRevision());

    r.undo();

    Assert.assertEquals(UndoAvailable.UNDO_UNAVAILABLE, r.hasUndo());
    Assert.assertEquals(RedoAvailable.REDO_AVAILABLE, r.hasRedo());
    Assert.assertEquals(
      UnsavedChanges.NO_UNSAVED_CHANGES, r.hasUnsavedChanges());
    Assert.assertEquals(Integer.valueOf(0), r.getCurrentValue());
    Assert.assertEquals(BigInteger.ZERO, r.getCurrentRevision());
  }

  @Test
  public void testReset()
  {
    final Revisions<Integer> r = new Revisions<>(Integer.valueOf(0), 5);

    Assert.assertEquals(UndoAvailable.UNDO_UNAVAILABLE, r.hasUndo());
    Assert.assertEquals(RedoAvailable.REDO_UNAVAILABLE, r.hasRedo());
    Assert.assertEquals(
      UnsavedChanges.NO_UNSAVED_CHANGES, r.hasUnsavedChanges());
    Assert.assertEquals(Integer.valueOf(0), r.getCurrentValue());
    Assert.assertEquals(BigInteger.ZERO, r.getCurrentRevision());

    r.newRevision(Integer.valueOf(10));

    Assert.assertEquals(UndoAvailable.UNDO_AVAILABLE, r.hasUndo());
    Assert.assertEquals(RedoAvailable.REDO_UNAVAILABLE, r.hasRedo());
    Assert.assertEquals(
      UnsavedChanges.UNSAVED_CHANGES, r.hasUnsavedChanges());
    Assert.assertEquals(Integer.valueOf(10), r.getCurrentValue());
    Assert.assertEquals(BigInteger.ONE, r.getCurrentRevision());

    r.reset(Integer.valueOf(20));

    Assert.assertEquals(UndoAvailable.UNDO_UNAVAILABLE, r.hasUndo());
    Assert.assertEquals(RedoAvailable.REDO_UNAVAILABLE, r.hasRedo());
    Assert.assertEquals(
      UnsavedChanges.NO_UNSAVED_CHANGES, r.hasUnsavedChanges());
    Assert.assertEquals(Integer.valueOf(20), r.getCurrentValue());
    Assert.assertEquals(BigInteger.ZERO, r.getCurrentRevision());
  }

  @Test
  public void testUndoRedo()
  {
    final Revisions<Integer> r = new Revisions<>(Integer.valueOf(0), 5);

    r.newRevision(Integer.valueOf(10));
    r.undo();

    Assert.assertEquals(UndoAvailable.UNDO_UNAVAILABLE, r.hasUndo());
    Assert.assertEquals(RedoAvailable.REDO_AVAILABLE, r.hasRedo());
    Assert.assertEquals(
      UnsavedChanges.NO_UNSAVED_CHANGES, r.hasUnsavedChanges());
    Assert.assertEquals(Integer.valueOf(0), r.getCurrentValue());
    Assert.assertEquals(BigInteger.ZERO, r.getCurrentRevision());

    r.redo();

    Assert.assertEquals(UndoAvailable.UNDO_AVAILABLE, r.hasUndo());
    Assert.assertEquals(RedoAvailable.REDO_UNAVAILABLE, r.hasRedo());
    Assert.assertEquals(UnsavedChanges.UNSAVED_CHANGES, r.hasUnsavedChanges());
    Assert.assertEquals(Integer.valueOf(10), r.getCurrentValue());
    Assert.assertEquals(BigInteger.ONE, r.getCurrentRevision());

    r.redo();

    Assert.assertEquals(UndoAvailable.UNDO_AVAILABLE, r.hasUndo());
    Assert.assertEquals(RedoAvailable.REDO_UNAVAILABLE, r.hasRedo());
    Assert.assertEquals(UnsavedChanges.UNSAVED_CHANGES, r.hasUnsavedChanges());
    Assert.assertEquals(Integer.valueOf(10), r.getCurrentValue());
    Assert.assertEquals(BigInteger.ONE, r.getCurrentRevision());
  }

  @Test
  public void testSubscribeUnsaved()
  {
    final AtomicInteger calls = new AtomicInteger(0);
    final Revisions<Integer> r = new Revisions<>(Integer.valueOf(0), 5);

    final Consumer<UnsavedChanges> subscriber = u -> {
      calls.incrementAndGet();
      Assert.assertEquals(u, r.hasUnsavedChanges());
    };

    r.subscribeUnsaved(subscriber);
    Assert.assertEquals(1L, (long) calls.get());
    r.newRevision(Integer.valueOf(10));
    Assert.assertEquals(2L, (long) calls.get());
    r.undo();
    Assert.assertEquals(3L, (long) calls.get());

    r.unsubscribeUnsaved(subscriber);

    r.newRevision(Integer.valueOf(10));
    Assert.assertEquals(3L, (long) calls.get());
    r.undo();
    Assert.assertEquals(3L, (long) calls.get());
  }

  @Test
  public void testSubscribeUndo()
  {
    final AtomicInteger calls = new AtomicInteger(0);
    final Revisions<Integer> r = new Revisions<>(Integer.valueOf(0), 5);

    final Consumer<UndoAvailable> subscriber = u -> {
      calls.incrementAndGet();
      Assert.assertEquals(u, r.hasUndo());
    };

    r.subscribeUndo(subscriber);
    Assert.assertEquals(1L, (long) calls.get());
    r.newRevision(Integer.valueOf(10));
    Assert.assertEquals(2L, (long) calls.get());
    r.undo();
    Assert.assertEquals(3L, (long) calls.get());

    r.unsubscribeUndo(subscriber);

    r.newRevision(Integer.valueOf(10));
    Assert.assertEquals(3L, (long) calls.get());
    r.undo();
    Assert.assertEquals(3L, (long) calls.get());
  }

  @Test
  public void testSubscribeRedo()
  {
    final AtomicInteger calls = new AtomicInteger(0);
    final Revisions<Integer> r = new Revisions<>(Integer.valueOf(0), 5);

    final Consumer<RedoAvailable> subscriber = u -> {
      calls.incrementAndGet();
      Assert.assertEquals(u, r.hasRedo());
    };

    r.subscribeRedo(subscriber);
    Assert.assertEquals(1L, (long) calls.get());
    r.newRevision(Integer.valueOf(10));
    Assert.assertEquals(2L, (long) calls.get());
    r.undo();
    Assert.assertEquals(3L, (long) calls.get());
    r.redo();
    Assert.assertEquals(4L, (long) calls.get());

    r.unsubscribeRedo(subscriber);

    r.newRevision(Integer.valueOf(10));
    Assert.assertEquals(4L, (long) calls.get());
    r.undo();
    Assert.assertEquals(4L, (long) calls.get());
    r.redo();
    Assert.assertEquals(4L, (long) calls.get());
  }

  @Test
  public void testSave()
  {
    final Revisions<Integer> r = new Revisions<>(Integer.valueOf(0), 5);

    r.newRevision(Integer.valueOf(10));

    Assert.assertEquals(UndoAvailable.UNDO_AVAILABLE, r.hasUndo());
    Assert.assertEquals(RedoAvailable.REDO_UNAVAILABLE, r.hasRedo());
    Assert.assertEquals(
      UnsavedChanges.UNSAVED_CHANGES, r.hasUnsavedChanges());
    Assert.assertEquals(Integer.valueOf(10), r.getCurrentValue());
    Assert.assertEquals(BigInteger.ONE, r.getCurrentRevision());

    r.save();

    Assert.assertEquals(UndoAvailable.UNDO_AVAILABLE, r.hasUndo());
    Assert.assertEquals(RedoAvailable.REDO_UNAVAILABLE, r.hasRedo());
    Assert.assertEquals(
      UnsavedChanges.NO_UNSAVED_CHANGES, r.hasUnsavedChanges());
    Assert.assertEquals(Integer.valueOf(10), r.getCurrentValue());
    Assert.assertEquals(BigInteger.ONE, r.getCurrentRevision());
  }
}
