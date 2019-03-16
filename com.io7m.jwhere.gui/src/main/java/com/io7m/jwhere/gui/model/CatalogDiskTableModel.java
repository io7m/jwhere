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

import java.util.Objects;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDirectoryEntry;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskMetadata;
import com.io7m.jwhere.core.CatalogFileHash;
import com.io7m.jwhere.core.CatalogFileNode;
import com.io7m.jwhere.core.CatalogNodeMatcherType;
import com.io7m.jwhere.core.CatalogNodeType;
import org.jgrapht.graph.UnmodifiableGraph;
import org.valid4j.Assertive;

import javax.swing.table.AbstractTableModel;
import java.math.BigInteger;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Supplier;

/**
 * A table model that maps directories of a given disk to table rows and
 * columns.
 */

final class CatalogDiskTableModel extends AbstractTableModel
{
  private final     List<CatalogDirectoryEntry> current_entries;
  private final     Supplier<CatalogState>      state_supplier;
  private @Nullable CatalogDisk                 current_disk;
  private @Nullable CatalogDirectoryNode        current_dir;

  CatalogDiskTableModel(final Supplier<CatalogState> supplier)
  {
    this.current_entries = new ArrayList<>(128);
    this.state_supplier = Objects.requireNonNull(supplier, "supplier");
  }

  private static void makeEntries(
    final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> graph,
    final CatalogNodeType node,
    final List<CatalogDirectoryEntry> entries)
  {
    /**
     * Entries are inserted into a sorted array for accessing by "row".
     */

    entries.clear();
    final Set<CatalogDirectoryEntry> out = graph.outgoingEdgesOf(node);
    out.stream().forEach(entries::add);
    entries.sort((ea, eb) -> ea.getName().compareTo(eb.getName()));

    /**
     * An entry representing ".." is synthesized.
     *
     * If the directory is the root node, there won't be any incoming
     * edges and therefore the entry should point to itself. The UI
     * is responsible for doing something sensible with this.
     */

    final Set<CatalogDirectoryEntry> in = graph.incomingEdgesOf(node);
    Assertive.require(in.size() >= 0);
    Assertive.require(in.size() <= 1);

    final CatalogDirectoryEntry parent;
    if (in.isEmpty()) {
      Assertive.require(node instanceof CatalogDirectoryNode);
      parent =
        new CatalogDirectoryEntry((CatalogDirectoryNode) node, node, "..");
    } else {
      parent = in.iterator().next();
    }

    entries.add(
      0, new CatalogDirectoryEntry(
        parent.getSource(), parent.getTarget(), ".."));
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
    final Class<?> type = CatalogDiskTableModelField.values()[col].getType();
    Assertive.require(
      type.isInstance(c), "%s must be an instance of %s", c.getClass(), type);
    return c;
  }

  /**
   * Load the entries of the root directory of the given disk into the table
   * model.
   *
   * @param disk The disk
   */

  public void openDiskAtRoot(final CatalogDisk disk)
  {
    Objects.requireNonNull(disk, "disk");
    this.openDiskAtDirectory(disk, disk.getFilesystemRoot());
  }

  /**
   * Load the entries of the directory {@code node} of the given disk into the
   * table model.
   *
   * @param disk The disk
   * @param node The directory
   */

  public void openDiskAtDirectory(
    final CatalogDisk disk,
    final CatalogDirectoryNode node)
  {
    Objects.requireNonNull(disk, "disk");
    Objects.requireNonNull(node, "node");

    final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> graph =
      disk.getFilesystemGraph();
    Assertive.require(graph.containsVertex(node));

    this.current_disk = disk;
    this.current_dir = node;

    CatalogDiskTableModel.makeEntries(graph, node, this.current_entries);
  }

  @Override public String getColumnName(final int col)
  {
    Assertive.require(col >= 0);
    Assertive.require(col < CatalogDiskTableModelField.values().length);

    return CatalogDiskTableModelField.values()[col].getName();
  }

  @Override public int getRowCount()
  {
    return this.current_entries.size();
  }

  @Override public int getColumnCount()
  {
    return CatalogDiskTableModelField.values().length;
  }

