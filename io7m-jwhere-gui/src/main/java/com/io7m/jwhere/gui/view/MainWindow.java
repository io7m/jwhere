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

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.ProcedureType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jwhere.gui.ControllerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

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
  private final StatusBar      status;
  private final CatalogTab     tab_catalog;
  private final SearchTab      tab_search;

  /**
   * Construct the main window.
   *
   * @param in_controller The application controller
   */

  public MainWindow(final ControllerType in_controller)
  {
    super();

    this.controller = NullCheck.notNull(in_controller);

    /**
     * Create status bar.
     */

    this.status = new StatusBar();
    final Container pane = this.getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(this.status, BorderLayout.SOUTH);

    /**
     * Tabs.
     */

    final JTabbedPane tabs = new JTabbedPane();
    this.tab_catalog = new CatalogTab(this.controller);
    this.tab_search = new SearchTab(this.controller);
    tabs.add("Catalog", this.tab_catalog);
    tabs.add("Search", this.tab_search);
    pane.add(tabs, BorderLayout.CENTER);

    this.setTitle(this.makeTitle());
    this.setMinimumSize(new Dimension(640, 480));
    this.setJMenuBar(MainWindow.makeMenu(this, this.status, in_controller));
    this.addWindowListener(
      new WindowAdapter()
      {
        @Override public void windowClosing(final WindowEvent e)
        {
          MainWindow.onActionCatalogClose(
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
    final JMenuBar bar = new JMenuBar();
    final JMenu file = MainWindow.makeMenuFile(window, status, controller);
    bar.add(file);
    return bar;
  }

  private static JMenu makeMenuFile(
    final JFrame window,
    final StatusBar status,
    final ControllerType controller)
  {
    final JMenuItem file_new = new JMenuItem("New");
    file_new.setMnemonic('N');
    file_new.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));

    final JMenuItem file_open = new JMenuItem("Open...");
    file_open.setMnemonic('O');
    file_open.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
    file_open.addActionListener(
      e -> MainWindow.onActionCatalogSaveOpen(window, status, controller));

    final JMenuItem file_save = new JMenuItem("Save");
    file_save.setMnemonic('S');
    file_save.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
    file_save.addActionListener(
      (e) -> MainWindow.onActionCatalogSave(window, status, controller));

    final JMenuItem file_save_as = new JMenuItem("Save as...");
    file_save_as.addActionListener(
      (e) -> MainWindow.onActionCatalogSaveAs(window, status, controller));

    final JMenuItem file_close = new JMenuItem("Close");
    file_close.addActionListener(
      (e) -> MainWindow.onActionCatalogClose(window, status, controller));

    final JMenuItem file_exit = new JMenuItem("Exit");
    file_exit.setMnemonic('X');
    file_exit.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
    file_exit.addActionListener(
      (e) -> MainWindow.onActionExit(window, status, controller));

    final JMenu file = new JMenu("File");
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
    final FunctionType<Unit, UnsavedChangesChoice> on_unsaved_changes =
      (x) -> UnsavedChangesDialog.showUnsavedChangesDialog(window);
    final FunctionType<Unit, Optional<Path>> on_want_save_file =
      (x) -> MainWindow.onWantSaveFile(window);

    /**
     * Evaluated when saving starts.
     */

    final Runnable on_start_io = () -> status.onInfoLater("Exiting...");

    /**
     * Evaluated when closing/saving is finished.
     */

    final ProcedureType<Optional<Throwable>> on_finish_io = (ex_opt) -> {
      if (ex_opt.isPresent()) {
        final Throwable ex = ex_opt.get();
        MainWindow.LOG.error("closing/saving failed: ", ex);
        status.onErrorLater("Closing failed!");
        ErrorBox.showErrorLater(ex);
      } else {
        MainWindow.LOG.debug("closing/saving finished");
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
    final FunctionType<Unit, UnsavedChangesChoice> on_unsaved_changes =
      (x) -> UnsavedChangesDialog.showUnsavedChangesDialog(window);
    final FunctionType<Unit, Optional<Path>> on_want_save_file =
      (x) -> MainWindow.onWantSaveFile(window);
    final FunctionType<Unit, Optional<Path>> on_want_open_file =
      (x) -> MainWindow.onWantOpenFile(window);

    /**
     * Evaluated when saving/loading starts.
     */

    final Runnable on_start_io = () -> status.onInfoLater("Loading...");

    /**
     * Evaluated when saving/loading is finished.
     */

    final ProcedureType<Optional<Throwable>> on_finish_io = (ex_opt) -> {
      if (ex_opt.isPresent()) {
        final Throwable ex = ex_opt.get();
        MainWindow.LOG.error("loading/saving failed: ", ex);
        status.onErrorLater("Load failed!");
        ErrorBox.showErrorLater(ex);
      } else {
        status.onInfoLater("Loaded catalog");
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
    final FunctionType<Unit, Optional<Path>> on_want_save_file =
      (x) -> MainWindow.onWantSaveFile(window);

    /**
     * Evaluated when closing/saving starts.
     */

    final Runnable on_start_io = () -> status.onInfoLater("Saving...");

    /**
     * Evaluated when closing/saving is finished.
     */

    final ProcedureType<Optional<Throwable>> on_finish_io = (ex_opt) -> {
      if (ex_opt.isPresent()) {
        final Throwable ex = ex_opt.get();
        MainWindow.LOG.error("saving failed: ", ex);
        status.onErrorLater("Save failed!");
        ErrorBox.showErrorLater(ex);
      } else {
        status.onInfoLater("Save completed");
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
    final FunctionType<Unit, Optional<Path>> on_want_save_file =
      (x) -> MainWindow.onWantSaveFile(window);

    /**
     * Evaluated when closing/saving starts.
     */

    final Runnable on_start_io = () -> status.onInfoLater("Saving...");

    /**
     * Evaluated when closing/saving is finished.
     */

    final ProcedureType<Optional<Throwable>> on_finish_io = (ex_opt) -> {
      if (ex_opt.isPresent()) {
        final Throwable ex = ex_opt.get();
        MainWindow.LOG.error("saving failed: ", ex);
        status.onErrorLater("Save failed!");
        ErrorBox.showErrorLater(ex);
      } else {
        status.onInfoLater("Save completed");
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
    final FunctionType<Unit, UnsavedChangesChoice> on_unsaved_changes =
      (x) -> UnsavedChangesDialog.showUnsavedChangesDialog(window);
    final FunctionType<Unit, Optional<Path>> on_want_save_file =
      (x) -> MainWindow.onWantSaveFile(window);

    /**
     * Evaluated when closing/saving starts.
     */

    final Runnable on_start_io = () -> status.onInfoLater("Closing...");

    /**
     * Evaluated when closing/saving is finished.
     */

    final ProcedureType<Optional<Throwable>> on_finish_io = (ex_opt) -> {
      if (ex_opt.isPresent()) {
        final Throwable ex = ex_opt.get();
        MainWindow.LOG.error("closing/saving failed: ", ex);
        status.onErrorLater("Closing failed!");
        ErrorBox.showErrorLater(ex);
      } else {
        status.onInfoLater("Closed catalog");
      }
    };

    controller.catalogClose(
      on_unsaved_changes, on_want_save_file, on_start_io, on_finish_io);
  }

  /**
   * Evaluated when a filename is required to open a new catalog. Returning
   * nothing indicates that the whole process should be aborted.
   */

  private static Optional<Path> onWantOpenFile(final JFrame window)
  {
    final Optional<Path> r_path;
    final JFileChooser chooser = new JFileChooser();
    final int r = chooser.showOpenDialog(window);
    if (r == JFileChooser.APPROVE_OPTION) {
      final File file = chooser.getSelectedFile();
      MainWindow.LOG.debug("open: selected {}", file);
      r_path = Optional.of(file.toPath());
    } else {
      r_path = Optional.empty();
    }
    return r_path;
  }

  /**
   * Evaluated when a filename is required to save changes. Returning nothing
   * indicates that the whole process should be aborted.
   */

  private static Optional<Path> onWantSaveFile(final JFrame window)
  {
    final Optional<Path> r_path;
    final JFileChooser chooser = new JFileChooser();
    final int r = chooser.showSaveDialog(window);
    if (r == JFileChooser.APPROVE_OPTION) {
      final File file = chooser.getSelectedFile();
      MainWindow.LOG.debug("save: selected {}", file);
      r_path = Optional.of(file.toPath());
    } else {
      r_path = Optional.empty();
    }
    return r_path;
  }

  private String makeTitle()
  {
    final Package p = this.getClass().getPackage();
    final StringBuilder sb = new StringBuilder(64);
    sb.append(p.getImplementationTitle());
    sb.append(" ");
    sb.append(p.getImplementationVersion());
    return sb.toString();
  }
}
