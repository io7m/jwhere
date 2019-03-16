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

import java.util.Objects;

import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskMetadata;
import com.io7m.jwhere.gui.model.DirectoryEntryDirectory;
import com.io7m.jwhere.gui.model.DirectoryEntryFile;
import com.io7m.jwhere.gui.model.DirectoryEntryType;
import com.io7m.jwhere.gui.model.DirectoryEntryUp;
import com.io7m.jwhere.gui.model.SizeBytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

final class CatalogTable extends JTable
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CatalogTable.class);
  }

  CatalogTable(final TableModel in_model)
  {
    super(in_model);
    Objects.requireNonNull(in_model, "in_model");

    this.getTableHeader().setReorderingAllowed(false);

    final DirectoryEntryRenderer er = new DirectoryEntryRenderer();
    this.setDefaultRenderer(DirectoryEntryDirectory.class, er);
    this.setDefaultRenderer(DirectoryEntryFile.class, er);
    this.setDefaultRenderer(DirectoryEntryUp.class, er);
    this.setDefaultRenderer(DirectoryEntryType.class, er);

    this.setDefaultRenderer(
      CatalogDiskMetadata.class, new CatalogDiskMetadataRenderer());
    this.setDefaultRenderer(
      CatalogDiskID.class, new CatalogDiskIDRenderer());
    this.setDefaultRenderer(SizeBytes.class, new SizeBytesRenderer());
    this.setFont(Fonts.getMonospacedSmall());

    this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.setRowSelectionAllowed(true);
    this.setColumnSelectionAllowed(false);
  }
}
