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

import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwhere.gui.model.Model;
import com.io7m.jwhere.gui.view.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

/**
 * The main GUI program.
 */

public final class GUIMain
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(GUIMain.class);
  }

  private GUIMain()
  {
    throw new UnreachableCodeException();
  }

  /**
   * The main entry point.
   *
   * @param args Command line arguments
   */

  public static void main(final String[] args)
  {
    Thread.setDefaultUncaughtExceptionHandler(
      (t, e) -> LOG.error("uncaught exception: thread [{}]: ", t, e));

    try {
      UIManager.setLookAndFeel(
        UIManager.getSystemLookAndFeelClassName());

    } catch (final ClassNotFoundException | UnsupportedLookAndFeelException
      | IllegalAccessException | InstantiationException e) {
      LOG.error("unable to set look and feel: ", e);
    }

    final var model = new Model();
    final var controller = Controller.newController(model);

    SwingUtilities.invokeLater(
      () -> {
        final var w = new MainWindow(controller);
        w.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        w.pack();
        w.setVisible(true);
      });
  }
}
