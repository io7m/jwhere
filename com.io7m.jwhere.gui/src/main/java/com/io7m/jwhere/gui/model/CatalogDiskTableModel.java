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
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDirectoryEntry;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDirectoryNodeType;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskMetadata;
import com.io7m.jwhere.core.CatalogFileHash;
import com.io7m.jwhere.core.CatalogFileNodeType;
import com.io7m.jwhere.core.CatalogNodeMatcherType;
import com.io7m.jwhere.core.CatalogNodeType;
import org.jgrapht.graph.UnmodifiableGraph;

import javax.swing.table.AbstractTableModel;
import java.math.BigInteger;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Supplier;

/**
 * A table model that maps directories of a given disk to table rows and columns.
 */

final class CatalogDiskTableModel extends AbstractTableModel
{
  private final List<CatalogDirectoryEntry> current_entries;
  private final Supplier<CatalogState> state_supplier;
  private CatalogDisk current_disk;
  private CatalogDirectoryNodeType current_dir;

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
    Preconditions.checkPreconditionV(in.size() >= 0, "in.size() >= 0");
    Preconditions.checkPreconditionV(in.size() <= 1, "in.size() <= 1");

    final CatalogDirectoryEntry parent;
    if (in.isEmpty()) {
      Preconditions.checkPreconditionV(
        node instanceof CatalogDirectoryNode,
        "node instanceof CatalogDirectoryNode");
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
    Preconditions.checkPreconditionV(
      type.isInstance(c), "%s must be an instance of %s", c.getClass(), type);
    return c;
  }

  /**
   * Load the entries of the root directory of the given disk into the table model.
   *
   * @param disk The disk
   */

  public void openDiskAtRoot(final CatalogDisk disk)
  {
    Objects.requireNonNull(disk, "disk");
    this.openDiskAtDirectory(disk, disk.getFilesystemRoot());
  }

  /**
   * Load the entries of the directory {@code node} of the given disk into the table model.
   *
   * @param disk The disk
   * @param node The directory
   */

  public void openDiskAtDirectory(
    final CatalogDisk disk,
    final CatalogDirectoryNodeType node)
  {
    Objects.requireNonNull(disk, "disk");
    Objects.requireNonNull(node, "node");

    final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> graph =
      disk.getFilesystemGraph();
    Preconditions.checkPreconditionV(graph.containsVertex(node), "graph.containsVertex(node)");

    this.current_disk = disk;
    this.current_dir = node;

    CatalogDiskTableModel.makeEntries(graph, node, this.current_entries);
  }

  @Override
  public String getColumnName(final int col)
  {
    Preconditions.checkPreconditionV(col >= 0, "col >= 0");
    Preconditions.checkPreconditionV(
      col < CatalogDiskTableModelField.values().length,
      "col < CatalogDiskTableModelField.values().length");

    return CatalogDiskTableModelField.values()[col].getName();
  }

  @Override
  public int getRowCount()
  {
    return this.current_entries.size();
  }

  @Override
  public int getColumnCount()
  {
    return CatalogDiskTableModelField.values().length;
  }

