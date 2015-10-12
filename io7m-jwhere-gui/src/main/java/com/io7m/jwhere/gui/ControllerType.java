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

package com.io7m.jwhere.gui;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.ProcedureType;
import com.io7m.jfunctional.Unit;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskName;
import com.io7m.jwhere.gui.model.RedoAvailable;
import com.io7m.jwhere.gui.model.UndoAvailable;
import com.io7m.jwhere.gui.model.UnsavedChanges;
import com.io7m.jwhere.gui.view.UnsavedChangesChoice;

import javax.swing.ListModel;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The main controller.
 */

public interface ControllerType
{
  /**
   * @return The current catalog's table model
   */

  TableModel catalogGetTableModel();

  /**
   * @return A value indicating if the catalog has unsaved changes
   */

  UnsavedChanges catalogIsUnsaved();

  /**
   * Open a catalog.
   *
   * @param on_unsaved           A function that, when evaluated, indicates what
   *                             should be done with any unsaved changes that
   *                             may exist.
   * @param on_no_save_file_name A function that, when evaluated, indicates the
   *                             file name that will contain the saved catalog,
   *                             iff unsaved changes are to be saved and no
   *                             current catalog filename has been chosen.
   * @param on_open_file         A function that, when evaluated, will return
   *                             the filename of the catalog that will be
   *                             opened.
   * @param on_start_io          A procedure that, when evaluated, indicates
   *                             that the operation has started.
   * @param on_finish_io         A procedure that, when evaluated, indicates
   *                             that the operation has started, passing it a
   *                             non-empty optional exception in the case of
   *                             failure.
   */

  void catalogOpen(
    FunctionType<Unit, UnsavedChangesChoice> on_unsaved,
    FunctionType<Unit, Optional<Path>> on_no_save_file_name,
    FunctionType<Unit, Optional<Path>> on_open_file,
    Runnable on_start_io,
    ProcedureType<Optional<Throwable>> on_finish_io);

  /**
   * Close the current catalog.
   *
   * @param on_unsaved_changes A function that, when evaluated, indicates what
   *                           should be done with any unsaved changes that may
   *                           exist.
   * @param on_want_save_file  A function that, when evaluated, indicates the
   *                           file name that will contain the saved catalog,
   *                           iff unsaved changes are to be saved and no
   *                           current catalog filename has been chosen.
   * @param on_start_io        A procedure that, when evaluated, indicates that
   *                           the operation has started.
   * @param on_finish_io       A procedure that, when evaluated, indicates that
   *                           the operation has started, passing it a non-empty
   *                           optional exception in the case of failure.
   */

  void catalogClose(
    FunctionType<Unit, UnsavedChangesChoice> on_unsaved_changes,
    FunctionType<Unit, Optional<Path>> on_want_save_file,
    Runnable on_start_io,
    ProcedureType<Optional<Throwable>> on_finish_io);

  /**
   * The user has explicitly told the program to exit.
   *
   * @param status The status code
   */

  void programExit(int status);

  /**
   * Save the current catalog.
   *
   * @param on_want_save_file A function that, when evaluated, indicates the
   *                          file name that will contain the saved catalog, iff
   *                          no current catalog filename has been chosen.
   * @param on_start_io       A procedure that, when evaluated, indicates that
   *                          the operation has started.
   * @param on_finish_io      A procedure that, when evaluated, indicates that
   *                          the operation has started, passing it a non-empty
   *                          optional exception in the case of failure.
   */

  void catalogSave(
    FunctionType<Unit, Optional<Path>> on_want_save_file,
    Runnable on_start_io,
    ProcedureType<Optional<Throwable>> on_finish_io);

  /**
   * Save the current catalog under a different name to the one that it is
   * currently using.
   *
   * @param on_want_save_file A function that, when evaluated, indicates the
   *                          file name that will contain the saved catalog.
   * @param on_start_io       A procedure that, when evaluated, indicates that
   *                          the operation has started.
   * @param on_finish_io      A procedure that, when evaluated, indicates that
   *                          the operation has started, passing it a non-empty
   *                          optional exception in the case of failure.
   */

  void catalogSaveAs(
    FunctionType<Unit, Optional<Path>> on_want_save_file,
    Runnable on_start_io,
    ProcedureType<Optional<Throwable>> on_finish_io);

  /**
   * @return The currrent catalog disk tree model
   */

  TreeModel catalogGetTreeModel();

  /**
   * Load the root directory of disk {@code index} into the table model. The
   * directory is required to exist in disk {@code index}.
   *
   * @param index The disk ID
   */

  void catalogSelectDiskAtRoot(CatalogDiskID index);

  /**
   * Load the directory {@code dir} for disk {@code index} into the table model.
   * The directory is required to exist in disk {@code index}.
   *
   * @param index The disk ID
   * @param dir   The directory
   */

  void catalogSelectDiskAtDirectory(
    CatalogDiskID index,
    CatalogDirectoryNode dir);

  /**
   * Load the catalog into the table model.
   */

  void catalogSelectRoot();

  /**
   * @return The task list model
   */

  ListModel<CatalogTask> catalogGetTasksListModel();

  /**
   * Subscribe for changes to the saved or unsaved state of the catalog.
   *
   * @param listener The listener that will receive the state of the catalog
   */

  void catalogUnsavedChangesSubscribe(Consumer<UnsavedChanges> listener);

  /**
   * Subscribe for changes to the availability of undo steps for the catalog.
   *
   * @param listener The listener that will receive the state of the catalog
   */

  void catalogUndoSubscribe(Consumer<UndoAvailable> listener);

  /**
   * Subscribe for changes to the availability of redo steps for the catalog.
   *
   * @param listener The listener that will receive the state of the catalog
   */

  void catalogRedoSubscribe(Consumer<RedoAvailable> listener);

  /**
   * Redo the last undone operation.
   *
   * @see #catalogUndo()
   */

  void catalogRedo();

  /**
   * Undo the last operation.
   */

  void catalogUndo();

  /**
   * Add a disk to the catalog.
   *
   * @param disk_name    The name that will be used for the disk.
   * @param disk_id      The disk ID
   * @param path         The root of the filesystem
   * @param on_start_io  A procedure that, when evaluated, indicates that the
   *                     operation has started.
   * @param on_finish_io A procedure that, when evaluated, indicates that the
   *                     operation has started, passing it a non-empty optional
   *                     exception in the case of failure.
   */

  void catalogAddDisk(
    CatalogDiskName disk_name,
    CatalogDiskID disk_id,
    Path path,
    Runnable on_start_io,
    ProcedureType<Optional<Throwable>> on_finish_io);

  /**
   * @return A new disk ID that is guaranteed not to be equal to any other disk
   * ID in the catalog at the time of the call
   */

  CatalogDiskID catalogGetFreshDiskID();

}
