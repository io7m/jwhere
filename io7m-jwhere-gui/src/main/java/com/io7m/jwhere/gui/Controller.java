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
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jwhere.core.CatalogDirectoryNode;
import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogException;
import com.io7m.jwhere.gui.model.Model;
import com.io7m.jwhere.gui.view.UnsavedChangesChoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class Controller implements ControllerType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(Controller.class);
  }

  private final Model           model;
  private final ExecutorService exec;

  private Controller(final Model in_model)
  {
    this.model = NullCheck.notNull(in_model);
    this.exec = Executors.newSingleThreadExecutor();
  }

  public static ControllerType newController(final Model model)
  {
    return new Controller(model);
  }

  @Override public TableModel catalogGetTableModel()
  {
    return this.model.getCatalogTableModel();
  }

  @Override public boolean catalogIsUnsaved()
  {
    return this.model.isCatalogUnsaved();
  }

  @Override public void catalogOpen(
    final FunctionType<Unit, UnsavedChangesChoice> on_unsaved_changes,
    final FunctionType<Unit, Optional<Path>> on_want_save_file,
    final FunctionType<Unit, Optional<Path>> on_open_file,
    final Runnable on_start_io,
    final ProcedureType<Optional<Throwable>> on_finish_io)
  {
    /**
     * Check to see if saving is desired, or if the whole thing should be
     * aborted.
     */

    Optional<Path> save_file = Optional.empty();
    boolean cancel = false;

    try {
      save_file = this.getSaveFileForUnsavedChanges(
        on_unsaved_changes, on_want_save_file);
    } catch (final CancellationException ex) {
      cancel = true;
    }

    final Optional<Path> save_file_last = save_file;

    if (!cancel) {
      final Optional<Path> open_file = on_open_file.call(Unit.unit());
      if (open_file.isPresent()) {
        CompletableFuture.supplyAsync(
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
          (ok, ex) -> on_finish_io.call(Optional.ofNullable(ex)));
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

  private Optional<Path> getSaveFileForUnsavedChanges(
    final FunctionType<Unit, UnsavedChangesChoice> on_unsaved_changes,
    final FunctionType<Unit, Optional<Path>> on_want_save_file)
    throws CancellationException
  {
    if (this.catalogIsUnsaved()) {
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
        default: {
          throw new UnreachableCodeException();
        }
      }
    } else {
      return Optional.empty();
    }
  }

  /**
   * Make all the requests necessary to get a filename for saving. Throw {@link
   * CancellationException} if the whole operation should be aborted.
   */

  private Optional<Path> getSaveFile(
    final FunctionType<Unit, Optional<Path>> on_want_save_file)
    throws CancellationException
  {
    final Optional<Path> file_opt = this.model.getCatalogFileName();
    if (file_opt.isPresent()) {
      Controller.LOG.debug("using existing file name");
      return file_opt;
    } else {
      Controller.LOG.debug("no save file specified, asking user");
      final Optional<Path> save_opt = on_want_save_file.call(Unit.unit());
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
    final FunctionType<Unit, Optional<Path>> on_want_save_file,
    final Runnable on_start_io,
    final ProcedureType<Optional<Throwable>> on_finish_io)
  {
    /**
     * Check to see if saving is desired, or if the whole thing should be
     * aborted.
     */

    Optional<Path> save_file = Optional.empty();
    boolean cancel = false;

    try {
      save_file = this.getSaveFileForUnsavedChanges(
        on_unsaved_changes, on_want_save_file);
    } catch (final CancellationException ex) {
      cancel = true;
    }

    final Optional<Path> save_file_opt = save_file;
    if (!cancel) {
      CompletableFuture.supplyAsync(
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
        (ok, ex) -> on_finish_io.call(Optional.ofNullable(ex)));
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
    final FunctionType<Unit, Optional<Path>> on_want_save_file,
    final Runnable on_start_io,
    final ProcedureType<Optional<Throwable>> on_finish_io)
  {
    try {
      final Optional<Path> save_file = this.getSaveFile(on_want_save_file);
      CompletableFuture.supplyAsync(
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
        (ok, ex) -> on_finish_io.call(Optional.ofNullable(ex)));
    } catch (final CancellationException ex) {
      Controller.LOG.debug("aborting save");
    }
  }

  @Override public void catalogSaveAs(
    final FunctionType<Unit, Optional<Path>> on_want_save_file,
    final Runnable on_start_io,
    final ProcedureType<Optional<Throwable>> on_finish_io)
  {
    try {
      final Optional<Path> save_file = on_want_save_file.call(Unit.unit());
      if (save_file.isPresent()) {
        CompletableFuture.supplyAsync(
          () -> {
            try {
              on_start_io.run();
              this.model.catalogSave(save_file.get());
              return Unit.unit();
            } catch (IOException e) {
              throw new IOError(e);
            }
          }, this.exec).whenComplete(
          (ok, ex) -> on_finish_io.call(Optional.ofNullable(ex)));
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
    final CatalogDirectoryNode dir)
  {
    this.model.selectDiskAtDirectory(index, dir);
  }

  @Override public void catalogSelectRoot()
  {
    this.model.selectRoot();
  }
}
