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

import java.util.Objects;
import com.io7m.jwhere.gui.ControllerType;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;

final class StatusBar extends JPanel
{
  private final JLabel       text;
  private final JLabel       error_icon;
  private final JProgressBar progress;

  StatusBar(final ControllerType in_controller)
  {
    Objects.requireNonNull(in_controller, "in_controller");

    this.setBorder(new LineBorder(this.getBackground(), 1));
    this.setPreferredSize(new Dimension(this.getWidth(), 24));

    this.progress = new JProgressBar();
    this.error_icon = new JLabel(Icons.getWarningIcon16());
    this.text = new JLabel();
    this.text.setFont(Fonts.getMonospacedSmall());
    this.text.setHorizontalAlignment(SwingConstants.LEFT);
    this.text.setText("");

    final BorderLayout layout = new BorderLayout(4, 4);
    this.setLayout(layout);

    this.error_icon.setBorder(new EmptyBorder(0, 4, 0, 4));
    this.text.setBorder(new EmptyBorder(0, 4, 0, 4));

    this.add(this.error_icon, BorderLayout.LINE_START);
    this.add(this.text, BorderLayout.CENTER);
    this.add(this.progress, BorderLayout.LINE_END);

    this.text.setVisible(false);
    this.error_icon.setVisible(false);
    this.progress.setVisible(false);
  }

  public void onErrorLater(final String message)
  {
    SwingUtilities.invokeLater(() -> this.onError(message));
  }

  public void onInfoLater(final String message)
  {
    SwingUtilities.invokeLater(() -> this.onInfo(message));
  }

  public void onProgressIndeterminateStartLater()
  {
    SwingUtilities.invokeLater(() -> this.onProgressIndeterminateStart());
  }

  public void onProgressIndeterminateFinishLater()
  {
    SwingUtilities.invokeLater(() -> this.onProgressIndeterminateFinish());
  }

  private void onProgressIndeterminateFinish()
  {
    this.progress.setVisible(false);
    this.validate();
  }

  private void onProgressIndeterminateStart()
  {
    this.progress.setVisible(true);
    this.progress.setIndeterminate(true);
    this.validate();
  }

  private void onInfo(final String message)
  {
    this.text.setText(message);
    this.text.setVisible(true);
    this.error_icon.setVisible(false);
    this.validate();
  }

  private void onError(final String message)
  {
    this.text.setText(message);
    this.text.setVisible(true);
    this.error_icon.setVisible(true);
    this.validate();
  }
}
