package com.wms.ui.panel;

import com.wms.ui.component.RoundedButton;

import javax.swing.AbstractCellEditor;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.FlowLayout;

public abstract class AbstractCellEditorPanel extends AbstractCellEditor implements TableCellEditor {

    private final JPanel panel;
    private int currentId;

    protected AbstractCellEditorPanel() {
        panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        panel.setOpaque(true);

        RoundedButton editBtn = new RoundedButton("\u270E", RoundedButton.Style.ICON);
        editBtn.setPreferredSize(new java.awt.Dimension(28, 28));
        editBtn.addActionListener(e -> {
            fireEditingStopped();
            onEdit(currentId);
        });

        RoundedButton delBtn = new RoundedButton("\uD83D\uDDD1", RoundedButton.Style.ICON);
        delBtn.setPreferredSize(new java.awt.Dimension(28, 28));
        delBtn.addActionListener(e -> {
            fireEditingStopped();
            onDelete(currentId);
        });

        panel.add(editBtn);
        panel.add(delBtn);
    }

    protected abstract void onEdit(int id);

    protected abstract void onDelete(int id);

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        currentId = value == null ? -1 : (Integer) value;
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return currentId;
    }
}
