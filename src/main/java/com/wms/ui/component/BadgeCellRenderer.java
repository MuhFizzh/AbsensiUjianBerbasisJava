package com.wms.ui.component;

import com.wms.ui.UITheme;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

public class BadgeCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                     boolean hasFocus, int row, int column) {
        String status = value == null ? "" : value.toString();
        boolean active = "AKTIF".equalsIgnoreCase(status);
        BadgeLabel badge = new BadgeLabel(active ? "Aktif" : "Nonaktif", active);
        badge.setOpaque(false);
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        panel.setOpaque(true);
        panel.setBackground(isSelected ? table.getSelectionBackground() : UITheme.CARD);
        panel.add(badge);
        return panel;
    }
}
