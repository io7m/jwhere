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

package com.io7m.jwhere.gui.view;

import com.io7m.junreachable.UnreachableCodeException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import java.awt.event.WindowEvent;

/**
 * Window utility functions.
 */

final class WindowUtilities
{
  private WindowUtilities()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Send a {@link WindowEvent#WINDOW_CLOSING} event to the given dialog.
   */

  static void closeDialog(
    final JDialog dialog)
  {
    final WindowEvent ev =
      new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING);
    dialog.dispatchEvent(ev);
  }

  /**
   * Send a {@link WindowEvent#WINDOW_CLOSING} event to the given window.
   */

  static void closeWindow(
    final JFrame frame)
  {
    final WindowEvent ev = new WindowEvent(frame, WindowEvent.WINDOW_CLOSING);
    frame.dispatchEvent(ev);
  }
}