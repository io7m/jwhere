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
import java.util.Objects;

import com.io7m.jwhere.core.CatalogDiskID;
import com.io7m.jwhere.core.CatalogDiskName;
import com.io7m.jwhere.gui.ControllerType;
import net.java.dev.designgridlayout.DesignGridLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

final class CatalogAddDiskDialog extends JDialog
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CatalogAddDiskDialog.class);
  }

  CatalogAddDiskDialog(
    final JFrame parent,
    final ControllerType controller,
    final StatusBar status)
  {
    super(Objects.requireNonNull(parent, "parent"));
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(controller, "controller");

    this.setModalityType(ModalityType.APPLICATION_MODAL);

    final JTextField disk_name = new JTextField();

    final BigIntegerTextField disk_id = new BigIntegerTextField();
    disk_id.setText(controller.catalogGetFreshDiskID().toString());

    final JTextField disk_root = new JTextField(32);

    final JButton disk_root_open = new JButton("Open...");
    disk_root_open.addActionListener(
      e -> {
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        final int r = chooser.showOpenDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
          disk_root.setText(chooser.getSelectedFile().toString());
        }
      });

    final JButton cancel = new JButton("Cancel");
    cancel.addActionListener(e -> WindowUtilities.closeDialog(this));

    final JButton add = new JButton("Add");
    add.addActionListener(
      e -> {
        final Optional<BigInteger> id_opt = disk_id.getBigInteger();
        if (id_opt.isPresent()) {
          final CatalogDiskID new_id = CatalogDiskID.of(id_opt.get());
          final CatalogDiskName new_name =
            CatalogDiskName.of(disk_name.getText());
          final Path new_path = Paths.get(disk_root.getText());

          final ProcedureType<Optional<Throwable>> on_finish_io = ex_opt -> {
            if (ex_opt.isPresent()) {
              final Throwable ex = ex_opt.get();
              status.onErrorLater("Adding disk failed!");
              status.onProgressIndeterminateFinishLater();
              CatalogAddDiskDialog.LOG.error(
                "Failed to add disk: ", ex);
              ErrorBox.showErrorLater(ex);
            } else {
              status.onInfoLater("Added disk.");
              status.onProgressIndeterminateFinishLater();
            }
          };

          final Runnable on_start_io = () -> {
            status.onProgressIndeterminateStartLater();
            status.onInfoLater("Adding disk...");
          };

          controller.catalogAddDisk(
            new_name, new_id, new_path, on_start_io, on_finish_io);
          WindowUtilities.closeDialog(this);
        }
      });

    final DesignGridLayout dg = new DesignGridLayout(this.getContentPane());
    dg.row().grid(new JLabel("Disk Name")).add(disk_name);
    dg.row().grid(new JLabel("Disk ID")).add(disk_id);
    dg.row().grid(new JLabel("Root Directory")).add(disk_root, 3).add(
      disk_root_open);
    dg.row().right().add(cancel).add(add);

    this.getRootPane().setDefaultButton(add);
  }
}
