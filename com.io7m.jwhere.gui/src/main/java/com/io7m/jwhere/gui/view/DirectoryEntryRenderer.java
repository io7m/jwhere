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

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jfunctional.Unit;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwhere.gui.model.DirectoryEntryDirectory;
import com.io7m.jwhere.gui.model.DirectoryEntryFile;
import com.io7m.jwhere.gui.model.DirectoryEntryMatcherType;
import com.io7m.jwhere.gui.model.DirectoryEntryType;
import com.io7m.jwhere.gui.model.DirectoryEntryUp;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

final class DirectoryEntryRenderer extends DefaultTableCellRenderer
{
  DirectoryEntryRenderer()
  {
    super();
  }

  @Override
  public Component getTableCellRendererComponent(
    final JTable table,
    final Object value,
    final boolean is_selected,
    final boolean has_focus,
    final int row,
    final int column)
  {
    super.getTableCellRendererComponent(
      table, value, is_selected, has_focus, row, column);

    Preconditions.checkPreconditionV(
      value instanceof DirectoryEntryType,
      "value instanceof DirectoryEntryType");
    final var entry = (DirectoryEntryType) value;

    this.setText(entry.getName());
    entry.matchEntry(new IconSetter());

    return this;
  }

  private final class IconSetter
    implements DirectoryEntryMatcherType<Unit, UnreachableCodeException>
  {
    IconSetter()
    {

    }

    @Override
    public Unit onFile(final DirectoryEntryFile n)
    {
      DirectoryEntryRenderer.this.setIcon(Icons.getFile16());
      return Unit.unit();
    }

    @Override
    public Unit onDirectory(final DirectoryEntryDirectory n)
    {
      DirectoryEntryRenderer.this.setIcon(Icons.getFolder16());
      return Unit.unit();
    }

    @Override
    public Unit onUp(final DirectoryEntryUp n)
    {
      DirectoryEntryRenderer.this.setIcon(Icons.getFolderUp16());
      return Unit.unit();
    }
  }
}
