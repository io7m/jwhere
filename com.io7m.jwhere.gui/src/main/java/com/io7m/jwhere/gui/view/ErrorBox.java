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
import net.java.dev.designgridlayout.DesignGridLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

/**
 * Error message functions.
 */

final class ErrorBox
{
  private ErrorBox()
  {
    throw new UnreachableCodeException();
  }

  private static JDialog showActualErrorBox(
    final String title,
    final String message,
    final JTextArea backtrace)
  {
    final var d = new JDialog();
    d.setTitle(title);

    final var ok = new JButton("OK");
    ok.addActionListener((x) -> WindowUtilities.closeDialog(d));

    final var icon = new JLabel(Icons.getErrorIcon());
    final var main = new JPanel();

    final var dg = new DesignGridLayout(main);
    dg.row().grid(icon).add(new JLabel(title));
    dg.emptyRow();

    if (backtrace != null) {
      final var pane = new JScrollPane(backtrace);
      pane.setPreferredSize(new Dimension(300, 200));
      backtrace.setCaretPosition(0);

      final var backtrace_note =
        new JLabel("The full error backtrace is as follows:");
      dg.emptyRow();
      dg.row().grid().add(backtrace_note);
      dg.emptyRow();
      dg.row().grid().add(pane);
    }

    dg.emptyRow();
    dg.row().right().add(ok);

    d.setContentPane(main);
    d.pack();
    d.setVisible(true);
    return d;
  }

  private static JDialog showActualErrorWithException(
    final String title,
    final String message,
    final Throwable e)
  {
    final var text = new JTextArea();
    text.setEditable(false);
    text.setText(showStackTraceText(e));
    text.setFont(Fonts.getMonospacedSmall());
    return showActualErrorBox(title, message, text);
  }

  public static JDialog showError(
    final Throwable e)
  {
    final var title = e.getClass().getCanonicalName();
    return showActualErrorWithException(
      title, e.getMessage(), e);
  }

  public static void showErrorLater(
    final Throwable e)
  {
    SwingUtilities.invokeLater(
      () -> showError(e));
  }

  public static JDialog showErrorWithoutException(
    final String title,
    final String message)
  {
    return showActualErrorBox(title, message, null);
  }

  public static void showErrorWithoutExceptionLater(
    final String title,
    final String message)
  {
    SwingUtilities.invokeLater(
      () -> {
        final var text = new JTextArea();
        text.setEditable(false);
        text.setText(message);
        text.setFont(Fonts.getMonospacedSmall());
        showActualErrorBox(title, message, text);
      });
  }

  public static JDialog showErrorWithTitle(
    final String title,
    final Throwable e)
  {
    return showActualErrorWithException(title, e.getMessage(), e);
  }

  public static void showErrorWithTitleLater(
    final String title,
    final Throwable e)
  {
    SwingUtilities.invokeLater(
      () -> showErrorWithTitle(title, e));
  }

  private static String showStackTraceText(
    final Throwable e)
  {
    try (var writer = new StringWriter()) {
      writer.append(e.getMessage());
      writer.append("\n");
      writer.append("\n");

      e.printStackTrace(new PrintWriter(writer));
      return Objects.requireNonNull(writer.toString(), "writer.toString()");
    } catch (final IOException x) {
      throw new UnreachableCodeException(x);
    }
  }
}
