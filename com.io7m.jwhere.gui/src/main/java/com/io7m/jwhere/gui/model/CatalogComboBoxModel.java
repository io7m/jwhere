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

package com.io7m.jwhere.gui.model;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskMetadata;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import java.util.Objects;
import java.util.SortedMap;
import java.util.function.Supplier;

final class CatalogComboBoxModel extends AbstractListModel<CatalogDiskMetadata>
  implements ComboBoxModel<CatalogDiskMetadata>
{
  private final Supplier<CatalogState> supplier;
  private Object selection;
  private int size_last;

  CatalogComboBoxModel(final Supplier<CatalogState> in_supplier)
  {
    this.supplier = Objects.requireNonNull(in_supplier, "in_supplier");
  }

  @Override
  public int getSize()
  {
    return this.supplier.get().getCatalogDiskCount();
  }

  @Override
  public CatalogDiskMetadata getElementAt(final int index)
  {
    final CatalogState state = this.supplier.get();

    Preconditions.checkPreconditionV(index >= 0, "index >= 0");
    Preconditions.checkPreconditionV(
      index < state.getCatalogDiskCount(),
      "index < state.getCatalogDiskCount()");

    final CatalogDiskID disk_id = state.getCatalogDiskAt(index);
    final Catalog c = state.getCatalog();
    final SortedMap<CatalogDiskID, CatalogDisk> disks = c.getDisks();
    return disks.get(disk_id).getMeta();
  }

  public void update()
  {
    final int old_size = this.size_last;
    this.fireIntervalRemoved(this, 0, old_size);
    this.selection = null;

    final int new_size = this.getSize();
    if (new_size > 0) {
      this.fireIntervalAdded(this, 0, new_size - 1);
    }

    this.fireContentsChanged(this, 0, new_size - 1);
  }

  @Override
  public Object getSelectedItem()
  {
    return this.selection;
  }

  @Override
  public void setSelectedItem(final Object x)
  {
    this.selection = x;
  }
}
