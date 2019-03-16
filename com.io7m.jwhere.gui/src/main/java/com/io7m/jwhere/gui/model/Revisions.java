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

import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A versioned data structure storing revisions of a given immutable value, with undo and redo
 * support.
 *
 * Essentially, the structure maintains a bounded stack of {@code N} revisions. Each new value
 * pushed into the history removes the oldest revision. Up to {@code N} revisions can be "undone" by
 * popping the most recent revision from the stack. An "undone" operation can be "redone" by pushing
 * the popped revision back onto the stack. The structure assigns a monotonically increasing version
 * number to each new revision. The current revision can be marked as "saved" (with the initial
 * revision implicitly being marked as such), and each new revision from that point on is considered
 * to be "unsaved" data, until the next time a revision is "saved".
 *
 * @param <T> The type of values
 */

public final class Revisions<T>
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(Revisions.class);
  }

  private final MutableBoundedNonEmptyDiscardStack<Element<T>> history;
  private final MutableBoundedDiscardStack<Element<T>> redo;
  private final ObservableValue<UnsavedChanges> watch_unsaved;
  private final ObservableValue<UndoAvailable> watch_undo;
  private final ObservableValue<RedoAvailable> watch_redo;
  private BigInteger saved;

  /**
   * Construct a new version history with the starting value of {@code initial} and storing {@code
   * count} undo and redo versions.
   *
   * @param initial The initial value
   * @param count   The number of undo and redo versions
   */

  public Revisions(
    final T initial,
    final int count)
  {
    Objects.requireNonNull(initial, "initial");
    this.history = new MutableBoundedNonEmptyDiscardStack<>(
      new Element<>(BigInteger.ZERO, initial), count);
    this.redo = new MutableBoundedDiscardStack<>(count);
    this.saved = BigInteger.ZERO;

    // Checkstyle is currently choking on these definitions, claiming
    // that an explicit 'this' is required.
    // CHECKSTYLE:OFF
    this.watch_unsaved = new ObservableValue<>(this::hasUnsavedChanges);
    this.watch_undo = new ObservableValue<>(this::hasUndo);
    this.watch_redo = new ObservableValue<>(this::hasRedo);
    // CHECKSTYLE:ON
  }

  /**
   * Subscribe to changes in the unsaved state.
   *
   * @param o The consumer of events
   */

  public void subscribeUnsaved(final Consumer<UnsavedChanges> o)
  {
    this.watch_unsaved.addObserver(o);
  }

  /**
   * Subscribe to changes in the undo availability state.
   *
   * @param o The consumer of events
   */

  public void subscribeUndo(final Consumer<UndoAvailable> o)
  {
    this.watch_undo.addObserver(o);
  }

  /**
   * Subscribe to changes in the redo availability state.
   *
   * @param o The consumer of events
   */

  public void subscribeRedo(final Consumer<RedoAvailable> o)
  {
    this.watch_redo.addObserver(o);
  }

  /**
   * Unsubscribe the given consumer of unsaved changes events. Has no effect if the consumer is not
   * already subscribed.
   *
   * @param o The consumer of events
   */

  public void unsubscribeUnsaved(final Consumer<UnsavedChanges> o)
  {
    this.watch_unsaved.removeObserver(o);
  }

  /**
   * Unsubscribe the given consumer of undo state change events. Has no effect if the consumer is
   * not already subscribed.
   *
   * @param o The consumer of events
   */

  public void unsubscribeUndo(final Consumer<UndoAvailable> o)
  {
    this.watch_undo.removeObserver(o);
  }

  /**
   * Unsubscribe the given consumer of redo state change events. Has no effect if the consumer is
   * not already subscribed.
   *
   * @param o The consumer of events
   */

  public void unsubscribeRedo(final Consumer<RedoAvailable> o)
  {
    this.watch_redo.removeObserver(o);
  }

  /**
   * @return The current version number
   */

  public BigInteger getCurrentRevision()
  {
    return this.history.peek().revision;
  }

  /**
   * @return The current value
   */

  public T getCurrentValue()
  {
    return this.history.peek().value;
  }

  /**
   * Push a new version of the value, {@code x}, into the history.
   *
   * @param x The new value
   */

  public void newRevision(final T x)
  {
    Objects.requireNonNull(x, "x");
    final var current = this.history.peek();
    final var next_id = current.revision.add(BigInteger.ONE);
    this.history.push(new Element<>(next_id, x));
    this.broadcast();
  }

  /**
   * Redo the most recent undo operation.
   */

  public void redo()
  {
    final var current = this.history.peek();
    LOG.debug("redo: requesting at revision {}", current.revision);

    if (this.hasRedo() == RedoAvailable.REDO_AVAILABLE) {
      final var redo_current = this.redo.pop().get();
      this.history.push(redo_current);
      final var new_current = this.history.peek();
      LOG.debug("redo: now at revision {}", new_current.revision);
      this.broadcast();
    } else {
      LOG.debug("redo: nothing to redo");
    }
  }

  private void broadcast()
  {
    this.watch_redo.broadcast();
    this.watch_undo.broadcast();
    this.watch_unsaved.broadcast();
  }

  /**
   * Undo the most recent revision.
   */

  public void undo()
  {
    final var current = this.history.peek();
    LOG.debug("undo: requesting at revision {}", current.revision);

    if (this.hasUndo() == UndoAvailable.UNDO_AVAILABLE) {
      this.history.pop();
      this.redo.push(current);
      final var new_current = this.history.peek();
      LOG.debug("undo: now at revision {}", new_current.revision);
      this.broadcast();
    } else {
      LOG.debug("undo: nothing to undo");
    }
  }

  /**
   * @return An indication of whether or not changes have been made since the last time {@link
   * #save()} was called.
   */

  public UnsavedChanges hasUnsavedChanges()
  {
    final var current_revision = this.getCurrentRevision();
    if (current_revision.equals(this.saved)) {
      return UnsavedChanges.NO_UNSAVED_CHANGES;
    } else {
      return UnsavedChanges.UNSAVED_CHANGES;
    }
  }

  /**
   * Discard all history and reset to the initial value {@code x}.
   *
   * @param x The new value
   */

  public void reset(final T x)
  {
    Objects.requireNonNull(x, "x");
    this.history.clear(new Element<>(BigInteger.ZERO, x));
    this.redo.clear();
    this.saved = BigInteger.ZERO;
    this.broadcast();
  }

  /**
   * @return An indication of whether or not changes have been made that can be undone.
   */

  public UndoAvailable hasUndo()
  {
    if (this.history.size() > 1) {
      return UndoAvailable.UNDO_AVAILABLE;
    } else {
      return UndoAvailable.UNDO_UNAVAILABLE;
    }
  }

  /**
   * @return An indication of whether or not undo operations have been performed that can be redone.
   */

  public RedoAvailable hasRedo()
  {
    if (this.redo.size() > 0) {
      return RedoAvailable.REDO_AVAILABLE;
    } else {
      return RedoAvailable.REDO_UNAVAILABLE;
    }
  }

  /**
   * Mark the current revision as having been saved.
   */

  public void save()
  {
    this.saved = this.getCurrentRevision();
    this.broadcast();
  }

  @Immutable
  private static final class Element<T>
  {
    private final T value;
    private final BigInteger revision;

    Element(
      final BigInteger in_revision,
      final T in_value)
    {
      this.revision = Objects.requireNonNull(in_revision, "in_revision");
      this.value = Objects.requireNonNull(in_value, "in_value");
    }
  }
}