  @Override
  public Object getValueAt(
    final int row,
    final int col)
  {
    Preconditions.checkPreconditionV(col >= 0, "col >= 0");
    Preconditions.checkPreconditionV(
      col < CatalogDiskTableModelField.values().length,
      "col < CatalogDiskTableModelField.values().length");
    Preconditions.checkPreconditionV(row >= 0, "row >= 0");
    Preconditions.checkPreconditionV(
      row < this.current_entries.size(),
      "row < this.current_entries.size()");

    final CatalogDisk disk = Objects.requireNonNull(this.current_disk, "this.current_disk");
    final CatalogDirectoryEntry entry = this.current_entries.get(row);
    final CatalogNodeType target = entry.getTarget();
    final CatalogDiskMetadata meta = disk.getMeta();

    switch (CatalogDiskTableModelField.values()[col]) {
      case NAME:
        return CatalogDiskTableModel.check(
          col, entry.getTarget().matchNode(new NameGetter(row, entry, meta, disk)));
      case SIZE:
        return CatalogDiskTableModel.check(col, target.matchNode(new SizeGetter()));
      case CREATION_TIME:
        return CatalogDiskTableModel.check(col, target.creationTime());
      case MODIFICATION_TIME:
        return CatalogDiskTableModel.check(col, target.modificationTime());
      case ACCESS_TIME:
        return CatalogDiskTableModel.check(col, target.accessTime());
      case OWNER:
        return CatalogDiskTableModel.check(col, target.owner());
      case GROUP:
        return CatalogDiskTableModel.check(col, target.group());
      case PERMISSIONS:
        return CatalogDiskTableModel.check(
          col, PosixFilePermissions.toString(target.permissions()));
      case HASH:
        return CatalogDiskTableModel.check(col, target.matchNode(new HashGetter()));
    }

    throw new UnreachableCodeException();
  }

  @Override
  public Class<?> getColumnClass(final int col)
  {
    Preconditions.checkPreconditionV(
      col < CatalogDiskTableModelField.values().length,
      "col < CatalogDiskTableModelField.values().length");
    Preconditions.checkPreconditionV(col >= 0, "col >= 0");

    return CatalogDiskTableModelField.values()[col].getType();
  }

  public void checkStillValid()
  {
    final CatalogState state = this.state_supplier.get();
    final Catalog c = state.getCatalog();
    final SortedMap<CatalogDiskID, CatalogDisk> disks = c.getDisks();

    if (this.current_disk != null) {
      final CatalogDirectoryNodeType dir =
        Objects.requireNonNull(this.current_dir, "this.current_dir");

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

  private static final class NameGetter
    implements CatalogNodeMatcherType<DirectoryEntryType, UnreachableCodeException>
  {
    private final int row;
    private final CatalogDirectoryEntry entry;
    private final CatalogDiskMetadata meta;
    private final CatalogDisk disk;

    NameGetter(
      final int in_row,
      final CatalogDirectoryEntry in_entry,
      final CatalogDiskMetadata in_meta,
      final CatalogDisk in_disk)
    {
      this.row = in_row;
      this.entry = in_entry;
      this.meta = in_meta;
      this.disk = in_disk;
    }

    @Override
    public DirectoryEntryType onFile(final CatalogFileNodeType f)
    {
      Preconditions.checkPreconditionV(this.row > 0, "this.row > 0");
      return new DirectoryEntryFile(this.entry.getName());
    }

    @Override
    public DirectoryEntryType onDirectory(
      final CatalogDirectoryNodeType d)
    {
      if (this.row == 0) {
        return new DirectoryEntryUp(
          this.meta.getDiskID(),
          this.entry.getName(),
          this.entry.getSource(),
          d.equals(this.disk.getFilesystemRoot()));
      }
      return new DirectoryEntryDirectory(this.meta.getDiskID(), this.entry.getName(), d);
    }
  }

  private static final class SizeGetter
    implements CatalogNodeMatcherType<SizeBytes, UnreachableCodeException>
  {
    SizeGetter()
    {

    }

    @Override
    public SizeBytes onFile(
      final CatalogFileNodeType f)
    {
      return new SizeBytes(f.size());
    }

    @Override
    public SizeBytes onDirectory(
      final CatalogDirectoryNodeType d)
    {
      return new SizeBytes(BigInteger.ZERO);
    }
  }

  private static final class HashGetter
    implements CatalogNodeMatcherType<String, UnreachableCodeException>
  {
    HashGetter()
    {

    }

    @Override
    public String onFile(
      final CatalogFileNodeType f)
    {
      final Optional<CatalogFileHash> h_opt = f.hash();
      if (h_opt.isPresent()) {
        return h_opt.get().toString();
      } else {
        return "";
      }
    }

    @Override
    public String onDirectory(
      final CatalogDirectoryNodeType d)
    {
      return "";
    }
  }
}
