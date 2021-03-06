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

import com.io7m.jwhere.core.CatalogCompress;
import com.io7m.jwhere.core.CatalogSaveSpecification;
import com.io7m.jwhere.gui.ControllerType;
import com.io7m.jwhere.gui.model.UnsavedChanges;
import net.java.dev.designgridlayout.DesignGridLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The application's main window.
 */

public final class MainWindow extends JFrame
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(MainWindow.class);
  }

  private final ControllerType controller;
  private final StatusBar status;
  private final CatalogTab tab_catalog;
  private final SearchTab tab_search;
  private final TasksTab tab_tasks;
  private final VerifyTab tab_verify;

  /**
   * Construct the main window.
   *
   * @param in_controller The application controller
   */

  public MainWindow(final ControllerType in_controller)
  {
    super();

    this.controller = Objects.requireNonNull(in_controller, "in_controller");

    /*
     * Create status bar.
     */

    this.status = new StatusBar(in_controller);
    final var pane = this.getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(this.status, BorderLayout.SOUTH);

    /*
     * Tabs.
     */

    final var tabs = new JTabbedPane();
    this.tab_catalog = new CatalogTab(this, this.status, this.controller);
    this.tab_search = new SearchTab(this.controller);
    this.tab_tasks = new TasksTab(this.controller);
    this.tab_verify = new VerifyTab(this.controller, this.status);
    tabs.add("Catalog", this.tab_catalog);
    tabs.add("Search", this.tab_search);
    tabs.add("Tasks", this.tab_tasks);
    tabs.add("Verify", this.tab_verify);
    pane.add(tabs, BorderLayout.CENTER);

    /*
     * Set the window title and arrange to set it every time the unsaved
     * state of the catalog changes.
     */

    this.setTitle(this.makeTitle(UnsavedChanges.NO_UNSAVED_CHANGES));
    this.controller.catalogUnsavedChangesSubscribe(
      c -> SwingUtilities.invokeLater(() -> this.setTitle(this.makeTitle(c))));

    this.setMinimumSize(new Dimension(640, 480));
    this.setJMenuBar(makeMenu(this, this.status, in_controller));
    this.addWindowListener(
      new WindowAdapter()
      {
        @Override
        public void windowClosing(final WindowEvent e)
        {
          onActionExit(
            MainWindow.this,
            MainWindow.this.status,
            MainWindow.this.controller);
        }
      });
  }

  private static JMenuBar makeMenu(
    final JFrame window,
    final StatusBar status,
    final ControllerType controller)
  {
    final var bar = new JMenuBar();
    final var file = makeMenuFile(window, status, controller);
    final var edit = makeMenuEdit(window, status, controller);
    bar.add(file);
    bar.add(edit);
    return bar;
  }

  private static JMenu makeMenuEdit(
    final JFrame window,
    final StatusBar status,
    final ControllerType controller)
  {
    final var edit_undo = new JMenuItem("Undo");
    edit_undo.setMnemonic('U');
    edit_undo.addActionListener((e) -> controller.catalogUndo());
    edit_undo.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));
    edit_undo.setModel(new UndoButtonModel(controller));

    final var edit_redo = new JMenuItem("Redo");
    edit_redo.setMnemonic('R');
    edit_redo.addActionListener((e) -> controller.catalogRedo());
    edit_redo.setAccelerator(
      KeyStroke.getKeyStroke(
        KeyEvent.VK_Z, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
    edit_redo.setModel(new RedoButtonModel(controller));

    final var edit = new JMenu("Edit");
    edit.setMnemonic('E');
    edit.add(edit_undo);
    edit.add(edit_redo);

    return edit;
  }

  private static JMenu makeMenuFile(
    final JFrame window,
    final StatusBar status,
    final ControllerType controller)
  {
    final var file_new = new JMenuItem("New");
    file_new.setMnemonic('N');
    file_new.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));

    final var file_open = new JMenuItem("Open...");
    file_open.setMnemonic('O');
    file_open.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
    file_open.addActionListener(
      e -> onActionCatalogSaveOpen(window, status, controller));

    final var file_save = new JMenuItem("Save");
    file_save.setMnemonic('S');
    file_save.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
    file_save.addActionListener(
      (e) -> onActionCatalogSave(window, status, controller));
    file_save.setModel(new UnsavedButtonModel(controller));

    final var file_save_as = new JMenuItem("Save as...");
    file_save_as.addActionListener(
      (e) -> onActionCatalogSaveAs(window, status, controller));

    final var file_close = new JMenuItem("Close");
    file_close.addActionListener(
      (e) -> onActionCatalogClose(window, status, controller));

    final var file_exit = new JMenuItem("Exit");
    file_exit.setMnemonic('X');
    file_exit.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
    file_exit.addActionListener(
      (e) -> onActionExit(window, status, controller));

    final var file = new JMenu("File");
    file.setMnemonic('F');
    file.add(file_new);
    file.add(file_open);
    file.add(file_save);
    file.add(file_save_as);
    file.add(file_close);
    file.add(new JSeparator());
    file.add(file_exit);

    return file;
  }

  private static void onActionExit(
    final JFrame window,
    final StatusBar status,
    final ControllerType controller)
  {
    final Supplier<UnsavedChangesChoice> on_unsaved_changes =
      () -> UnsavedChangesDialog.showUnsavedChangesDialog(window);
    final Supplier<Optional<CatalogSaveSpecification>>
      on_want_save_file = () -> onWantSaveFile(window);

    /*
     * Evaluated when saving starts.
     */

    final Runnable on_start_io = () -> {
      status.onProgressIndeterminateStartLater();
      status.onInfoLater("Exiting...");
    };

    /*
     * Evaluated when closing/saving is finished.
     */

    final Consumer<Optional<Throwable>> on_finish_io = (ex_opt) -> {
      if (ex_opt.isPresent()) {
        final var ex = ex_opt.get();
        LOG.error("closing/saving failed: ", ex);
        status.onErrorLater("Closing failed!");
        status.onProgressIndeterminateFinishLater();
        ErrorBox.showErrorLater(ex);
      } else {
        LOG.debug("closing/saving finished");
        controller.programExit(0);
      }
    };

    controller.catalogClose(
      on_unsaved_changes, on_want_save_file, on_start_io, on_finish_io);
  }

  private static void onActionCatalogSaveOpen(
    final JFrame window,
    final StatusBar status,
    final ControllerType controller)
  {
    final Supplier<UnsavedChangesChoice> on_unsaved_changes =
      () -> UnsavedChangesDialog.showUnsavedChangesDialog(window);
    final Supplier<Optional<CatalogSaveSpecification>> on_want_save_file =
      () -> onWantSaveFile(window);
    final Supplier<Optional<Path>> on_want_open_file =
      () -> onWantOpenFile(window);

    /*
     * Evaluated when saving/loading starts.
     */

    final Runnable on_start_io = () -> {
      status.onProgressIndeterminateStartLater();
      status.onInfoLater("Loading...");
    };

    /*
     * Evaluated when saving/loading is finished.
     */

    final Consumer<Optional<Throwable>> on_finish_io = (ex_opt) -> {
      if (ex_opt.isPresent()) {
        final var ex = ex_opt.get();
        LOG.error("loading/saving failed: ", ex);
        status.onErrorLater("Load failed!");
        status.onProgressIndeterminateFinishLater();
        ErrorBox.showErrorLater(ex);
      } else {
        status.onInfoLater("Loaded catalog");
        status.onProgressIndeterminateFinishLater();
      }
    };

    controller.catalogOpen(
      on_unsaved_changes,
      on_want_save_file,
      on_want_open_file,
      on_start_io,
      on_finish_io);
  }

  private static void onActionCatalogSave(
    final JFrame window,
    final StatusBar status,
    final ControllerType controller)
  {
    final Supplier<Optional<CatalogSaveSpecification>> on_want_save_file =
      () -> onWantSaveFile(window);

    /*
     * Evaluated when closing/saving starts.
     */

    final Runnable on_start_io = () -> {
      status.onProgressIndeterminateStartLater();
      status.onInfoLater("Saving...");
    };

    /*
     * Evaluated when closing/saving is finished.
     */

    final Consumer<Optional<Throwable>> on_finish_io = (ex_opt) -> {
      if (ex_opt.isPresent()) {
        final var ex = ex_opt.get();
        LOG.error("saving failed: ", ex);
        status.onErrorLater("Save failed!");
        status.onProgressIndeterminateFinishLater();
        ErrorBox.showErrorLater(ex);
      } else {
        status.onInfoLater("Save completed");
        status.onProgressIndeterminateFinishLater();
      }
    };

    controller.catalogSave(
      on_want_save_file, on_start_io, on_finish_io);
  }

  private static void onActionCatalogSaveAs(
    final JFrame window,
    final StatusBar status,
    final ControllerType controller)
  {
    final Supplier<Optional<CatalogSaveSpecification>> on_want_save_file =
      () -> onWantSaveFile(window);

    /*
     * Evaluated when closing/saving starts.
     */

    final Runnable on_start_io = () -> {
      status.onProgressIndeterminateStartLater();
      status.onInfoLater("Saving...");
    };

    /*
     * Evaluated when closing/saving is finished.
     */

    final Consumer<Optional<Throwable>> on_finish_io = (ex_opt) -> {
      if (ex_opt.isPresent()) {
        final var ex = ex_opt.get();
        LOG.error("saving failed: ", ex);
        status.onErrorLater("Save failed!");
        status.onProgressIndeterminateFinishLater();
        ErrorBox.showErrorLater(ex);
      } else {
        status.onInfoLater("Save completed");
        status.onProgressIndeterminateFinishLater();
      }
    };

    controller.catalogSaveAs(
      on_want_save_file, on_start_io, on_finish_io);
  }

  private static void onActionCatalogClose(
    final JFrame window,
    final StatusBar status,
    final ControllerType controller)
  {
    final Supplier<UnsavedChangesChoice> on_unsaved_changes =
      () -> UnsavedChangesDialog.showUnsavedChangesDialog(window);
    final Supplier<Optional<CatalogSaveSpecification>> on_want_save_file =
      () -> onWantSaveFile(window);

    /*
     * Evaluated when closing/saving starts.
     */

    final Runnable on_start_io = () -> {
      status.onProgressIndeterminateStartLater();
      status.onInfoLater("Closing...");
    };

    /*
     * Evaluated when closing/saving is finished.
     */

    final Consumer<Optional<Throwable>> on_finish_io = (ex_opt) -> {
      if (ex_opt.isPresent()) {
        final var ex = ex_opt.get();
        LOG.error("closing/saving failed: ", ex);
        status.onErrorLater("Closing failed!");
        status.onProgressIndeterminateFinishLater();
        ErrorBox.showErrorLater(ex);
      } else {
        status.onInfoLater("Closed catalog");
        status.onProgressIndeterminateFinishLater();
      }
    };

    controller.catalogClose(
      on_unsaved_changes, on_want_save_file, on_start_io, on_finish_io);
  }

  /**
   * Evaluated when a filename is required to open a new catalog. Returning nothing indicates that
   * the whole process should be aborted.
   */

  private static Optional<Path> onWantOpenFile(final JFrame window)
  {
    final Optional<Path> r_path;
    final var chooser = new JFileChooser();
    final FileFilter filter = new CatalogFileFilter();
    chooser.addChoosableFileFilter(filter);
    chooser.setFileFilter(filter);
    final var r = chooser.showOpenDialog(window);
    if (r == JFileChooser.APPROVE_OPTION) {
      final var file = chooser.getSelectedFile();
      LOG.debug("open: selected {}", file);
      r_path = Optional.of(file.toPath());
    } else {
      r_path = Optional.empty();
    }
    return r_path;
  }

  /**
   * Evaluated when a filename is required to save changes. Returning nothing indicates that the
   * whole process should be aborted.
   */

  private static Optional<CatalogSaveSpecification> onWantSaveFile(
    final JFrame window)
  {
    final var spec_panel = new SaveSpecificationPanel();
    final Optional<CatalogSaveSpecification> r_path;
    final var chooser = new JFileChooser();
    final FileFilter filter = new CatalogFileFilter();
    chooser.addChoosableFileFilter(filter);
    chooser.setFileFilter(filter);
    chooser.setAccessory(spec_panel);

    final var r = chooser.showSaveDialog(window);
    if (r == JFileChooser.APPROVE_OPTION) {
      final var file = chooser.getSelectedFile();
      final var compress = spec_panel.isCompressSelected();
      final CatalogSaveSpecification spec;
      if (compress) {
        spec =
          CatalogSaveSpecification.builder()
            .setCompress(CatalogCompress.COMPRESS_GZIP)
            .setPath(file.toPath())
            .build();
      } else {
        spec =
          CatalogSaveSpecification.builder()
            .setCompress(CatalogCompress.COMPRESS_NONE)
            .setPath(file.toPath())
            .build();
      }

      LOG.debug("save: selected {}", spec);
      r_path = Optional.of(spec);
    } else {
      r_path = Optional.empty();
    }

    return r_path;
  }

  private String makeTitle(final UnsavedChanges c)
  {
    final var p = this.getClass().getPackage();
    final var sb = new StringBuilder(64);
    sb.append(p.getImplementationTitle());
    sb.append(" ");
    sb.append(p.getImplementationVersion());

    switch (c) {
      case NO_UNSAVED_CHANGES:
        break;
      case UNSAVED_CHANGES:
        sb.append(" (modified)");
    }
    return sb.toString();
  }

  private static final class SaveSpecificationPanel extends JPanel
  {
    private final JCheckBox compress;

    SaveSpecificationPanel()
    {
      this.compress = new JCheckBox();
      this.compress.setSelected(true);

      final var dg = new DesignGridLayout(this);
      dg.row().grid(new JLabel("Compress")).add(this.compress);
    }

    boolean isCompressSelected()
    {
      return this.compress.isSelected();
    }
  }

  private static final class CatalogFileFilter extends FileFilter
  {
    CatalogFileFilter()
    {

    }

    @Override
    public boolean accept(final File f)
    {
      final var name = f.getName();
      final var likely_file =
        f.isFile() && name.endsWith(".jcz") || name.endsWith(".jcz");
      return f.isDirectory() || likely_file;
    }

    @Override
    public String getDescription()
    {
      return "Catalogs (*.jc, *.jcz)";
    }
  }
}
