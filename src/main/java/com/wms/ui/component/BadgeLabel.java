package com.wms.ui.component;

import com.wms.ui.UITheme;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class BadgeLabel extends JLabel {

    private Color bg;
    private Color fg;

    public BadgeLabel(String text, boolean active) {
        super(text, SwingConstants.CENTER);
        setFont(UITheme.FONT_BOLD);
        setOpaque(false);
        setActive(active);
    }

    public void setActive(boolean active) {
        bg = active ? UITheme.SUCCESS_LIGHT : UITheme.DANGER_LIGHT;
        fg = active ? UITheme.SUCCESS : UITheme.DANGER;
        setForeground(fg);
        repaint();
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
