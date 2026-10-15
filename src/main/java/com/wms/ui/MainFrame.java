package com.wms.ui;

import com.wms.dao.InboundDAO;
import com.wms.dao.OutboundDAO;
import com.wms.dao.StockOpnameDAO;
import com.wms.ui.component.RoundedButton;
import com.wms.ui.component.SidebarPanel;
import com.wms.ui.panel.ApprovalPanel;
import com.wms.ui.panel.BarangPanel;
import com.wms.ui.panel.CreateRequestPanel;
import com.wms.ui.panel.CustomerPanel;
import com.wms.ui.panel.DashboardPanel;
import com.wms.ui.panel.InboundPanel;
import com.wms.ui.panel.KategoriPanel;
import com.wms.ui.panel.LaporanAktivitasPanel;
import com.wms.ui.panel.LokasiRakPanel;
import com.wms.ui.panel.OutboundPanel;
import com.wms.ui.panel.RefreshablePanel;
import com.wms.ui.panel.SupplierPanel;
import com.wms.ui.panel.UserPanel;
import com.wms.ui.panel.StockPanel;
import com.wms.ui.panel.StockOpnamePanel;
import com.wms.util.Permissions;
import com.wms.util.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Jendela utama aplikasi. Punya 3 layar level-atas yang bisa dipilih dari
 * sidebar: Master Data (dengan sub-tab Barang/Kategori/dst), Inbound, dan
 * Outbound. Menu sidebar lain (Dashboard, Create Request, Approval) masih
 * berupa placeholder karena belum dibangun.
 */
public class MainFrame extends JFrame {

    // ---- Screen level (Master Data / Inbound / Outbound) ----
    private final CardLayout screenLayout = new CardLayout();
    private final JPanel screensPanel = new JPanel(screenLayout);
    private final Map<String, ScreenInfo> screens = new LinkedHashMap<>();

    // ---- Sub-tab level (khusus di dalam layar Master Data) ----
    private final CardLayout masterDataCardLayout = new CardLayout();
    private final JPanel masterDataCardsPanel = new JPanel(masterDataCardLayout);
    private final Map<String, RoundedButton> masterDataTabButtons = new LinkedHashMap<>();
    private final Map<String, JComponent> masterDataTabPanels = new LinkedHashMap<>();
    private final JPanel masterDataTabsBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

    private JLabel topbarTitle;
    private JLabel topbarSubtitle;
    private com.wms.ui.component.SidebarPanel sidebar;
    private JPanel root;
    private RoundedButton sidebarToggleBtn;
    private final Runnable onLogout;

    private final InboundDAO inboundDAO = new InboundDAO();
    private final OutboundDAO outboundDAO = new OutboundDAO();
    private final StockOpnameDAO stockOpnameDAO = new StockOpnameDAO();
    private javax.swing.Timer badgeTimer;

    private record ScreenInfo(String title, String subtitle) {
    }

    /** onLogout dipanggil setelah user memilih Logout dan sesi sudah dibersihkan;
     *  dipakai Main.java untuk menutup jendela ini dan menampilkan LoginDialog lagi. */
    public MainFrame(Runnable onLogout) {
        super("WMS - Master Data");
        this.onLogout = onLogout;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setLocationRelativeTo(null);

        root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG);

        sidebar = new SidebarPanel(this::onSidebarClicked, this::toggleSidebar);

        // Terapkan hak akses berdasarkan role user yang sedang login
        for (String menu : new String[]{
                "Dashboard",
                "Master Data",
                "Inbound",
                "Outbound",
                "Stock",
                "Stock Opname",
                "Create Request",
                "Approval",
                "Aktivitas Kerja"
        }) {
            sidebar.setMenuVisible(
                    menu,
                    Permissions.canAccess(menu));
        }

