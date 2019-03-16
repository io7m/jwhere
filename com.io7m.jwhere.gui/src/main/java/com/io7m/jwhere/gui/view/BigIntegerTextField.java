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

import javax.swing.JTextField;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

final class BigIntegerTextField extends JTextField
{
  private static final Color ERROR_COLOR;

  static {
    ERROR_COLOR = Color.PINK;
  }

  private Color set_color;

  BigIntegerTextField()
  {
    super();
    this.set_color = this.getBackground();

    this.addKeyListener(
      new KeyAdapter()
      {
        @Override
        public void keyReleased(final KeyEvent e)
        {
          BigIntegerTextField.this.getBigInteger();
        }
      });
  }

  @Override
  public void setBackground(final Color bg)
  {
    this.set_color = Objects.requireNonNull(bg, "bg");
    super.setBackground(bg);
  }

  public Optional<BigInteger> getBigInteger()
  {
    try {
      final var r =
        Optional.of(new BigInteger(this.getText()));
      super.setBackground(this.set_color);
      return r;
    } catch (final NumberFormatException e) {
      super.setBackground(ERROR_COLOR);
      return Optional.empty();
    }
  }
}
