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

import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskMetadata;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Set;
import java.util.SortedMap;

/**
 * A tree model that maps a given catalog to a tree.
 */

final class CatalogTreeModel extends DefaultTreeModel
{
  CatalogTreeModel()
  {
    super(
      new DefaultMutableTreeNode((CatalogRootType) () -> "Catalog"));
  }

  public void update(final Catalog in_catalog)
  {
    final DefaultMutableTreeNode new_root = new DefaultMutableTreeNode(
      (CatalogRootType) () -> "Catalog");

    final SortedMap<CatalogDiskID, CatalogDisk> disks = in_catalog.getDisks();
    final Set<CatalogDiskID> indices = disks.keySet();
    for (final CatalogDiskID index : indices) {
      final CatalogDisk disk = disks.get(index);
      final CatalogDiskMetadata meta = disk.getMeta();
      new_root.add(new DefaultMutableTreeNode(meta));
    }
    this.setRoot(new_root);
  }
}
