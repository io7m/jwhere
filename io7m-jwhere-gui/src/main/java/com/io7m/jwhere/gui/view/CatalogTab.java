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

import com.io7m.jnull.NullCheck;
import com.io7m.jwhere.core.CatalogDiskMetadata;
import com.io7m.jwhere.gui.ControllerType;
import com.io7m.jwhere.gui.model.CatalogRootType;
import com.io7m.jwhere.gui.model.DirectoryEntryDirectory;
import com.io7m.jwhere.gui.model.DirectoryEntryUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class CatalogTab extends JPanel
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CatalogTab.class);
  }

  private final CatalogTree    catalog_disk_list;
  private final CatalogTable   catalog_table;
  private final ControllerType controller;

  CatalogTab(final ControllerType in_controller)
  {
    super();
    this.controller = NullCheck.notNull(in_controller);

    this.catalog_table = new CatalogTable(in_controller.catalogGetTableModel());
    this.catalog_table.addMouseListener(
      new MouseAdapter()
      {
        @Override public void mouseClicked(final MouseEvent e)
        {
          if (e.getClickCount() == 2) {
            final JTable target = (JTable) e.getSource();
            final int row = target.getSelectedRow();
            if (row >= 0) {
              final Object val = target.getValueAt(row, 0);
              CatalogTab.LOG.debug("selected: {} ({})", val, val.getClass());

              if (val instanceof DirectoryEntryUp) {
                final DirectoryEntryUp up = (DirectoryEntryUp) val;
                in_controller.catalogSelectDiskAtDirectory(
                  up.getDiskIndex(), up.getNode());
              }
              if (val instanceof DirectoryEntryDirectory) {
                final DirectoryEntryDirectory dir =
                  (DirectoryEntryDirectory) val;
                in_controller.catalogSelectDiskAtDirectory(
                  dir.getDiskIndex(), dir.getNode());
              }
            }
          }
        }
      });

    final JScrollPane table_scroller = new JScrollPane();
    table_scroller.setViewportView(this.catalog_table);

    this.catalog_disk_list =
      new CatalogTree(in_controller.catalogGetTreeModel());
    this.catalog_disk_list.addTreeSelectionListener(
      e -> {
        final Object selected =
          this.catalog_disk_list.getLastSelectedPathComponent();

        if (selected == null) {
          return;
        }

        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) selected;
        final Object node_value = node.getUserObject();

        CatalogTab.LOG.debug("selected: {}", node_value.getClass());

        if (node_value instanceof CatalogDiskMetadata) {
          final CatalogDiskMetadata meta = (CatalogDiskMetadata) node_value;
          this.controller.catalogSelectDiskAtRoot(meta.getDiskID());
        }

        if (node_value instanceof CatalogRootType) {
          this.controller.catalogSelectRoot();
        }
      });

    final JScrollPane list_scroller = new JScrollPane();
    list_scroller.setViewportView(this.catalog_disk_list);

    final JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitter.add(list_scroller);
    splitter.add(table_scroller);
    splitter.setContinuousLayout(false);

    this.setLayout(new BorderLayout());
    this.add(splitter, BorderLayout.CENTER);
  }
}
