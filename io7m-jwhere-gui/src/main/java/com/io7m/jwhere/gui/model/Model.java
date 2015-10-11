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
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDirectoryEntry;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskMetadata;
import com.io7m.jwhere.core.CatalogDiskName;
import com.io7m.jwhere.core.CatalogException;
import com.io7m.jwhere.core.CatalogFileHash;
import com.io7m.jwhere.core.CatalogFileNode;
import com.io7m.jwhere.core.CatalogJSONParser;
import com.io7m.jwhere.core.CatalogJSONParserType;
import com.io7m.jwhere.core.CatalogJSONSerializer;
import com.io7m.jwhere.core.CatalogJSONSerializerType;
import com.io7m.jwhere.core.CatalogNodeMatcherType;
import com.io7m.jwhere.core.CatalogNodeType;
import org.jgrapht.graph.UnmodifiableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valid4j.Assertive;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The GUI model.
 */

public final class Model
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(Model.class);
  }

  private final CatalogMultiTableModel catalog_table_model;
  private final CatalogTreeModel       catalog_tree_model;
  private       Optional<Path>         catalog_file;
  private       Catalog                catalog;
  private       boolean                unsaved;
  private       List<CatalogDiskID>    catalog_rows;

  /**
   * Construct the model.
   */

  public Model()
  {
    this.catalog_file = Optional.empty();
    this.catalog = new Catalog(new TreeMap<>());
    this.catalog_rows = new ArrayList<>();
    this.catalog_table_model = new CatalogMultiTableModel(
      new CatalogTableModel(),
      new CatalogDiskTableModel(),
      CatalogTarget.CATALOG);
    this.catalog_tree_model = new CatalogTreeModel();
  }

  private static List<CatalogDiskID> makeRows(final Catalog c)
  {
    final SortedMap<CatalogDiskID, CatalogDisk> disks = c.getDisks();
    final Set<CatalogDiskID> keys = disks.keySet();
    final Iterator<CatalogDiskID> iter = keys.iterator();
    final List<CatalogDiskID> rows = new ArrayList<>(keys.size());

    while (iter.hasNext()) {
      rows.add(iter.next());
    }
    return rows;
  }

  /**
   * @return The current catalog table model
   */

  public TableModel getCatalogTableModel()
  {
    return this.catalog_table_model;
  }

  /**
   * @return {@code true} if the current catalog has unsaved changes
   */

  public boolean isCatalogUnsaved()
  {
    return this.unsaved;
  }

  /**
   * @return The current catalog filename, if any
   */

  public Optional<Path> getCatalogFileName()
  {
    return this.catalog_file;
  }

  /**
   * Save the catalog to {@code path}.
   *
   * @param path The path to the catalog
   *
   * @throws IOException On I/O errors
   */

  public void catalogSave(final Path path)
    throws IOException
  {
    Model.LOG.debug("saving catalog to: {}", path);

    final CatalogJSONSerializerType serial =
      CatalogJSONSerializer.newSerializer();

    try (final OutputStream stream = Files.newOutputStream(path)) {
      serial.serializeCatalogToStream(this.catalog, stream);
      this.unsaved = false;
      this.catalog_file = Optional.of(path);
    }
  }

  /**
   * Open the catalog at {@code path}.
   *
   * @param path The path to the catalog
   *
   * @throws IOException      On I/O errors
   * @throws CatalogException On malformed catalogs
   */

  public void catalogOpen(final Path path)
    throws IOException, CatalogException
  {
    Model.LOG.debug("opening catalog from: {}", path);

    final CatalogJSONParserType parser = CatalogJSONParser.newParser();
    try (final InputStream stream = Files.newInputStream(path)) {
      this.catalog = parser.parseCatalogFromStream(stream);
      this.catalog_rows = Model.makeRows(this.catalog);
      this.unsaved = false;
      this.catalog_file = Optional.of(path);
      this.catalog_table_model.reset();
      this.catalog_tree_model.changeTree(this.catalog);
    }
  }

  /**
   * Close the current catalog.
   */

  public void catalogClose()
  {
    Model.LOG.debug("closing catalog");

    this.catalog = new Catalog(new TreeMap<>());
    this.catalog_rows = new ArrayList<>(32);
    this.unsaved = false;
    this.catalog_file = Optional.empty();
    this.catalog_table_model.reset();
    this.catalog_tree_model.changeTree(this.catalog);
  }

  /**
   * @return The current catalog's tree model.
   */

  public TreeModel getCatalogTreeModel()
  {
    return this.catalog_tree_model;
  }

  /**
   * Load the root of the disk {@code index} into the tabel model.
   *
   * @param index The disk ID
   */

  public void selectDiskAtRoot(final CatalogDiskID index)
  {
    NullCheck.notNull(index);
    Assertive.require(this.catalog.getDisks().containsKey(index));
    this.catalog_table_model.openDiskAtRoot(index);
  }

  /**
   * Load the directory {@code dir} of the disk {@code index} into the tabel
   * model.
   *
   * @param dir   The directory
   * @param index The disk ID
   */

  public void selectDiskAtDirectory(
    final CatalogDiskID index,
    final CatalogDirectoryNode dir)
  {
    NullCheck.notNull(index);
    NullCheck.notNull(dir);
    Assertive.require(this.catalog.getDisks().containsKey(index));
    this.catalog_table_model.openDiskAtDirectory(index, dir);
  }

  /**
   * Load the root of the catalog into the tabel model.
   */

  public void selectRoot()
  {
    this.catalog_table_model.reset();
  }

  private enum CatalogTableModelField
  {
    NAME(0, "Name", CatalogDiskName.class),
    ARCHIVE_NUMBER(1, "Disk ID", CatalogDiskID.class),
    FILESYSTEM(2, "Filesystem", String.class),
    SIZE(3, "Size", SizeBytes.class);

    private final int      position;
    private final String   name;
    private final Class<?> type;

    CatalogTableModelField(
      final int in_position,
      final String in_name,
      final Class<?> in_type)
    {
      this.position = in_position;
      this.name = in_name;
      this.type = in_type;
    }
  }

  private enum CatalogDiskTableModelField
  {
    NAME(0, "Name", DirectoryEntryType.class),
    SIZE(1, "Size", SizeBytes.class),
    CREATION_TIME(2, "Creation Time", Instant.class),
    MODIFICATION_TIME(3, "Modification Time", Instant.class),
    ACCESS_TIME(4, "Access Time", Instant.class),
    OWNER(5, "Owner", String.class),
    GROUP(6, "Group", String.class),
    PERMISSIONS(7, "Permissions", String.class),
    HASH(8, "Hash", String.class);

    private final String   name;
    private final int      position;
    private final Class<?> type;

    CatalogDiskTableModelField(
      final int in_position,
      final String in_name,
      final Class<?> in_type)
    {
      this.position = in_position;
      this.name = NullCheck.notNull(in_name);
      this.type = NullCheck.notNull(in_type);
    }
  }

  enum CatalogTarget
  {
    CATALOG,
    DISK
  }

  private static final class CatalogTreeModel extends DefaultTreeModel
  {
    CatalogTreeModel()
    {
      super(
        new DefaultMutableTreeNode((CatalogRootType) () -> "Catalog"));
    }

    public void changeTree(final Catalog in_catalog)
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

  private static final class CatalogDiskTableModel extends AbstractTableModel
  {
    private final     List<CatalogDirectoryEntry> current_entries;
    private @Nullable CatalogDisk                 current_disk;
    private @Nullable UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry>
                                                  current_graph;
    private @Nullable CatalogNodeType             current_node;

    CatalogDiskTableModel()
    {
      this.current_entries = new ArrayList<>(128);
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

    public void openDiskAtRoot(final CatalogDisk disk)
    {
      NullCheck.notNull(disk);
      this.openDiskAtDirectory(disk, disk.getFilesystemRoot());
    }

    public void openDiskAtDirectory(
      final CatalogDisk disk,
      final CatalogDirectoryNode node)
    {
      NullCheck.notNull(disk);
      NullCheck.notNull(node);

      final UnmodifiableGraph<CatalogNodeType, CatalogDirectoryEntry> graph =
        disk.getFilesystemGraph();
      Assertive.require(graph.containsVertex(node));

      this.current_disk = disk;
      this.current_node = node;
      this.current_graph = graph;

      CatalogDiskTableModel.makeEntries(graph, node, this.current_entries);
    }

    @Override public String getColumnName(final int col)
    {
      Assertive.require(col >= 0);
      Assertive.require(col < CatalogDiskTableModelField.values().length);

      return CatalogDiskTableModelField.values()[col].name;
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

      final CatalogDirectoryEntry entry = this.current_entries.get(row);
      final CatalogNodeType target = entry.getTarget();
      final CatalogDiskMetadata meta = this.current_disk.getMeta();

      switch (CatalogDiskTableModelField.values()[col]) {
        case NAME:
          return entry.getTarget().matchNode(
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
                    meta.getDiskID(), entry.getName(), entry.getSource());
                }
                return new DirectoryEntryDirectory(
                  meta.getDiskID(), entry.getName(), d);
              }
            });
        case SIZE:
          return target.matchNode(
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
            });
        case CREATION_TIME:
          return target.getCreationTime();
        case MODIFICATION_TIME:
          return target.getModificationTime();
        case ACCESS_TIME:
          return target.getAccessTime();
        case OWNER:
          return target.getOwner();
        case GROUP:
          return target.getGroup();
        case PERMISSIONS:
          return PosixFilePermissions.toString(target.getPermissions());
        case HASH:
          return target.matchNode(
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
            });
      }

      throw new UnreachableCodeException();
    }

    @Override public Class<?> getColumnClass(final int col)
    {
      Assertive.require(col < CatalogDiskTableModelField.values().length);
      Assertive.require(col >= 0);

      return CatalogDiskTableModelField.values()[col].type;
    }
  }

  private final class CatalogTableModel extends AbstractTableModel
  {
    CatalogTableModel()
    {

    }

    @Override public int getRowCount()
    {
      return Model.this.catalog_rows.size();
    }

    @Override public int getColumnCount()
    {
      return CatalogTableModelField.values().length;
    }

    @Override public String getColumnName(final int col)
    {
      Assertive.require(col >= 0);
      Assertive.require(col < CatalogTableModelField.values().length);
      return CatalogTableModelField.values()[col].name;
    }

    @Override public Class<?> getColumnClass(final int col)
    {
      Assertive.require(col >= 0);
      Assertive.require(col < CatalogTableModelField.values().length);
      return CatalogTableModelField.values()[col].type;
    }

    @Override public Object getValueAt(
      final int row,
      final int col)
    {
      final SortedMap<CatalogDiskID, CatalogDisk> disks =
        Model.this.catalog.getDisks();

      Assertive.require(row >= 0);
      Assertive.require(row < disks.size());
      Assertive.require(col >= 0);
      Assertive.require(col < CatalogTableModelField.values().length);

      final CatalogDiskID disk_index = Model.this.catalog_rows.get(row);
      Assertive.ensure(disks.containsKey(disk_index));
      final CatalogDisk disk = disks.get(disk_index);
      final CatalogDiskMetadata meta = disk.getMeta();

      switch (CatalogTableModelField.values()[col]) {
        case NAME:
          return meta.getDiskName();
        case ARCHIVE_NUMBER:
          return meta.getDiskID();
        case FILESYSTEM:
          return meta.getFilesystemType();
        case SIZE:
          return new SizeBytes(meta.getSize());
      }

      throw new UnreachableCodeException();
    }
  }

  private final class CatalogMultiTableModel extends AbstractTableModel
  {
    private @Nullable final CatalogTableModel     catalog_model;
    private @Nullable final CatalogDiskTableModel disk_model;
    private                 CatalogTarget         target;

    private CatalogMultiTableModel(
      final CatalogTableModel in_catalog_model,
      final CatalogDiskTableModel in_disk_model,
      final CatalogTarget in_target)
    {
      this.catalog_model = NullCheck.notNull(in_catalog_model);
      this.disk_model = NullCheck.notNull(in_disk_model);
      this.target = NullCheck.notNull(in_target);
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

      final SortedMap<CatalogDiskID, CatalogDisk> disks =
        Model.this.catalog.getDisks();
      final CatalogDisk disk = disks.get(index);
      this.disk_model.openDiskAtDirectory(disk, dir);

      this.updateTarget(CatalogTarget.DISK);
      this.fireTableDataChanged();
    }

    public void openDiskAtRoot(final CatalogDiskID index)
    {
      NullCheck.notNull(index);

      final SortedMap<CatalogDiskID, CatalogDisk> disks =
        Model.this.catalog.getDisks();
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

    public void reset()
    {
      this.updateTarget(CatalogTarget.CATALOG);
      this.fireTableDataChanged();
    }
  }
}
