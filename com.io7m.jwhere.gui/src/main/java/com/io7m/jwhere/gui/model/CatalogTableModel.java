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
import com.io7m.junreachable.UnreachableCodeException;

import javax.swing.table.AbstractTableModel;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A table model that maps the set of disks in a catalog to rows of a table.
 */

final class CatalogTableModel extends AbstractTableModel
{
  private final Supplier<CatalogState> state_get;

  CatalogTableModel(final Supplier<CatalogState> in_state_get)
  {
    this.state_get = Objects.requireNonNull(in_state_get, "in_state_get");
  }

  /**
   * Check that an object of the correct type is being returned.
   *
   * @param c   The object
   * @param col The field column index
   *
   * @return {@code c}
   */

  private static Object check(
    final Object c,
    final int col)
  {
    final var type = CatalogTableModelField.values()[col].getType();
    Preconditions.checkPreconditionV(
      type.isInstance(c), "%s must be an instance of %s", c.getClass(), type);
    return c;
  }

  @Override
  public int getRowCount()
  {
    return this.state_get.get().getCatalogDiskCount();
  }

  @Override
  public int getColumnCount()
  {
    return CatalogTableModelField.values().length;
  }

  @Override
  public String getColumnName(final int col)
  {
    Preconditions.checkPreconditionV(col >= 0, "col >= 0");
    Preconditions.checkPreconditionV(
      col < CatalogTableModelField.values().length,
      "col < CatalogTableModelField.values().length");
    return CatalogTableModelField.values()[col].getName();
  }

  @Override
  public Class<?> getColumnClass(final int col)
  {
    Preconditions.checkPreconditionV(col >= 0, "col >= 0");
    Preconditions.checkPreconditionV(
      col < CatalogTableModelField.values().length,
      "col < CatalogTableModelField.values().length");
    return CatalogTableModelField.values()[col].getType();
  }

  @Override
  public Object getValueAt(
    final int row,
    final int col)
  {
    final var state = this.state_get.get();
    final var disks =
      state.getCatalog().getDisks();

    Preconditions.checkPreconditionV(row >= 0, "row >= 0");
    Preconditions.checkPreconditionV(row < disks.size(), "row < disks.size()");
    Preconditions.checkPreconditionV(col >= 0, "col >= 0");
    Preconditions.checkPreconditionV(
      col < CatalogTableModelField.values().length,
      "col < CatalogTableModelField.values().length");

    final var disk_index = state.getCatalogDiskAt(row);
    Preconditions.checkPreconditionV(
      disks.containsKey(disk_index),
      "disks.containsKey(disk_index)");
    final var disk = disks.get(disk_index);
    final var meta = disk.getMeta();

    switch (CatalogTableModelField.values()[col]) {
      case NAME:
        return check(meta, col);
      case ARCHIVE_NUMBER:
        return check(meta.getDiskID(), col);
      case FILESYSTEM:
        return check(meta.getFilesystemType(), col);
      case SIZE:
        return check(new SizeBytes(meta.getSize()), col);
    }

    throw new UnreachableCodeException();
  }
}
