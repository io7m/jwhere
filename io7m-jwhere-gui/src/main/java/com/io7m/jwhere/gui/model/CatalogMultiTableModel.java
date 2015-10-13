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

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.util.SortedMap;
import java.util.function.Supplier;

/**
 * A table model that delegates to a {@link CatalogTableModel} or a {@link
 * CatalogDiskTableModel} based on whatever the user selected most recently.
 */

final class CatalogMultiTableModel extends AbstractTableModel
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CatalogMultiTableModel.class);
  }



  private final CatalogTableModel      catalog_model;
  private final CatalogDiskTableModel  disk_model;
  private final Supplier<CatalogState> state_get;
  private       CatalogTarget          target;

  CatalogMultiTableModel(
    final Supplier<CatalogState> in_state_get,
    final CatalogTableModel in_catalog_model,
    final CatalogDiskTableModel in_disk_model)
  {
    this.state_get = NullCheck.notNull(in_state_get);
    this.catalog_model = NullCheck.notNull(in_catalog_model);
    this.disk_model = NullCheck.notNull(in_disk_model);
    this.target = CatalogTarget.CATALOG;
  }

  @Override public int getRowCount()
  {
    switch (this.target) {
      case CATALOG:
        return this.catalog_model.getRowCount();
      case DISK:
        return this.disk_model.getRowCount();
    }

    throw new UnreachableCodeException();
  }

  @Override public int getColumnCount()
  {
    switch (this.target) {
      case CATALOG:
        return this.catalog_model.getColumnCount();
      case DISK:
        return this.disk_model.getColumnCount();
    }

    throw new UnreachableCodeException();
  }

  @Override public Object getValueAt(
    final int row,
    final int col)
  {
    switch (this.target) {
      case CATALOG:
        return this.catalog_model.getValueAt(row, col);
      case DISK:
        return this.disk_model.getValueAt(row, col);
    }

    throw new UnreachableCodeException();
  }

  public void openDiskAtDirectory(
    final CatalogDiskID index,
    final CatalogDirectoryNode dir)
  {
    NullCheck.notNull(index);
    NullCheck.notNull(dir);

    final CatalogState state = this.state_get.get();
    final SortedMap<CatalogDiskID, CatalogDisk> disks =
      state.getCatalog().getDisks();
    final CatalogDisk disk = disks.get(index);
    this.disk_model.openDiskAtDirectory(disk, dir);

    this.updateTarget(CatalogTarget.DISK);
    this.fireTableDataChanged();
  }

  public void openDiskAtRoot(final CatalogDiskID index)
  {
    NullCheck.notNull(index);

    final CatalogState state = this.state_get.get();
    final SortedMap<CatalogDiskID, CatalogDisk> disks =
      state.getCatalog().getDisks();
    final CatalogDisk disk = disks.get(index);
    this.disk_model.openDiskAtRoot(disk);

    this.updateTarget(CatalogTarget.DISK);
    this.fireTableDataChanged();
  }

  @Override public Class<?> getColumnClass(final int index)
  {
    switch (this.target) {
      case CATALOG:
        return this.catalog_model.getColumnClass(index);
      case DISK:
        return this.disk_model.getColumnClass(index);
    }

    throw new UnreachableCodeException();
  }

  @Override public String getColumnName(final int column)
  {
    switch (this.target) {
      case CATALOG:
        return this.catalog_model.getColumnName(column);
      case DISK:
        return this.disk_model.getColumnName(column);
    }

    throw new UnreachableCodeException();
  }

  private void updateTarget(final CatalogTarget t)
  {
    final CatalogTarget old_target = this.target;
    this.target = t;
    if (t != old_target) {
      this.fireTableStructureChanged();
    }
  }

  @Override public void fireTableDataChanged()
  {
    this.disk_model.checkStillValid();
    super.fireTableDataChanged();
  }

  public void reset()
  {
    this.updateTarget(CatalogTarget.CATALOG);
    this.fireTableDataChanged();
  }

  private enum CatalogTarget
  {
    CATALOG,
    DISK
  }
}
