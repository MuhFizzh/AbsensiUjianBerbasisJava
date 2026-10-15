package com.wms.ui.component;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Ikon hamburger (\u2261) digambar manual pakai 3 garis, bukan mengandalkan
 * karakter unicode (\u2630) yang ternyata tidak selalu ada di font UI
 * standar - sebelumnya tampil sebagai "..." di tombol toggle sidebar.
 */
public class HamburgerIcon implements Icon {

    private final int size;

    public HamburgerIcon(int size) {
        this.size = size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(c.getForeground());
        int barHeight = Math.max(2, size / 8);
        int gap = (size - barHeight * 3) / 2;
        for (int i = 0; i < 3; i++) {
            g2.fillRoundRect(x, y + i * (barHeight + gap), size, barHeight, barHeight, barHeight);
        }
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