        root.add(sidebar, BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(UITheme.BG);
        main.add(buildTopbar(), BorderLayout.NORTH);
        main.add(buildScreens(), BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);

        setContentPane(root);
        sidebarToggleBtn.setVisible(!sidebar.isVisible());
        showScreen("dashboard");

        // Badge "menunggu konfirmasi" hanya berarti buat Administrator,
        // karena cuma dia yang bisa menindaklanjutinya (lihat
        // Permissions.canConfirmTransaction()). Poll berkala tiap 5 detik
        // supaya angkanya tetap update walau admin sedang buka layar lain.
        if (Permissions.canConfirmTransaction()) {
            refreshBadgeCounts();
            badgeTimer = new javax.swing.Timer(5000, e -> refreshBadgeCounts());
            badgeTimer.start();
        }
    }

    /** Update badge angka "menunggu konfirmasi" di sidebar (Inbound/Outbound/Stock Opname). */
    private void refreshBadgeCounts() {
        try {
            sidebar.setBadge("Inbound", inboundDAO.countDraft());
            sidebar.setBadge("Outbound", outboundDAO.countDraft());
            sidebar.setBadge("Stock Opname", stockOpnameDAO.countDraft());
        } catch (java.sql.SQLException ex) {
            System.err.println("Gagal memuat badge notifikasi: " + ex.getMessage());
        }
    }

    private JPanel buildTopbar() {
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Color.WHITE);
        topbar.setBorder(new MatteBorder(0, 0, 1, 0, UITheme.BORDER));
        topbar.setPreferredSize(new Dimension(0, 64));

        sidebarToggleBtn = new RoundedButton("", RoundedButton.Style.ICON);
        sidebarToggleBtn.setIcon(new com.wms.ui.component.HamburgerIcon(16));
        sidebarToggleBtn.setToolTipText("Buka/tutup sidebar");
        sidebarToggleBtn.setPreferredSize(new Dimension(38, 38));
        sidebarToggleBtn.addActionListener(e -> toggleSidebar());

        JPanel leftBox = new JPanel(new BorderLayout());
        leftBox.setOpaque(false);

        JPanel toggleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        toggleWrap.setOpaque(false);
        toggleWrap.add(sidebarToggleBtn);
        leftBox.add(toggleWrap, BorderLayout.WEST);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setBorder(new EmptyBorder(0, 16, 0, 0));
        topbarTitle = new JLabel("Master Data");
        topbarTitle.setFont(UITheme.FONT_TITLE);
        topbarSubtitle = new JLabel("Kelola data referensi sistem gudang");
        topbarSubtitle.setFont(UITheme.FONT_SUBTITLE);
        topbarSubtitle.setForeground(UITheme.TEXT_MUTED);
        titleBox.add(Box.createVerticalGlue());
        titleBox.add(topbarTitle);
        titleBox.add(topbarSubtitle);
        titleBox.add(Box.createVerticalGlue());

