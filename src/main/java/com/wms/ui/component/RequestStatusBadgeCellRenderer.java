package com.wms.ui.component;

import com.wms.ui.UITheme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Renderer kolom Status pada tabel Permintaan: MENUNGGU / DISETUJUI / DITOLAK. */
public class RequestStatusBadgeCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                     boolean hasFocus, int row, int column) {
        String status = value == null ? "" : value.toString();
        String label;
        Color bg;
        Color fg;
        switch (status) {
            case "DISETUJUI" -> { label = "Disetujui"; bg = UITheme.SUCCESS_LIGHT; fg = UITheme.SUCCESS; }
            case "DITOLAK"   -> { label = "Ditolak";   bg = UITheme.DANGER_LIGHT;  fg = UITheme.DANGER;  }
            default          -> { label = "Menunggu";  bg = UITheme.WARNING_LIGHT; fg = UITheme.WARNING; }
        }

        Badge badge = new Badge(label, bg, fg);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(true);
        panel.setBackground(isSelected ? table.getSelectionBackground() : UITheme.CARD);
        panel.add(badge);
        return panel;
    }

    /** Badge kecil bulat sekali pakai, dipakai internal oleh renderer ini saja. */
    private static class Badge extends JLabel {
        private final Color bg;

        Badge(String text, Color bg, Color fg) {
            super(text, SwingConstants.CENTER);
            this.bg = bg;
            setFont(UITheme.FONT_BOLD);
            setForeground(fg);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int w = fm.stringWidth(getText()) + 24;
            int h = fm.getHeight() + 8;
            return new Dimension(w, h);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
