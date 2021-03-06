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

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jwhere.gui.CatalogTask;
import com.io7m.jwhere.gui.ControllerType;
import net.java.dev.designgridlayout.DesignGridLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.Component;
import java.util.Objects;

final class TasksTab extends JPanel
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(TasksTab.class);
  }

  TasksTab(final ControllerType in_controller)
  {
    super();
    Objects.requireNonNull(in_controller, "in_controller");

    /*
     * Task list.
     */

    final var tasks = new JList<>(in_controller.catalogGetTasksListModel());
    final var cancel = new JButton("Cancel");
    cancel.setEnabled(false);
    cancel.addActionListener((e) -> {
      final var task = tasks.getSelectedValue();
      if (task != null) {
        LOG.debug("cancelling: {}");
        task.getFuture().cancel(true);
      }
    });

    tasks.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tasks.setCellRenderer(new TasksCellRenderer());
    tasks.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        final var task = tasks.getSelectedValue();
        if (task != null) {
          LOG.debug("selected: {}");
          cancel.setEnabled(true);
        } else {
          cancel.setEnabled(false);
        }
      }
    });
    final var tasks_pane = new JScrollPane(tasks);

    final var dg = new DesignGridLayout(this);
    dg.row().left().add(new JLabel("Running Tasks"));
    dg.row().grid().add(tasks_pane);
    dg.row().right().add(cancel);
  }

  private static final class TasksCellRenderer extends DefaultListCellRenderer
  {
    TasksCellRenderer()
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
      super.getListCellRendererComponent(list, value, index, is_selected, has_focus);

      Preconditions.checkPreconditionV(
        value instanceof CatalogTask,
        "value instanceof CatalogTask");
      final var task = (CatalogTask) value;

      this.setText(task.getName());
      return this;
    }
  }
}
