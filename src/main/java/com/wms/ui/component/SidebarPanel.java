package com.wms.ui.component;

import com.wms.ui.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SidebarPanel extends JPanel {

    private static final int WIDTH = 230;

    private final Map<String, NavItem> navItems = new LinkedHashMap<>();
    private String activeMenu = "Master Data";

    public SidebarPanel(Consumer<String> onMenuSelected, Runnable onToggle) {
        // Sebelumnya pakai setLayout(null) + setBounds() manual untuk tiap item.
        // Itu bisa memicu bug repaint (komponen lain "bocor" tergambar di area
        // sidebar saat hover) karena Swing tidak mengelola posisi/area kotornya
        // sendiri. BoxLayout membiarkan Swing yang menghitung ulang bounds dan
        // dirty-region repaint secara benar setiap saat.
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UITheme.NAVY);
        setPreferredSize(new Dimension(WIDTH, 0));
        setMaximumSize(new Dimension(WIDTH, Integer.MAX_VALUE));

        JPanel brand = new JPanel();
        brand.setLayout(new BorderLayout());
        brand.setBackground(UITheme.NAVY);
        brand.setBorder(new MatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 20)));
        brand.setPreferredSize(new Dimension(WIDTH, 64));
        brand.setMaximumSize(new Dimension(WIDTH, 64));
        brand.setAlignmentX(LEFT_ALIGNMENT);

        JPanel brandLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandLeft.setOpaque(false);

        JLabel icon = new JLabel("W", SwingConstants.CENTER);
        icon.setForeground(UITheme.SIDEBAR_TEXT);
        icon.setFont(new Font("Segoe UI", Font.BOLD, 13));
        icon.setPreferredSize(new Dimension(32, 32));
        icon.setBorder(BorderFactory.createLineBorder(UITheme.SIDEBAR_TEXT, 2, true));

        JPanel brandText = new JPanel();
        brandText.setLayout(new java.awt.GridLayout(2, 1));
        brandText.setOpaque(false);
        JLabel brandName = new JLabel("WMS");
        brandName.setForeground(Color.WHITE);
        brandName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        JLabel brandSub = new JLabel("Warehouse Management");
        brandSub.setForeground(UITheme.SIDEBAR_TEXT_MUTED);
        brandSub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        brandText.add(brandName);
        brandText.add(brandSub);

        brandLeft.add(icon);
        brandLeft.add(brandText);
        brand.add(brandLeft, BorderLayout.CENTER);

        RoundedButton toggleBtn = new RoundedButton("", RoundedButton.Style.ICON_DARK);
        toggleBtn.setIcon(new HamburgerIcon(15));
        toggleBtn.setToolTipText("Tutup sidebar");
        toggleBtn.setPreferredSize(new Dimension(30, 30));
        toggleBtn.addActionListener(e -> onToggle.run());
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        togglePanel.setOpaque(false);
        togglePanel.add(toggleBtn);
        brand.add(togglePanel, BorderLayout.EAST);

        add(brand);

        addNavItem("Dashboard", onMenuSelected);
        addGroupLabel("Master");
        addNavItem("Master Data", onMenuSelected);
        addGroupLabel("Operations");
        addNavItem("Inbound", onMenuSelected);
        addNavItem("Outbound", onMenuSelected);
        addNavItem("Stock", onMenuSelected);
        addNavItem("Stock Opname", onMenuSelected);
        addGroupLabel("Request");
        addNavItem("Create Request", onMenuSelected);
        addNavItem("Approval", onMenuSelected);
        addGroupLabel("Laporan");
        addNavItem("Aktivitas Kerja", onMenuSelected);

        // Mengisi sisa ruang vertikal supaya sidebar tetap penuh sampai bawah
        // walau jumlah menu sedikit, tanpa perlu menghitung tinggi manual.
        add(Box.createVerticalGlue());

        setActiveMenu("Master Data");
    }

    /** Memindahkan highlight ke menu tertentu (dipanggil dari MainFrame saat berpindah layar). */
    public void setActiveMenu(String menu) {
        this.activeMenu = menu;
        navItems.forEach((name, item) -> item.setActive(name.equals(menu)));
        repaint();
    }

    public void setMenuVisible(
            String menu,
            boolean visible) {

        NavItem item =
                navItems.get(menu);

        if (item != null) {

            item.item.setVisible(
                    visible);

            item.item.setMaximumSize(
                    new Dimension(
                            WIDTH,
                            visible ? 38 : 0));

            revalidate();
            repaint();
        }
    }

    /**
     * Menampilkan/menyembunyikan badge angka di samping sebuah menu
     * (mis. jumlah transaksi DRAFT yang menunggu konfirmasi admin).
     * count null atau <= 0 -> badge disembunyikan.
     */
    public void setBadge(String menu, Integer count) {

        NavItem item = navItems.get(menu);
        if (item == null) {
            return;
        }

        if (count == null || count <= 0) {
            item.badge.setVisible(false);
        } else {
            item.badge.setText(String.valueOf(count));
            item.badge.setVisible(true);
        }

        item.item.revalidate();
        item.item.repaint();
    }
    
    private void addGroupLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setForeground(UITheme.SIDEBAR_TEXT_MUTED);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setBorder(new EmptyBorder(12, 18, 4, 0));
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(WIDTH, 34));
        add(label);
    }

    private void addNavItem(String text, Consumer<String> onMenuSelected) {
        JPanel item = new JPanel(new BorderLayout());
        item.setPreferredSize(new Dimension(WIDTH, 38));
        item.setMaximumSize(new Dimension(WIDTH, 38));
        item.setAlignmentX(LEFT_ALIGNMENT);

        JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        inner.setOpaque(false);
        JLabel dot = new JLabel("\u25CF");
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 6));

        JLabel label = new JLabel(text);

        inner.add(dot);
        inner.add(label);
        item.add(inner, BorderLayout.WEST);

        JLabel badge = new JLabel("0", SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(new Color(0xD8, 0x5A, 0x30));
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setBorder(new EmptyBorder(1, 6, 1, 6));
        badge.setVisible(false);
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);
        item.add(badgeWrap, BorderLayout.EAST);

        NavItem navItem = new NavItem(item, dot, label, badge);
        navItems.put(text, navItem);

        item.setCursor(new Cursor(Cursor.HAND_CURSOR));
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onMenuSelected.accept(text);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!text.equals(activeMenu)) {
                    item.setBackground(new Color(255, 255, 255, 13));
                    SidebarPanel.this.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!text.equals(activeMenu)) {
                    item.setBackground(UITheme.NAVY);
                    SidebarPanel.this.repaint();
                }
            }
        });

        add(item);
    }

    /** Menyimpan referensi komponen 1 item navigasi supaya bisa diubah aktif/tidaknya. */
    private static class NavItem {
        private final JPanel item;
        private final JLabel dot;
        private final JLabel label;
        private final JLabel badge;

        NavItem(JPanel item, JLabel dot, JLabel label, JLabel badge) {
            this.item = item;
            this.dot = dot;
            this.label = label;
            this.badge = badge;
        }

        void setActive(boolean active) {
            item.setBackground(active ? UITheme.NAVY_3 : UITheme.NAVY);
            item.setBorder(active
                    ? new MatteBorder(0, 3, 0, 0, UITheme.ACCENT)
                    : new EmptyBorder(0, 3, 0, 0));
            dot.setForeground(active ? UITheme.ACCENT : new Color(0x5F, 0x75, 0x90));
            label.setForeground(active ? Color.WHITE : UITheme.SIDEBAR_TEXT);
            label.setFont(active ? UITheme.FONT_NAV_BOLD : UITheme.FONT_NAV);
        }
    }
}
