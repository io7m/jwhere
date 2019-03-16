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
import java.util.Objects;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDirectoryNodeType;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskMetadata;
import com.io7m.jwhere.core.CatalogDiskName;
import com.io7m.jwhere.core.CatalogException;
import com.io7m.jwhere.core.CatalogSaveSpecification;
import com.io7m.jwhere.gui.model.Model;
import com.io7m.jwhere.gui.model.RedoAvailable;
import com.io7m.jwhere.gui.model.UndoAvailable;
import com.io7m.jwhere.gui.model.UnsavedChanges;
import com.io7m.jwhere.gui.view.UnsavedChangesChoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * The default implementation of the {@link ControllerType}
 */

public final class Controller implements ControllerType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(Controller.class);
  }

  private final Model                         model;
  private final ExecutorService               exec;
  private final Map<Long, CatalogTask>        tasks;
  private final AtomicLong                    task_ids;
  private final DefaultListModel<CatalogTask> tasks_list_model;

  private Controller(final Model in_model)
  {
    this.model = Objects.requireNonNull(in_model, "in_model");
    this.exec = Executors.newSingleThreadExecutor(
      r -> {
        final Thread thread = new Thread(r);
        thread.setName("controller-task");
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
      });
    this.tasks = new LinkedHashMap<>(8);
    this.task_ids = new AtomicLong(0L);
    this.tasks_list_model = new DefaultListModel<>();
  }

  /**
   * Construct a new controller.
   *
   * @param model The model
   *
   * @return A new controller
   */

  public static ControllerType newController(final Model model)
  {
    return new Controller(model);
  }

  @Override public TableModel catalogGetTableModel()
  {
    return this.model.getCatalogTableModel();
  }

  @Override public UnsavedChanges catalogIsUnsaved()
  {
    return this.model.isCatalogUnsaved();
  }

  @Override public void catalogOpen(
    final FunctionType<Unit, UnsavedChangesChoice> on_unsaved_changes,
    final FunctionType<Unit, Optional<CatalogSaveSpecification>>
      on_want_save_file,
    final FunctionType<Unit, Optional<Path>> on_open_file,
    final Runnable on_start_io,
    final ProcedureType<Optional<Throwable>> on_finish_io)
  {
    /**
     * Check to see if saving is desired, or if the whole thing should be
     * aborted.
     */

    Optional<CatalogSaveSpecification> save_file = Optional.empty();
    boolean cancel = false;

    try {
      save_file = this.getSaveFileForUnsavedChanges(
        on_unsaved_changes, on_want_save_file);
    } catch (final CancellationException ex) {
      cancel = true;
    }

    final Optional<CatalogSaveSpecification> save_file_last = save_file;

    if (!cancel) {
      final Optional<Path> open_file = on_open_file.call(Unit.unit());
      if (open_file.isPresent()) {
        this.taskSubmit(
          "Open catalog", CompletableFuture.supplyAsync(
            () -> {
              try {
                on_start_io.run();
                if (save_file_last.isPresent()) {
                  this.model.catalogSave(save_file_last.get());
                }
                this.model.catalogOpen(open_file.get());
                return Unit.unit();
              } catch (IOException | CatalogException e) {
                throw new IOError(e);
              }
            }, this.exec).whenComplete(
            (ok, ex) -> on_finish_io.call(Optional.ofNullable(ex))));
      } else {
        Controller.LOG.debug("cancelled open explicitly");
      }
    } else {
      Controller.LOG.debug("cancelled open via save");
    }
  }

  /**
   * If there are unsaved changes, make all the requests necessary to get a
   * filename for saving. Otherwise, throw {@link CancellationException} if the
   * whole operation should be aborted.
   */

  private Optional<CatalogSaveSpecification> getSaveFileForUnsavedChanges(
    final FunctionType<Unit, UnsavedChangesChoice> on_unsaved_changes,
    final FunctionType<Unit, Optional<CatalogSaveSpecification>>
      on_want_save_file)
    throws CancellationException
  {
    switch (this.catalogIsUnsaved()) {
      case NO_UNSAVED_CHANGES:
        return Optional.empty();
      case UNSAVED_CHANGES:
        switch (on_unsaved_changes.call(Unit.unit())) {
          case UNSAVED_CHANGES_DISCARD: {
            Controller.LOG.debug("discarding unsaved changes");
            return Optional.empty();
          }
          case UNSAVED_CHANGES_CANCEL: {
            Controller.LOG.debug("cancelling");
            throw new CancellationException();
          }
          case UNSAVED_CHANGES_SAVE: {
            Controller.LOG.debug("user wants to save changes");
            return this.getSaveFile(on_want_save_file);
          }
        }
    }

    throw new UnreachableCodeException();
  }

  /**
   * Make all the requests necessary to get a filename for saving. Throw {@link
   * CancellationException} if the whole operation should be aborted.
   */

  private Optional<CatalogSaveSpecification> getSaveFile(
    final FunctionType<Unit, Optional<CatalogSaveSpecification>>
      on_want_save_file)
    throws CancellationException
  {
    final Optional<CatalogSaveSpecification> file_opt =
      this.model.getCatalogSaveSpecification();
    if (file_opt.isPresent()) {
      Controller.LOG.debug("using existing file name");
      return file_opt;
    } else {
      Controller.LOG.debug("no save file specified, asking user");
      final Optional<CatalogSaveSpecification> save_opt =
        on_want_save_file.call(Unit.unit());
      if (save_opt.isPresent()) {
        Controller.LOG.debug("provided save file");
        return save_opt;
      } else {
        Controller.LOG.debug("no save file specified, aborting");
        throw new CancellationException();
      }
    }
  }

  @Override public void catalogClose(
    final FunctionType<Unit, UnsavedChangesChoice> on_unsaved_changes,
    final FunctionType<Unit, Optional<CatalogSaveSpecification>>
      on_want_save_file,
    final Runnable on_start_io,
    final ProcedureType<Optional<Throwable>> on_finish_io)
  {
    /**
     * Check to see if saving is desired, or if the whole thing should be
     * aborted.
     */

    Optional<CatalogSaveSpecification> save_file = Optional.empty();
    boolean cancel = false;

    try {
      save_file = this.getSaveFileForUnsavedChanges(
        on_unsaved_changes, on_want_save_file);
    } catch (final CancellationException ex) {
      cancel = true;
    }

    final Optional<CatalogSaveSpecification> save_file_opt = save_file;
    if (!cancel) {
      this.taskSubmit(
        "Close catalog", CompletableFuture.supplyAsync(
          () -> {
            try {
              on_start_io.run();
              if (save_file_opt.isPresent()) {
                this.model.catalogSave(save_file_opt.get());
              }
              this.model.catalogClose();
              return Unit.unit();
            } catch (IOException e) {
              throw new IOError(e);
            }
          }, this.exec).whenComplete(
          (ok, ex) -> on_finish_io.call(Optional.ofNullable(ex))));
    } else {
      Controller.LOG.debug("aborting close");
    }
  }

  @Override public void programExit(final int status)
  {
    Controller.LOG.debug("exiting");
    System.exit(status);
  }

  @Override public void catalogSave(
    final FunctionType<Unit, Optional<CatalogSaveSpecification>>
      on_want_save_file,
    final Runnable on_start_io,
    final ProcedureType<Optional<Throwable>> on_finish_io)
  {
    try {
      final Optional<CatalogSaveSpecification> save_file =
        this.getSaveFile(on_want_save_file);
      this.taskSubmit(
        "Save catalog", CompletableFuture.supplyAsync(
          () -> {
            try {
              on_start_io.run();
              if (save_file.isPresent()) {
                this.model.catalogSave(save_file.get());
              }
              return Unit.unit();
            } catch (IOException e) {
              throw new IOError(e);
            }
          }, this.exec).whenComplete(
          (ok, ex) -> on_finish_io.call(Optional.ofNullable(ex))));
    } catch (final CancellationException ex) {
      Controller.LOG.debug("aborting save");
    }
  }

  @Override public void catalogSaveAs(
    final FunctionType<Unit, Optional<CatalogSaveSpecification>>
      on_want_save_file,
    final Runnable on_start_io,
    final ProcedureType<Optional<Throwable>> on_finish_io)
  {
    try {
      final Optional<CatalogSaveSpecification> save_file =
        on_want_save_file.call(Unit.unit());
      if (save_file.isPresent()) {
        this.taskSubmit(
          "Save catalog", CompletableFuture.supplyAsync(
            () -> {
              try {
                on_start_io.run();
                this.model.catalogSave(save_file.get());
                return Unit.unit();
              } catch (IOException e) {
                throw new IOError(e);
              }
            }, this.exec).whenComplete(
            (ok, ex) -> on_finish_io.call(Optional.ofNullable(ex))));
      }
    } catch (final CancellationException ex) {
      Controller.LOG.debug("aborting save");
    }
  }

  @Override public TreeModel catalogGetTreeModel()
  {
    return this.model.getCatalogTreeModel();
  }

  @Override public void catalogSelectDiskAtRoot(final CatalogDiskID index)
  {
    this.model.selectDiskAtRoot(index);
  }

  @Override public void catalogSelectDiskAtDirectory(
    final CatalogDiskID index,
    final CatalogDirectoryNodeType dir)
  {
    this.model.selectDiskAtDirectory(index, dir);
  }

  @Override public void catalogSelectRoot()
  {
    this.model.selectRoot();
  }

  @Override public ListModel<CatalogTask> catalogGetTasksListModel()
  {
    return this.tasks_list_model;
  }

  @Override public void catalogUnsavedChangesSubscribe(
    final Consumer<UnsavedChanges> listener)
  {
    this.model.catalogUnsavedChangesSubscribe(listener);
  }

  @Override public void catalogUndoSubscribe(
    final Consumer<UndoAvailable> listener)
  {
    this.model.catalogUndoSubscribe(listener);
  }

  @Override public void catalogRedoSubscribe(
    final Consumer<RedoAvailable> listener)
  {
    this.model.catalogRedoSubscribe(listener);
  }

  @Override public void catalogRedo()
  {
    this.model.catalogRedo();
  }

  @Override public void catalogUndo()
  {
    this.model.catalogUndo();
  }

  @Override public void catalogAddDisk(
    final CatalogDiskName disk_name,
    final CatalogDiskID disk_id,
    final Path path,
    final Runnable on_start_io,
    final ProcedureType<Optional<Throwable>> on_finish_io)
  {
    this.taskSubmit(
      "Add disk", CompletableFuture.supplyAsync(
        () -> {
          try {
            on_start_io.run();
            this.model.catalogAddDisk(disk_name, disk_id, path);
            return Unit.unit();
          } catch (IOException | CatalogException e) {
            throw new IOError(e);
          }
        }, this.exec).whenComplete(
        (ok, ex) -> on_finish_io.call(Optional.ofNullable(ex))));
  }

  @Override public CatalogDiskID catalogGetFreshDiskID()
  {
    return this.model.catalogGetFreshDiskID();
  }

  @Override public void catalogVerifyDisk(
    final CatalogDiskID id,
    final Path path,
    final Runnable on_start_io,
    final ProcedureType<Optional<Throwable>> on_finish_io)
  {
    this.taskSubmit(
      "Verify disk", CompletableFuture.supplyAsync(
        () -> {
          try {
            on_start_io.run();
            this.model.catalogVerifyDisk(id, path);
            return Unit.unit();
          } catch (IOException | CatalogException e) {
            throw new IOError(e);
          }
        }, this.exec).whenComplete(
        (ok, ex) -> on_finish_io.call(Optional.ofNullable(ex))));
  }

  @Override public ComboBoxModel<CatalogDiskMetadata> catalogGetComboBoxModel()
  {
    return this.model.getCatalogComboBoxModel();
  }

  @Override public TableModel catalogGetVerificationTableModel()
  {
    return this.model.getVerificationTableModel();
  }

  private Long taskSubmit(
    final String name,
    final CompletableFuture<?> f)
  {
    synchronized (this.tasks) {
      final long id = this.task_ids.incrementAndGet();
      final CatalogTask task = new CatalogTask(f, Long.valueOf(id), name);
      final Long xid = Long.valueOf(id);
      Controller.LOG.debug("submitted task {}: {}", xid, name);
      this.tasks.put(xid, task);
      this.tasks_list_model.addElement(task);
      f.whenComplete((x, ex) -> this.taskFinish(xid));
      return xid;
    }
  }

  private void taskFinish(final Long xid)
  {
    synchronized (this.tasks) {
      final CatalogTask r = this.tasks.remove(xid);
      if (r != null) {
        Controller.LOG.debug("removed task: {}", r);
        this.tasks_list_model.removeElement(r);
      } else {
        Controller.LOG.debug("failed to remove nonexistent task: {}", xid);
      }
    }
  }
}
