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
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskDuplicateIndexException;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskName;
import com.io7m.jwhere.core.CatalogException;
import com.io7m.jwhere.core.CatalogFilesystemReader;
import com.io7m.jwhere.core.CatalogJSONParser;
import com.io7m.jwhere.core.CatalogJSONParserType;
import com.io7m.jwhere.core.CatalogJSONSerializer;
import com.io7m.jwhere.core.CatalogJSONSerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valid4j.Assertive;

import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * The GUI model.
 */

public final class Model
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(Model.class);
  }

  private final CatalogMultiTableModel
    catalog_table_model;
  private final CatalogTreeModel
                                                               catalog_tree_model;
  private final MutableBoundedNonEmptyDiscardStack<ModelState> state_history;
  private final ObservableValue<UnsavedChanges>                unsaved;
  private final ObservableValue<UndoAvailable>                 undo;
  private final ObservableValue<RedoAvailable>                 redo;
  private       BigInteger
                                                               state_revision_saved;
  private       Optional<Path>                                 catalog_file;

  /**
   * Construct the model.
   */

  public Model()
  {
    this.catalog_file = Optional.empty();

    this.state_revision_saved = BigInteger.ZERO;
    this.state_history = new MutableBoundedNonEmptyDiscardStack<>(
      ModelState.newEmpty(this.state_revision_saved), 32);

    final CatalogTableModel tm =
      new CatalogTableModel(this.state_history::peek);
    final CatalogDiskTableModel dtm = new CatalogDiskTableModel();
    this.catalog_table_model = new CatalogMultiTableModel(
      this.state_history::peek, tm, dtm);
    this.catalog_tree_model = new CatalogTreeModel();

    // Checkstyle is currently choking on these definitions, claiming
    // that an explicit 'this' is required.
    // CHECKSTYLE:OFF
    this.unsaved = new ObservableValue<>(this::isCatalogUnsaved);
    this.undo = new ObservableValue<>(this::catalogCanUndo);
    this.redo = new ObservableValue<>(this::catalogCanRedo);
    // CHECKSTYLE:ON
  }

  private UndoAvailable catalogCanUndo()
  {
    if (this.state_history.size() > 1) {
      return UndoAvailable.UNDO_AVAILABLE;
    } else {
      return UndoAvailable.UNDO_UNAVAILABLE;
    }
  }

  private RedoAvailable catalogCanRedo()
  {
    return RedoAvailable.REDO_UNAVAILABLE;
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

  public UnsavedChanges isCatalogUnsaved()
  {
    final BigInteger current_revision = this.state_history.peek().getRevision();
    final boolean saved = current_revision.equals(this.state_revision_saved);
    if (saved) {
      return UnsavedChanges.NO_UNSAVED_CHANGES;
    } else {
      return UnsavedChanges.UNSAVED_CHANGES;
    }
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

    final ModelState current = this.state_history.peek();
    try (final OutputStream stream = Files.newOutputStream(path)) {
      serial.serializeCatalogToStream(current.getCatalog(), stream);
      this.state_revision_saved = current.getRevision();
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
      final Catalog c = parser.parseCatalogFromStream(stream);

      final ModelState current = ModelState.newCatalog(c, BigInteger.ZERO);
      this.state_history.clear(current);
      this.state_revision_saved = BigInteger.ZERO;
      this.unsaved.broadcast();
      this.undo.broadcast();

      this.catalog_file = Optional.of(path);
      this.catalog_table_model.reset();
      this.catalog_tree_model.changeTree(c);
    }
  }

  /**
   * Add a disk to the catalog.
   *
   * @param disk_name The name of the disk
   * @param disk_id   The disk ID
   * @param path      The path to the root of the disk
   *
   * @throws IOException      On I/O errors
   * @throws CatalogException On catalog errors
   */

  public void catalogAddDisk(
    final CatalogDiskName disk_name,
    final CatalogDiskID disk_id,
    final Path path)
    throws IOException, CatalogException
  {
    Model.LOG.debug("adding disk: {} {} {}", disk_name, disk_id, path);

    final ModelState current = this.state_history.peek();
    final SortedMap<CatalogDiskID, CatalogDisk> disks =
      current.getCatalog().getDisks();

    if (disks.containsKey(disk_id)) {
      throw new CatalogDiskDuplicateIndexException(disk_id.toString());
    }

    final CatalogDisk disk =
      CatalogFilesystemReader.newDisk(disk_name, disk_id, path);

    final SortedMap<CatalogDiskID, CatalogDisk> new_disks =
      new TreeMap<>(disks);
    new_disks.put(disk_id, disk);
    final Catalog c = new Catalog(new_disks);

    this.state_history.push(current.withNewCatalog(c));
    this.unsaved.broadcast();
    this.undo.broadcast();
    this.catalog_table_model.fireTableDataChanged();
    this.catalog_tree_model.changeTree(c);
  }

  /**
   * Close the current catalog.
   */

  public void catalogClose()
  {
    Model.LOG.debug("closing catalog");

    final ModelState current = ModelState.newEmpty(BigInteger.ZERO);
    this.state_history.clear(current);
    this.state_revision_saved = BigInteger.ZERO;
    this.unsaved.broadcast();
    this.undo.broadcast();

    this.catalog_file = Optional.empty();
    this.catalog_table_model.reset();
    this.catalog_tree_model.changeTree(current.getCatalog());
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

    final Catalog c = this.state_history.peek().getCatalog();
    Assertive.require(c.getDisks().containsKey(index));
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

    final Catalog c = this.state_history.peek().getCatalog();
    Assertive.require(c.getDisks().containsKey(index));
    this.catalog_table_model.openDiskAtDirectory(index, dir);
  }

  /**
   * Load the root of the catalog into the tabel model.
   */

  public void selectRoot()
  {
    this.catalog_table_model.reset();
  }

  /**
   * Redo the most recent action, if one exists.
   */

  public void catalogRedo()
  {

  }

  /**
   * Undo the most recent action, if one exists.
   */

  public void catalogUndo()
  {
    Model.LOG.debug(
      "undo: requesting at revision {}",
      this.state_history.peek().getRevision());

    if (this.catalogCanUndo() == UndoAvailable.UNDO_AVAILABLE) {
      this.state_history.pop();
      final ModelState current = this.state_history.peek();
      Model.LOG.debug("undo: now at revision {}", current.getRevision());
      this.unsaved.broadcast();
      this.undo.broadcast();
      this.catalog_tree_model.changeTree(current.getCatalog());
      this.catalog_table_model.fireTableDataChanged();
    } else {
      Model.LOG.debug("undo: empty stack");
    }
  }

  /**
   * Subscribe to changes of the unsaved state of the catalog.
   *
   * @param listener The listener
   */

  public void catalogUnsavedChangesSubscribe(
    final Consumer<UnsavedChanges> listener)
  {
    this.unsaved.addObserver(listener);
  }

  /**
   * Subscribe to changes of the undo state of the catalog.
   *
   * @param listener The listener
   */

  public void catalogUndoSubscribe(
    final Consumer<UndoAvailable> listener)
  {
    this.undo.addObserver(listener);
  }

  /**
   * @return A new disk ID that is guaranteed not to exist in the catalog
   */

  public CatalogDiskID catalogGetFreshDiskID()
  {
    final Catalog current = this.state_history.peek().getCatalog();
    final SortedMap<CatalogDiskID, CatalogDisk> disks = current.getDisks();
    if (!disks.isEmpty()) {
      final CatalogDiskID last = disks.lastKey();
      final CatalogDiskID new_id =
        new CatalogDiskID(last.getValue().add(BigInteger.ONE));
      Assertive.ensure(!disks.containsKey(new_id));
      return new_id;
    }

    return new CatalogDiskID(BigInteger.ZERO);
  }

  /**
   * Subscribe to changes of the redo state of the catalog.
   *
   * @param listener The listener
   */

  public void catalogRedoSubscribe(final Consumer<RedoAvailable> listener)
  {
    this.redo.addObserver(listener);
  }
}
