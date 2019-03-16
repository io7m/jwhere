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
import com.io7m.jwhere.core.Catalog;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDisk;
import com.io7m.jwhere.core.CatalogDiskDuplicateIDException;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskMetadata;
import com.io7m.jwhere.core.CatalogDiskName;
import com.io7m.jwhere.core.CatalogException;
import com.io7m.jwhere.core.CatalogFilesystemReader;
import com.io7m.jwhere.core.CatalogIgnoreAccessTime;
import com.io7m.jwhere.core.CatalogJSONParser;
import com.io7m.jwhere.core.CatalogJSONParserType;
import com.io7m.jwhere.core.CatalogJSONSerializer;
import com.io7m.jwhere.core.CatalogJSONSerializerType;
import com.io7m.jwhere.core.CatalogSaveSpecification;
import com.io7m.jwhere.core.CatalogVerificationListenerType;
import com.io7m.jwhere.core.CatalogVerificationReportItemErrorType;
import com.io7m.jwhere.core.CatalogVerificationReportItemOKType;
import com.io7m.jwhere.core.CatalogVerificationReportSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valid4j.Assertive;

import javax.swing.ComboBoxModel;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;
import java.io.IOException;
import java.math.BigInteger;
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

  private final CatalogMultiTableModel             catalog_table_model;
  private final CatalogTreeModel                   catalog_tree_model;
  private final Revisions<CatalogState>            catalog_history;
  private final CatalogComboBoxModel               catalog_combo_box_model;
  private final CatalogVerificationTableModel      catalog_verification_model;
  private       Optional<CatalogSaveSpecification> catalog_save_spec;

  /**
   * Construct the model.
   */

  public Model()
  {
    this.catalog_save_spec = Optional.empty();
    this.catalog_history = new Revisions<>(CatalogState.newEmpty(), 32);

    final CatalogTableModel tm =
      new CatalogTableModel(this.catalog_history::getCurrentValue);
    final CatalogDiskTableModel dtm =
      new CatalogDiskTableModel(this.catalog_history::getCurrentValue);
    this.catalog_table_model = new CatalogMultiTableModel(
      this.catalog_history::getCurrentValue, tm, dtm);
    this.catalog_tree_model = new CatalogTreeModel();
    this.catalog_combo_box_model =
      new CatalogComboBoxModel(this.catalog_history::getCurrentValue);

    this.catalog_verification_model = new CatalogVerificationTableModel();
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
    return this.catalog_history.hasUnsavedChanges();
  }

  /**
   * @return The current catalog filename, if any
   */

  public Optional<CatalogSaveSpecification> getCatalogSaveSpecification()
  {
    return this.catalog_save_spec;
  }

  /**
   * Save the catalog.
   *
   * @param spec The save specification
   *
   * @throws IOException On I/O errors
   */

  public void catalogSave(
    final CatalogSaveSpecification spec)
    throws IOException
  {
    Model.LOG.debug("saving catalog to: {}", spec);

    final CatalogJSONSerializerType serial =
      CatalogJSONSerializer.newSerializer();

    final CatalogState current = this.catalog_history.getCurrentValue();
    serial.serializeCatalogToPath(current.getCatalog(), spec);
    this.catalog_history.save();
    this.catalog_save_spec = Optional.of(spec);
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
    final Catalog c = parser.parseCatalogFromPath(path);
    this.catalog_history.reset(CatalogState.newWithCatalog(c));
    this.catalog_save_spec = Optional.of(
      new CatalogSaveSpecification(
        CatalogSaveSpecification.Compress.COMPRESS_GZIP, path));
    this.catalog_table_model.reset();
    this.catalog_tree_model.update(c);
    this.catalog_combo_box_model.update();
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

    final CatalogState current = this.catalog_history.getCurrentValue();
    final SortedMap<CatalogDiskID, CatalogDisk> disks =
      current.getCatalog().getDisks();

    if (disks.containsKey(disk_id)) {
      throw new CatalogDiskDuplicateIDException(disk_id.toString());
    }

    final CatalogDisk disk =
      CatalogFilesystemReader.newDisk(disk_name, disk_id, path);
    final SortedMap<CatalogDiskID, CatalogDisk> new_disks =
      new TreeMap<>(disks);
    new_disks.put(disk_id, disk);
    final Catalog c = new Catalog(new_disks);

    this.catalog_history.newRevision(CatalogState.newWithCatalog(c));
    this.catalog_table_model.fireTableDataChanged();
    this.catalog_tree_model.update(c);
    this.catalog_combo_box_model.update();
  }

  /**
   * Close the current catalog.
   */

  public void catalogClose()
  {
    Model.LOG.debug("closing catalog");

    final CatalogState current = CatalogState.newEmpty();
    this.catalog_history.reset(current);
    this.catalog_save_spec = Optional.empty();
    this.catalog_table_model.reset();
    this.catalog_tree_model.update(current.getCatalog());
    this.catalog_combo_box_model.update();
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
    Objects.requireNonNull(index, "index");

    final Catalog c = this.catalog_history.getCurrentValue().getCatalog();
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
    Objects.requireNonNull(index, "index");
    Objects.requireNonNull(dir, "dir");

    final Catalog c = this.catalog_history.getCurrentValue().getCatalog();
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
    this.catalog_history.redo();
    final CatalogState current = this.catalog_history.getCurrentValue();
    this.catalog_table_model.fireTableDataChanged();
    this.catalog_tree_model.update(current.getCatalog());
    this.catalog_combo_box_model.update();
  }

  /**
   * Undo the most recent action, if one exists.
   */

  public void catalogUndo()
  {
    this.catalog_history.undo();
    final CatalogState current = this.catalog_history.getCurrentValue();
    this.catalog_table_model.fireTableDataChanged();
    this.catalog_tree_model.update(current.getCatalog());
    this.catalog_combo_box_model.update();
    this.catalog_combo_box_model.update();
  }

  /**
   * Subscribe to changes of the unsaved state of the catalog.
   *
   * @param listener The listener
   */

  public void catalogUnsavedChangesSubscribe(
    final Consumer<UnsavedChanges> listener)
  {
    this.catalog_history.subscribeUnsaved(listener);
  }

  /**
   * Subscribe to changes of the undo state of the catalog.
   *
   * @param listener The listener
   */

  public void catalogUndoSubscribe(
    final Consumer<UndoAvailable> listener)
  {
    this.catalog_history.subscribeUndo(listener);
  }

  /**
   * @return A new disk ID that is guaranteed not to exist in the catalog
   */

  public CatalogDiskID catalogGetFreshDiskID()
  {
    final Catalog current = this.catalog_history.getCurrentValue().getCatalog();
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
    this.catalog_history.subscribeRedo(listener);
  }

  /**
   * Verify the disk with {@code id} against {@code path}.
   *
   * @param id   The disk ID
   * @param path The path
   *
   * @throws IOException      On I/O errors
   * @throws CatalogException On other catalog errors
   */

  public void catalogVerifyDisk(
    final CatalogDiskID id,
    final Path path)
    throws IOException, CatalogException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(path, "path");

    final CatalogVerificationReportSettings settings =
      new CatalogVerificationReportSettings(
        CatalogIgnoreAccessTime.IGNORE_ACCESS_TIME);

    final Catalog current = this.catalog_history.getCurrentValue().getCatalog();
    final SortedMap<CatalogDiskID, CatalogDisk> disks = current.getDisks();
    Assertive.require(disks.containsKey(id));
    final CatalogDisk disk = disks.get(id);
    final CatalogVerificationTableModel cvm = this.catalog_verification_model;

    cvm.reset();
    CatalogFilesystemReader.verifyDisk(
      disk, settings, path, new CatalogVerificationListenerType()
      {
        @Override public void onItemVerified(
          final CatalogVerificationReportItemOKType ok)
        {
          cvm.add(ok);
        }

        @Override public void onItemError(
          final CatalogVerificationReportItemErrorType error)
        {
          cvm.add(error);
        }

        @Override public void onCompleted()
        {
          // Nothing
        }
      });
  }

  /**
   * @return The list of disks as a combo box model.
   */

  public ComboBoxModel<CatalogDiskMetadata> getCatalogComboBoxModel()
  {
    return this.catalog_combo_box_model;
  }

  /**
   * @return The table model for verification results
   */

  public TableModel getVerificationTableModel()
  {
    return this.catalog_verification_model;
  }
}
