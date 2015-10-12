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
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwhere.core.CatalogDiskMetadata;
import com.io7m.jwhere.gui.ControllerType;
import com.io7m.jwhere.gui.model.CatalogRootType;
import com.io7m.jwhere.gui.model.DirectoryEntryDirectory;
import com.io7m.jwhere.gui.model.DirectoryEntryFile;
import com.io7m.jwhere.gui.model.DirectoryEntryUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
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

  CatalogTab(
    final JFrame parent,
    final StatusBar status,
    final ControllerType in_controller)
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

              if (val instanceof CatalogDiskMetadata) {
                final CatalogDiskMetadata meta = (CatalogDiskMetadata) val;
                in_controller.catalogSelectDiskAtRoot(meta.getDiskID());
                return;
              }

              if (val instanceof DirectoryEntryUp) {
                handleDirectoryEntryUp((DirectoryEntryUp) val);
                return;
              }

              if (val instanceof DirectoryEntryDirectory) {
                final DirectoryEntryDirectory dir =
                  (DirectoryEntryDirectory) val;
                in_controller.catalogSelectDiskAtDirectory(
                  dir.getDiskIndex(), dir.getNode());
                return;
              }

              if (val instanceof DirectoryEntryFile) {
                // Ignore for now.
                return;
              }

              throw new UnreachableCodeException();
            }
          }
        }

        private void handleDirectoryEntryUp(final DirectoryEntryUp up)
        {
          if (up.isRoot()) {
            in_controller.catalogSelectRoot();
          } else {
            in_controller.catalogSelectDiskAtDirectory(
              up.getDiskIndex(), up.getNode());
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
          return;
        }

        if (node_value instanceof CatalogRootType) {
          this.controller.catalogSelectRoot();
          return;
        }

        throw new UnreachableCodeException();
      });

    this.catalog_disk_list.addMouseListener(
      new CatalogTreePopupListener(
        parent, this.controller, status, this.catalog_disk_list));

    final JScrollPane list_scroller = new JScrollPane();
    list_scroller.setViewportView(this.catalog_disk_list);

    final JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitter.add(list_scroller);
    splitter.add(table_scroller);
    splitter.setContinuousLayout(false);

    this.setLayout(new BorderLayout());
    this.add(splitter, BorderLayout.CENTER);
  }

  static final class CatalogTreePopupListener extends MouseAdapter
  {
    private final CatalogTree    tree;
    private final JPopupMenu     disk_popup;
    private final JPopupMenu     catalog_popup;
    private final ControllerType controller;

    CatalogTreePopupListener(
      final JFrame parent,
      final ControllerType in_controller,
      final StatusBar in_status,
      final CatalogTree in_tree)
    {
      this.controller = NullCheck.notNull(in_controller);
      this.tree = NullCheck.notNull(in_tree);

      this.disk_popup = new JPopupMenu();
      this.catalog_popup = new JPopupMenu();

      {
        final JMenuItem add_disk = new JMenuItem("Add disk...");
        add_disk.addActionListener(
          (e) -> {
            final CatalogAddDiskDialog d =
              new CatalogAddDiskDialog(parent, in_controller, in_status);
            d.pack();
            d.setVisible(true);
          });
        this.catalog_popup.add(add_disk);
      }

      {
        final JMenuItem verify_disk = new JMenuItem("Verify disk...");
        this.disk_popup.add(verify_disk);
      }
    }

    @Override public void mousePressed(final MouseEvent e)
    {
      this.maybeShowPopup(e);
    }

    @Override public void mouseReleased(final MouseEvent e)
    {
      this.maybeShowPopup(e);
    }

    private void maybeShowPopup(final MouseEvent e)
    {
      if (e.isPopupTrigger()) {
        final int cx = e.getX();
        final int cy = e.getY();

        final TreePath p = this.tree.getPathForLocation(cx, cy);
        if (p == null) {
          return;
        }

        final DefaultMutableTreeNode last =
          (DefaultMutableTreeNode) p.getLastPathComponent();
        final Object node_value = last.getUserObject();

        CatalogTab.LOG.debug("popup selected: {}", node_value.getClass());

        if (node_value instanceof CatalogDiskMetadata) {
          this.disk_popup.show(e.getComponent(), e.getX(), e.getY());
        }

        if (node_value instanceof CatalogRootType) {
          this.catalog_popup.show(e.getComponent(), e.getX(), e.getY());
        }
      }
    }
  }
}
