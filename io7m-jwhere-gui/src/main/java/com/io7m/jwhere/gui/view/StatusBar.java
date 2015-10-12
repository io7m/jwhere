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

import com.io7m.jnull.NullCheck;
import com.io7m.jwhere.gui.ControllerType;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

final class StatusBar extends JPanel
{
  private final JLabel         text;
  private final JLabel         error_icon;
  private final Component      padding;

  StatusBar(final ControllerType in_controller)
  {
    NullCheck.notNull(in_controller);

    this.setBorder(new LineBorder(this.getBackground(), 1));
    this.setPreferredSize(new Dimension(this.getWidth(), 24));

    this.padding = Box.createRigidArea(new Dimension(2, 1));

    this.error_icon = new JLabel(Icons.getWarningIcon16());
    this.text = new JLabel();
    this.text.setFont(Fonts.getMonospacedSmall());
    this.text.setHorizontalAlignment(SwingConstants.LEFT);
    this.text.setText("");

    final FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
    layout.setHgap(3);
    layout.setVgap(3);
    this.setLayout(layout);
    this.add(this.text);
  }

  public void onErrorLater(final String message)
  {
    SwingUtilities.invokeLater(() -> this.onError(message));
  }

  public void onInfoLater(final String message)
  {
    SwingUtilities.invokeLater(() -> this.onInfo(message));
  }

  private void onInfo(final String message)
  {
    this.remove(this.error_icon);
    this.remove(this.padding);
    this.remove(this.text);

    this.text.setText(message);
    this.add(this.text);
    this.validate();
  }

  private void onError(final String message)
  {
    this.remove(this.error_icon);
    this.remove(this.padding);
    this.remove(this.text);

    this.text.setText(message);
    this.add(this.error_icon);
    this.add(this.padding);
    this.add(this.text);
    this.validate();
  }
}
