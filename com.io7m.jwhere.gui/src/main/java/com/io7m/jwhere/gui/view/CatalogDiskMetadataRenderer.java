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
import com.io7m.jwhere.core.CatalogDiskMetadata;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

final class CatalogDiskMetadataRenderer extends DefaultTableCellRenderer
{
  CatalogDiskMetadataRenderer()
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
      value instanceof CatalogDiskMetadata,
      "value instanceof CatalogDiskMetadata");
    final CatalogDiskMetadata meta = (CatalogDiskMetadata) value;

    this.setIcon(CatalogDiskIcons.getIconForDisk(meta));
    this.setText(meta.getDiskName().value());
    return this;
  }
}