        JLabel userLabel = new JLabel(
                Session.getDisplayName() == null ? "-" : Session.getDisplayName(),
                new UserAvatarIcon(28),
                SwingConstants.LEFT);
        userLabel.setFont(UITheme.FONT_BASE);
        userLabel.setForeground(UITheme.TEXT_MUTED);
        userLabel.setIconTextGap(8);
        userLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 12));
        userLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userLabel.setOpaque(false);

        JPopupMenu userMenu = new JPopupMenu();
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> doLogout());
        userMenu.add(logoutItem);

        userLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                userMenu.show(userLabel, 0, userLabel.getHeight());
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                userLabel.setOpaque(true);
                userLabel.setBackground(UITheme.BG);
                userLabel.repaint();
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                userLabel.setOpaque(false);
                userLabel.repaint();
            }
        });

        JPanel userBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        userBox.setOpaque(false);
        userBox.setBorder(new EmptyBorder(0, 0, 0, 20));
        userBox.add(userLabel);

        leftBox.add(titleBox, BorderLayout.CENTER);
        topbar.add(leftBox, BorderLayout.WEST);
        topbar.add(userBox, BorderLayout.EAST);
        return topbar;
    }

    /** Ikon avatar bulat sederhana (inisial nama) untuk label user di topbar. */
    private static class UserAvatarIcon implements Icon {
        private final int size;
        UserAvatarIcon(int size) { this.size = size; }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UITheme.ACCENT_LIGHT);
            g2.fillOval(x, y, size, size);
            String name = Session.getDisplayName();
            String initial = (name == null || name.isBlank()) ? "?" : name.trim().substring(0, 1).toUpperCase();
            g2.setColor(UITheme.ACCENT);
            g2.setFont(UITheme.FONT_LABEL.deriveFont(Font.BOLD, size * 0.5f));
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (size - fm.stringWidth(initial)) / 2;
            int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(initial, tx, ty);
            g2.dispose();
        }
    }

    /**
     * Logout: minta konfirmasi, catat audit log dengan user yang MASIH login
     * (sebelum session dibersihkan), lalu bersihkan Session, tutup jendela
     * ini, dan panggil callback onLogout supaya Main.java menampilkan
     * LoginDialog lagi tanpa perlu restart aplikasi/JVM.
     */
    private void doLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Keluar dari akun " + Session.getDisplayName() + "?\n" +
                "Anda bisa login kembali dengan user yang sama atau user lain.",
                "Konfirmasi Logout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (badgeTimer != null) {
            badgeTimer.stop();
        }

        com.wms.util.AuditLogger.log("LOGOUT", "LOGOUT", null,
                "User " + Session.getUsername() + " logout dari aplikasi.");
        Session.logout();
        dispose();
        if (onLogout != null) onLogout.run();
    }

    /**
     * Buka/tutup sidebar. Saat ditutup, sidebar.setVisible(false) membuat
     * BorderLayout mengabaikannya sepenuhnya (tidak dihitung sebagai ruang
     * kosong), sehingga area konten di CENTER otomatis melebar mengisi
     * seluruh jendela tanpa perlu menghitung ukuran secara manual.
     * <p>
     * Tombol toggle di topbar (sidebarToggleBtn) hanya ditampilkan saat
     * sidebar TERTUTUP - satu-satunya gunanya memang untuk membuka lagi.
     * Saat sidebar terbuka, tombol toggle yang dipakai adalah yang ada di
     * dalam header sidebar itu sendiri (lihat SidebarPanel), supaya tidak
     * ada 2 tombol toggle yang tampil bersamaan.
     */
    private void toggleSidebar() {
        boolean willBeVisible = !sidebar.isVisible();
        sidebar.setVisible(willBeVisible);
        sidebarToggleBtn.setVisible(!willBeVisible);
        root.revalidate();
        root.repaint();
    }

    private JPanel buildScreens() {
        screensPanel.setOpaque(false);

        addScreen("dashboard", "Dashboard", "Ringkasan aktivitas gudang", new DashboardPanel());
        addScreen("masterdata", "Master Data", "Kelola data referensi sistem gudang", buildMasterDataScreen());
        addScreen("inbound", "Inbound", "Kelola transaksi penerimaan barang dari supplier", new InboundPanel());
        addScreen("outbound", "Outbound", "Kelola transaksi pengiriman barang ke customer", new OutboundPanel());
        addScreen("request", "Create Request", "Ajukan permintaan pindah lokasi stok, barang baru, atau lainnya", new CreateRequestPanel());
        addScreen("approval", "Approval", "Setujui atau tolak permintaan yang masuk", new ApprovalPanel());
        addScreen(
            "stock",
            "Stok",
            "Lihat stok real per lokasi rak",
            new StockPanel());
        addScreen(
                "stockopname",
                "Stock Opname",
                "Cycle count dan penyesuaian stok",
                new StockOpnamePanel());
        addScreen(
                "laporanaktivitas",
                "Aktivitas Kerja",
                "Laporan aktivitas kerja per user (khusus Administrator)",
                new LaporanAktivitasPanel());
                return screensPanel;
    }

    private void addScreen(String key, String title, String subtitle, JComponent content) {
        screens.put(key, new ScreenInfo(title, subtitle));
        screensPanel.add(content, key);
    }

    private JScrollPane buildMasterDataScreen() {
        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(UITheme.BG);
        content.setBorder(new EmptyBorder(24, 28, 24, 28));

        masterDataTabsBar.setOpaque(false);
        addMasterDataTab("barang", "Barang", new BarangPanel());
        addMasterDataTab("kategori", "Kategori", new KategoriPanel());
        addMasterDataTab("supplier", "Supplier", new SupplierPanel());
        addMasterDataTab("customer", "Customer", new CustomerPanel());
        addMasterDataTab("lokasi", "Lokasi Rak", new LokasiRakPanel());
        addMasterDataTab("user", "User", new UserPanel());

        content.add(masterDataTabsBar, BorderLayout.NORTH);
        content.add(masterDataCardsPanel, BorderLayout.CENTER);

        showMasterDataTab("barang");

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBackground(UITheme.BG);
        scroll.getViewport().setBackground(UITheme.BG);
        return scroll;
    }

    private void addMasterDataTab(String key, String label, JComponent panel) {
        RoundedButton btn = new RoundedButton(label, RoundedButton.Style.SECONDARY);
        btn.addActionListener(e -> showMasterDataTab(key));
        masterDataTabButtons.put(key, btn);
        masterDataTabPanels.put(key, panel);
        masterDataTabsBar.add(btn);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(panel, BorderLayout.CENTER);
        masterDataCardsPanel.add(wrapper, key);
        masterDataCardsPanel.setOpaque(false);
    }

    private void showMasterDataTab(String key) {
        masterDataCardLayout.show(masterDataCardsPanel, key);
        masterDataTabButtons.forEach((k, btn) -> btn.setActive(k.equals(key)));
        // Ambil ulang data dari database setiap kali tab dibuka, supaya perubahan
        // yang dilakukan dari tab lain (mis. kategori baru) langsung terlihat.
        JComponent panel = masterDataTabPanels.get(key);
        if (panel instanceof RefreshablePanel refreshable) {
            refreshable.refresh();
        }
    }

    private void showScreen(String key) {
        ScreenInfo info = screens.get(key);
        if (info == null) return;
        topbarTitle.setText(info.title());
        topbarSubtitle.setText(info.subtitle());
        screenLayout.show(screensPanel, key);
        sidebar.setActiveMenu(info.title());

        if ("masterdata".equals(key)) {
            // Refresh tab yang sedang aktif di Master Data.
            masterDataTabButtons.keySet().stream()
                    .filter(k -> masterDataTabButtons.get(k).isActive())
                    .findFirst()
                    .ifPresent(this::showMasterDataTab);
        } else {
            Component current = getVisibleScreenComponent();
            if (current instanceof RefreshablePanel refreshable) {
                refreshable.refresh();
            }
        }
    }

    private Component getVisibleScreenComponent() {
        for (Component c : screensPanel.getComponents()) {
            if (c.isVisible()) return c;
        }
        return null;
    }

    private void onSidebarClicked(String menu) {
        switch (menu) {
            case "Dashboard" -> showScreen("dashboard");
            case "Master Data" -> showScreen("masterdata");
            case "Inbound" -> showScreen("inbound");
            case "Outbound" -> showScreen("outbound");
            case "Stock" -> showScreen("stock");
            case "Stock Opname" -> showScreen("stockopname");
            case "Create Request" -> showScreen("request");
            case "Approval" -> showScreen("approval");
            case "Aktivitas Kerja" -> showScreen("laporanaktivitas");
            default -> JOptionPane.showMessageDialog(this,
                    "Modul \"" + menu + "\" belum dibangun pada tahap ini.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    
}