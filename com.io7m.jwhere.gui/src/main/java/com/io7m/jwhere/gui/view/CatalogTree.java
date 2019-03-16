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
import com.io7m.jwhere.gui.model.CatalogRootType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Component;

final class CatalogTree extends JTree
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CatalogTree.class);
  }

  CatalogTree(final TreeModel in_model)
  {
    super(in_model);
    this.setCellRenderer(new CatalogTreeCellRenderer());
    this.getSelectionModel()
      .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

  }

  private static final class CatalogTreeCellRenderer
    extends DefaultTreeCellRenderer
  {
    CatalogTreeCellRenderer()
    {

    }

    @Override
    public Component getTreeCellRendererComponent(
      final JTree tree,
      final Object value,
      final boolean sel,
      final boolean expanded,
      final boolean leaf,
      final int row,
      final boolean has_focus)
    {
      super.getTreeCellRendererComponent(
        tree, value, sel, expanded, leaf, row, has_focus);

      Preconditions.checkPreconditionV(
        value instanceof DefaultMutableTreeNode,
        "value instanceof DefaultMutableTreeNode");
      final var node = (DefaultMutableTreeNode) value;
      final var node_value = node.getUserObject();

      LOG.debug("selected: {}", node_value.getClass());

      if (node_value instanceof CatalogRootType) {
        this.setIcon(Icons.getCatalogIcon16());
        this.setText("Catalog");
      }

      if (node_value instanceof CatalogDiskMetadata) {
        final var disk_meta = (CatalogDiskMetadata) node_value;
        this.setIcon(CatalogDiskIcons.getIconForDisk(disk_meta));
        this.setText(disk_meta.getDiskName().value());
      }

      return this;
    }
  }
}
