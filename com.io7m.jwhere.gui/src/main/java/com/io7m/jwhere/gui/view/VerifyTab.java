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

import com.io7m.jfunctional.ProcedureType;
import com.io7m.jwhere.core.CatalogDiskMetadata;
import com.io7m.jwhere.gui.ControllerType;
import net.java.dev.designgridlayout.DesignGridLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.Component;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

final class VerifyTab extends JPanel
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(VerifyTab.class);
  }

  private final ControllerType controller;

  VerifyTab(
    final ControllerType in_controller,
    final StatusBar status)
  {
    super();
    this.controller = Objects.requireNonNull(in_controller, "in_controller");
    Objects.requireNonNull(status, "status");

    final var mount = new JTextField(32);
    final var table = new CatalogVerificationTable(
      this.controller.catalogGetVerificationTableModel());

    final var table_scroller = new JScrollPane(table);

    final var disk_menu = new JComboBox<CatalogDiskMetadata>();
    disk_menu.setModel(in_controller.catalogGetComboBoxModel());
    disk_menu.setRenderer(new DiskMenuCellRenderer());

    final var select = new JButton("Select...");
    final var verify = new JButton("Verify");
    verify.setEnabled(false);

    disk_menu.addActionListener(
      e -> {
        final var selected =
          (CatalogDiskMetadata) disk_menu.getSelectedItem();
        if (selected != null) {
          verify.setEnabled(true);
        } else {
          verify.setEnabled(false);
        }
      });

    verify.addActionListener(
      e -> {
        final var selected = Objects.requireNonNull(
          (CatalogDiskMetadata) disk_menu.getSelectedItem(),
          "(CatalogDiskMetadata) disk_menu.getSelectedItem()");

        final var id = selected.getDiskID();
        final var path = Paths.get(mount.getText());
        final Runnable on_start_io = () -> {
          status.onProgressIndeterminateStartLater();
          status.onInfoLater("Verifying disk...");
        };

        final ProcedureType<Optional<Throwable>> on_finish_io = ex_opt -> {
          if (ex_opt.isPresent()) {
            final var ex = ex_opt.get();
            LOG.error("verification failed: ", ex);
            status.onErrorLater("Verification failed!");
            status.onProgressIndeterminateFinishLater();
            ErrorBox.showErrorLater(ex);
          } else {
            status.onInfoLater("Verification completed");
            status.onProgressIndeterminateFinishLater();
          }
        };

        this.controller.catalogVerifyDisk(id, path, on_start_io, on_finish_io);
      });

    final var dg = new DesignGridLayout(this);
    dg.row().grid(new JLabel("Mount")).add(mount, 3).add(select);
    dg.row().grid(new JLabel("Disk")).add(disk_menu, 3).add(verify);
    dg.row().grid().add(table_scroller);
  }

  private static final class DiskMenuCellRenderer extends DefaultListCellRenderer
  {
    DiskMenuCellRenderer()
    {

    }

    @Override
    public Component getListCellRendererComponent(
      final JList<?> list,
      final Object value,
      final int index,
      final boolean is_selected,
      final boolean has_focus)
    {
      super.getListCellRendererComponent(
        list, value, index, is_selected, has_focus);

      if (value instanceof CatalogDiskMetadata) {
        final var meta = (CatalogDiskMetadata) value;
        this.setIcon(CatalogDiskIcons.getIconForDisk(meta));
        this.setText(meta.getDiskName().value());
      }

      return this;
    }
  }
}
