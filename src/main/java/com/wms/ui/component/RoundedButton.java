package com.wms.ui.component;

import com.wms.ui.UITheme;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class RoundedButton extends JButton {

    public enum Style { PRIMARY, SECONDARY, DANGER, ICON, ICON_DARK }

    private final Style style;
    private Color bg;
    private Color bgHover;
    private Color fg;
    private Color border;
    private int arc = 8;
    private boolean active = false;

    public RoundedButton(String text, Style style) {
        super(text);
        this.style = style;
        setFont(UITheme.FONT_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setHorizontalAlignment(SwingConstants.CENTER);
        applyStyle();
        setBorder(javax.swing.BorderFactory.createEmptyBorder(9, 18, 9, 18));
    }

    private void applyStyle() {
        switch (style) {
            case PRIMARY -> {
                bg = UITheme.ACCENT; bgHover = UITheme.ACCENT_HOVER; fg = Color.WHITE; border = UITheme.ACCENT;
            }
            case SECONDARY -> {
                bg = Color.WHITE; bgHover = UITheme.BG; fg = UITheme.TEXT_MUTED; border = UITheme.BORDER;
                setFont(UITheme.FONT_BASE);
            }
            case DANGER -> {
                bg = UITheme.DANGER_LIGHT; bgHover = UITheme.DANGER; fg = UITheme.DANGER; border = UITheme.DANGER_LIGHT;
            }
            case ICON -> {
                bg = Color.WHITE; bgHover = UITheme.ACCENT_LIGHT; fg = UITheme.TEXT; border = UITheme.BORDER;
                arc = 6;
                setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
            }
            case ICON_DARK -> {
                // Tombol ikon transparan untuk dipakai di atas latar gelap (mis. sidebar).
                bg = new Color(255, 255, 255, 0); bgHover = new Color(255, 255, 255, 30);
                fg = UITheme.SIDEBAR_TEXT; border = null;
                arc = 6;
                setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
            }
        }
    }

    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    public boolean isActive() {
        return active;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean hover = getModel().isRollover();

        Color paintBg;
        Color paintFg;
        boolean drawBorder;
        if (active) {
            paintBg = UITheme.ACCENT;
            paintFg = Color.WHITE;
            drawBorder = false;
        } else {
            paintBg = hover ? bgHover : bg;
            paintFg = fg;
            drawBorder = (style == Style.SECONDARY || style == Style.ICON);
        }

        g2.setColor(paintBg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        if (drawBorder) {
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        g2.dispose();
        setForeground(active ? Color.WHITE : (hover && style == Style.DANGER ? Color.WHITE : paintFg));
        super.paintComponent(g);
    }
}