  @Override public Object getValueAt(
    final int row,
    final int col)
  {
    Assertive.require(col >= 0);
    Assertive.require(col < CatalogDiskTableModelField.values().length);
    Assertive.require(row >= 0);
    Assertive.require(row < this.current_entries.size());

    final CatalogDisk disk = Objects.requireNonNull(this.current_disk, "this.current_disk");
    final CatalogDirectoryEntry entry = this.current_entries.get(row);
    final CatalogNodeType target = entry.getTarget();
    final CatalogDiskMetadata meta = disk.getMeta();

    switch (CatalogDiskTableModelField.values()[col]) {
      case NAME:
        return CatalogDiskTableModel.check(
          col, entry.getTarget().matchNode(
            new CatalogNodeMatcherType<DirectoryEntryType,
              UnreachableCodeException>()
            {
              @Override
              public DirectoryEntryType onFile(final CatalogFileNode f)
              {
                Assertive.require(row > 0);
                return new DirectoryEntryFile(entry.getName());
              }

              @Override public DirectoryEntryType onDirectory(
                final CatalogDirectoryNode d)
              {
                if (row == 0) {
                  return new DirectoryEntryUp(
                    meta.getDiskID(),
                    entry.getName(),
                    entry.getSource(),
                    d.equals(disk.getFilesystemRoot()));
                }
                return new DirectoryEntryDirectory(
                  meta.getDiskID(), entry.getName(), d);
              }
            }));
      case SIZE:
        return CatalogDiskTableModel.check(
          col, target.matchNode(
            new CatalogNodeMatcherType<SizeBytes, UnreachableCodeException>()
            {
              @Override public SizeBytes onFile(
                final CatalogFileNode f)
              {
                return new SizeBytes(f.getSize());
              }

              @Override public SizeBytes onDirectory(
                final CatalogDirectoryNode d)
              {
                return new SizeBytes(BigInteger.ZERO);
              }
            }));
      case CREATION_TIME:
        return CatalogDiskTableModel.check(col, target.getCreationTime());
      case MODIFICATION_TIME:
        return CatalogDiskTableModel.check(col, target.getModificationTime());
      case ACCESS_TIME:
        return CatalogDiskTableModel.check(col, target.getAccessTime());
      case OWNER:
        return CatalogDiskTableModel.check(col, target.getOwner());
      case GROUP:
        return CatalogDiskTableModel.check(col, target.getGroup());
      case PERMISSIONS:
        return CatalogDiskTableModel.check(
          col, PosixFilePermissions.toString(target.getPermissions()));
      case HASH:
        return CatalogDiskTableModel.check(
          col, target.matchNode(
            new CatalogNodeMatcherType<String, UnreachableCodeException>()
            {
              @Override public String onFile(
                final CatalogFileNode f)
              {
                final Optional<CatalogFileHash> h_opt = f.getHash();
                if (h_opt.isPresent()) {
                  return h_opt.get().toString();
                } else {
                  return "";
                }
              }

              @Override public String onDirectory(
                final CatalogDirectoryNode d)
              {
                return "";
              }
            }));
    }

    throw new UnreachableCodeException();
  }

  @Override public Class<?> getColumnClass(final int col)
  {
    Assertive.require(col < CatalogDiskTableModelField.values().length);
    Assertive.require(col >= 0);

    return CatalogDiskTableModelField.values()[col].getType();
  }

  public void checkStillValid()
  {
    final CatalogState state = this.state_supplier.get();
    final Catalog c = state.getCatalog();
    final SortedMap<CatalogDiskID, CatalogDisk> disks = c.getDisks();

    if (this.current_disk != null) {
      final CatalogDirectoryNode dir = Objects.requireNonNull(this.current_dir, "this.current_dir");

      final CatalogDiskMetadata meta = this.current_disk.getMeta();
      final CatalogDiskID disk_id = meta.getDiskID();
      if (!disks.containsKey(disk_id)) {
        this.reset();
        return;
      }

      final CatalogDisk disk = disks.get(disk_id);
      final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> root =
        disk.getFilesystemGraph();
      if (!root.containsVertex(dir)) {
        this.reset();
        return;
      }
    }
  }

  private void reset()
  {
    this.current_dir = null;
    this.current_disk = null;
    this.current_entries.clear();
  }
}
