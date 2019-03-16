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
import com.io7m.jwhere.core.CatalogVerificationReportItemType;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

final class CatalogVerificationTableModel extends AbstractTableModel
{
  private final List<CatalogVerificationReportItemType> data;

  CatalogVerificationTableModel()
  {
    this.data = new ArrayList<>(256);
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
    final int col,
    final Object c)
  {
    final Class<?> type =
      CatalogVerificationTableModelField.values()[col].getType();
    Preconditions.checkPreconditionV(
      type.isInstance(c), "%s must be an instance of %s", c.getClass(), type);
    return c;
  }

  void reset()
  {
    this.data.clear();
    this.fireTableDataChanged();
  }

  void add(final CatalogVerificationReportItemType i)
  {
    this.data.add(i);
    this.fireTableDataChanged();
  }

  @Override
  public int getRowCount()
  {
    return this.data.size();
  }

  @Override
  public int getColumnCount()
  {
    return CatalogVerificationTableModelField.values().length;
  }

  @Override
  public Class<?> getColumnClass(final int col)
  {
    Preconditions.checkPreconditionV(col >= 0, "col >= 0");
    Preconditions.checkPreconditionV(
      col < CatalogVerificationTableModelField.values().length,
      "col < CatalogVerificationTableModelField.values().length");
    return CatalogVerificationTableModelField.values()[col].getType();
  }

  @Override
  public String getColumnName(final int col)
  {
    Preconditions.checkPreconditionV(col >= 0, "col >= 0");
    Preconditions.checkPreconditionV(
      col < CatalogVerificationTableModelField.values().length,
      "col < CatalogVerificationTableModelField.values().length");
    return CatalogVerificationTableModelField.values()[col].getName();
  }

  @Override
  public Object getValueAt(
    final int row,
    final int col)
  {
    Preconditions.checkPreconditionV(row >= 0, "row >= 0");
    Preconditions.checkPreconditionV(row < this.data.size(), "row < this.data.size()");
    Preconditions.checkPreconditionV(col >= 0, "col >= 0");
    Preconditions.checkPreconditionV(
      col < CatalogVerificationTableModelField.values().length,
      "col < CatalogVerificationTableModelField.values().length");

    switch (CatalogVerificationTableModelField.values()[col]) {
      case NAME:
        return CatalogVerificationTableModel.check(
          col, this.data.get(row).path());
      case RESULT:
        return CatalogVerificationTableModel.check(
          col, this.data.get(row).show());
    }

    throw new UnreachableCodeException();
  }
}
