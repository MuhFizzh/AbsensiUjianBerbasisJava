package com.wms.ui.component;

import com.wms.ui.UITheme;

import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class PlaceholderTextField extends JTextField {

    private String placeholder;

    public PlaceholderTextField(String placeholder) {
        this.placeholder = placeholder;
        setFont(UITheme.FONT_BASE);
        setOpaque(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(9, 30, 9, 12));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        g2.setColor(UITheme.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        g2.setColor(UITheme.TEXT_MUTED);
        g2.setFont(getFont());
        g2.drawString("\uD83D\uDD0D", 9, getHeight() / 2 + 5);
        g2.dispose();
        super.paintComponent(g);
        if (getText().isEmpty() && placeholder != null) {
            Graphics2D pg = (Graphics2D) g.create();
            pg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pg.setColor(UITheme.TEXT_MUTED);
            pg.setFont(getFont());
            pg.drawString(placeholder, 30, getHeight() / 2 + 5);
            pg.dispose();
        }
    }
}
