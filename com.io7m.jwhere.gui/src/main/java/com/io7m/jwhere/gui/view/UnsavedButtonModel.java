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

import com.io7m.jwhere.gui.ControllerType;
import com.io7m.jwhere.gui.model.UnsavedChanges;

import javax.swing.DefaultButtonModel;
import java.util.function.Consumer;

/**
 * A button model that enables or disables the button based on the current saved or unsaved status
 * of the catalog.
 */

public final class UnsavedButtonModel extends DefaultButtonModel
  implements Consumer<UnsavedChanges>
{
  /**
   * Construct a button model.
   *
   * @param c The controller
   */

  public UnsavedButtonModel(final ControllerType c)
  {
    c.catalogUnsavedChangesSubscribe(this);
  }

  @Override
  public void accept(final UnsavedChanges c)
  {
    this.setEnabled(c == UnsavedChanges.UNSAVED_CHANGES);
  }
}
