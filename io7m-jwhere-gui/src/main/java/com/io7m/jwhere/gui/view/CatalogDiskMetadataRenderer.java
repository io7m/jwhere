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

import com.io7m.jwhere.core.CatalogDiskMetadata;
import org.valid4j.Assertive;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

final class CatalogDiskMetadataRenderer extends DefaultTableCellRenderer
{
  CatalogDiskMetadataRenderer()
  {
    super();
  }

  @Override public Component getTableCellRendererComponent(
    final JTable table,
    final Object value,
    final boolean is_selected,
    final boolean has_focus,
    final int row,
    final int column)
  {
    super.getTableCellRendererComponent(
      table, value, is_selected, has_focus, row, column);

    Assertive.require(value instanceof CatalogDiskMetadata);
    final CatalogDiskMetadata meta = (CatalogDiskMetadata) value;

    if ("iso9660".equals(meta.getFilesystemType())) {
      this.setIcon(Icons.getDiskOpticalIcon16());
    } else {
      this.setIcon(Icons.getDiskIcon16());
    }

    this.setText(meta.getDiskName().getValue());
    return this;
  }
}