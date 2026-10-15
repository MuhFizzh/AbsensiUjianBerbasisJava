package com.wms.ui.panel;

import com.wms.dao.AktivitasKerjaDAO;
import com.wms.dao.InboundDAO;
import com.wms.dao.OutboundDAO;
import com.wms.dao.PermintaanDAO;
import com.wms.dao.StockOpnameDAO;
import com.wms.dao.UserDAO;
import com.wms.model.AktivitasKerja;
import com.wms.model.InboundHeader;
import com.wms.model.InboundItem;
import com.wms.model.OutboundHeader;
import com.wms.model.OutboundItem;
import com.wms.model.Permintaan;
import com.wms.model.StockOpname;
import com.wms.model.StockOpnameDetail;
import com.wms.model.User;
import com.wms.ui.UITheme;
import com.wms.ui.component.PlaceholderTextField;
import com.wms.ui.component.RoundedButton;
import com.wms.ui.component.TableCopyEnabler;
import com.wms.ui.component.TablePagination;
import com.wms.util.SnapshotUtil;
import com.wms.util.ReportExportService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Layar "Laporan Aktivitas Kerja" - khusus Administrator (lihat
 * Permissions#canAccess dan pendaftaran menu di MainFrame/SidebarPanel).
 *
 * Menampilkan riwayat aktivitas dari tabel aktivitas_kerja (dicatat oleh
 * ActivityLogger setiap transaksi Inbound/Outbound/Stock Opname selesai,
 * atau Permintaan disetujui/ditolak), dengan filter user, jenis aktivitas,
 * dan periode tanggal, plus ringkasan jumlah aktivitas/total item/total qty.
 */
public class LaporanAktivitasPanel extends JPanel implements RefreshablePanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final AktivitasKerjaDAO dao = new AktivitasKerjaDAO();
    private final UserDAO userDAO = new UserDAO();
    private final InboundDAO inboundDAO = new InboundDAO();
    private final OutboundDAO outboundDAO = new OutboundDAO();
    private final StockOpnameDAO stockOpnameDAO = new StockOpnameDAO();
    private final PermintaanDAO permintaanDAO = new PermintaanDAO();

    private final JComboBox<String> userCombo = new JComboBox<>();
    private final JComboBox<String> jenisCombo = new JComboBox<>(
            new String[]{"Semua Jenis", "INBOUND", "OUTBOUND", "STOCK_OPNAME", "PERMINTAAN"});
    private final PlaceholderTextField dariField = new PlaceholderTextField("Dari (dd-MM-yyyy)");
    private final PlaceholderTextField sampaiField = new PlaceholderTextField("Sampai (dd-MM-yyyy)");
    private final PlaceholderTextField cekNomorField = new PlaceholderTextField("No. Inbound/Outbound/Opname/Permintaan");

    private final JLabel jumlahAktivitasValue = new JLabel("0");
    private final JLabel totalItemValue = new JLabel("0");
    private final JLabel totalQtyValue = new JLabel("0");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Username", "Jenis", "No. Transaksi", "Jumlah Item", "Total Qty", "Status", "Waktu Selesai"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    /** Baris aktivitas yang sedang ditampilkan di tabel, urutan sejajar dengan tableModel. */
    private List<AktivitasKerja> currentList = new ArrayList<>();

    /** Tabel utama - disimpan sebagai field supaya bisa dipasangi listener klik-2x untuk lihat detail. */
    private JTable table;
    private TablePagination pagination;

    public LaporanAktivitasPanel() {
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(buildCekNomorRow());
        content.add(Box.createVerticalStrut(14));
        content.add(buildFilterRow());
        content.add(Box.createVerticalStrut(14));
        content.add(buildSummaryRow());
        content.add(Box.createVerticalStrut(14));
        content.add(buildTableCard());

        add(content, BorderLayout.CENTER);

        loadUserCombo();
        reload();
    }

    // ------------------------------------------------------------------
    // Cek 1 transaksi berdasarkan nomor (mis. OUT-00015 / IN-00010)
    // ------------------------------------------------------------------
    private JPanel buildCekNomorRow() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JLabel title = new JLabel("Cek Aktivitas per No. Transaksi");
        title.setFont(UITheme.FONT_LABEL);
        title.setForeground(UITheme.TEXT_MUTED);

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        inputRow.setOpaque(false);

        cekNomorField.setPreferredSize(new Dimension(300, 36));
        cekNomorField.addActionListener(e -> cekNomorTransaksi());

        RoundedButton cekBtn = new RoundedButton("Cek", RoundedButton.Style.PRIMARY);
        cekBtn.setPreferredSize(new Dimension(90, 36));
        cekBtn.addActionListener(e -> cekNomorTransaksi());

        inputRow.add(cekNomorField);
        inputRow.add(cekBtn);

        card.add(title, BorderLayout.NORTH);
        card.add(inputRow, BorderLayout.CENTER);
        return card;
    }

    private void cekNomorTransaksi() {
        String nomor = cekNomorField.getText().trim();
        if (nomor.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan nomor transaksi terlebih dahulu.",
                    "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            AktivitasKerja a = dao.findByNomorTransaksi(nomor);
            if (a != null) {
                showDetailDialog(a);
                return;
            }

            // Belum ada di aktivitas_kerja -> cek ke tabel transaksi asli,
            // supaya admin tahu penyebabnya: nomor salah, atau transaksi
            // memang belum selesai diproses (aktivitas baru dicatat saat
            // transaksi SELESAI, bukan saat masih draft).
            String statusAsli = cariStatusAsli(nomor);
            if (statusAsli != null) {
                JOptionPane.showMessageDialog(this,
                        "Transaksi \"" + nomor + "\" ditemukan, tapi statusnya masih \"" + statusAsli + "\".\n" +
                                "Aktivitas baru dicatat setelah transaksi ini selesai diproses\n" +
                                "(diterima/dikirim/diproses/disetujui-ditolak).",
                        "Belum Ada Aktivitas", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Nomor transaksi \"" + nomor + "\" tidak ditemukan di sistem.",
                        "Tidak Ditemukan", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /**
     * Mencari nomor transaksi di tabel asalnya (Inbound/Outbound/Stock
     * Opname/Permintaan) untuk mengetahui statusnya saat ini. Dipakai
     * sebagai fallback ketika belum ada catatan di aktivitas_kerja.
     * Mengembalikan status transaksi, atau null jika nomor sama sekali
     * tidak ditemukan di keempat tabel tersebut.
     */
    private String cariStatusAsli(String nomor) throws SQLException {
        for (InboundHeader h : inboundDAO.findAll(nomor)) {
            if (h.getNomor().equalsIgnoreCase(nomor)) return h.getStatus();
        }
        for (OutboundHeader h : outboundDAO.findAll(nomor)) {
            if (h.getNomor().equalsIgnoreCase(nomor)) return h.getStatus();
        }
        for (StockOpname o : stockOpnameDAO.findAll(nomor)) {
            if (o.getNomor().equalsIgnoreCase(nomor)) return o.getStatus();
        }
        for (Permintaan p : permintaanDAO.findAll(nomor)) {
            if (p.getNomor().equalsIgnoreCase(nomor)) return p.getStatus();
        }
        return null;
    }

    /** Menampilkan detail 1 aktivitas: info ringkasan + daftar barang yang terlibat. */
    private void showDetailDialog(AktivitasKerja a) {
        JPanel info = new JPanel(new GridLayout(0, 2, 12, 6));
        info.setBorder(new EmptyBorder(4, 4, 10, 4));

        info.add(detailLabel("Username"));
        info.add(detailValue(a.getUsername()));
        info.add(detailLabel("Jenis Aktivitas"));
        info.add(detailValue(a.getJenisAktivitas()));
        info.add(detailLabel("No. Transaksi"));
        info.add(detailValue(a.getNomorTransaksi()));
        info.add(detailLabel("Jumlah Item"));
        info.add(detailValue(a.getJumlahItem() + " item"));
        info.add(detailLabel("Total Qty"));
        info.add(detailValue(a.getTotalQty() + " qty"));
        info.add(detailLabel("Status"));
        info.add(detailValue(a.getStatus()));
        info.add(detailLabel("Waktu Selesai"));
        info.add(detailValue(a.getWaktuSelesai() == null ? "-" : a.getWaktuSelesai().format(DATETIME_FMT)));

        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.add(info, BorderLayout.NORTH);

        JLabel barangTitle = new JLabel("Barang yang terlibat");
        barangTitle.setFont(UITheme.FONT_LABEL);
        barangTitle.setForeground(UITheme.TEXT_MUTED);
        barangTitle.setBorder(new EmptyBorder(4, 4, 4, 4));

        JComponent barangDetail = buildBarangDetail(a);

        JPanel barangSection = new JPanel(new BorderLayout());
        barangSection.add(barangTitle, BorderLayout.NORTH);
        barangSection.add(barangDetail, BorderLayout.CENTER);
        wrapper.add(barangSection, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, wrapper,
                "Detail Aktivitas - " + a.getNomorTransaksi(), JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Mengambil & menampilkan daftar barang untuk 1 aktivitas.
     *
     * Prioritas: baca dari detail_snapshot (salinan JSON yang dibuat
     * ActivityLogger persis saat transaksi selesai) - ini yang membuat
     * rincian tetap utuh walau transaksi/barang aslinya belakangan
     * berubah atau terhapus. Kalau snapshot kosong (baris lama, dicatat
     * sebelum fitur ini ada), fallback ke cara lama: JOIN live ke tabel
     * transaksi asli berdasarkan reference_id.
     */
    private JComponent buildBarangDetail(AktivitasKerja a) {
        List<LinkedHashMap<String, String>> snapshotRows =
                SnapshotUtil.parse(a.getDetailSnapshot());

        if (!snapshotRows.isEmpty()) {
            return buildSnapshotTable(snapshotRows, true);
        }

        return buildLiveDetail(a);
    }

    /** Menyusun tabel dari baris snapshot (List of LinkedHashMap, kolom = key baris pertama). */
    private JComponent buildSnapshotTable(List<LinkedHashMap<String, String>> snapshotRows, boolean fromSnapshot) {
        List<String> columnNames = new ArrayList<>(snapshotRows.get(0).keySet());
        String[] columns = columnNames.toArray(new String[0]);

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (LinkedHashMap<String, String> row : snapshotRows) {
            Object[] values = new Object[columnNames.size()];
            for (int i = 0; i < columnNames.size(); i++) {
                values[i] = row.get(columnNames.get(i));
            }
            model.addRow(values);
        }

        JTable table = new JTable(model);
        TableCopyEnabler.enable(table);
        table.setRowHeight(28);
        table.setFont(UITheme.FONT_BASE);

        JPanel wrapper = new JPanel(new BorderLayout(0, 4));

        JLabel sourceLabel = new JLabel(fromSnapshot
                ? "Sumber: snapshot tersimpan (tetap utuh walau transaksi asli berubah/terhapus)"
                : "Sumber: data transaksi saat ini (belum ada snapshot untuk aktivitas ini)");
        sourceLabel.setFont(UITheme.FONT_LABEL.deriveFont(Font.ITALIC, 11f));
        sourceLabel.setForeground(UITheme.TEXT_MUTED);
        sourceLabel.setBorder(new EmptyBorder(0, 4, 4, 4));
        wrapper.add(sourceLabel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(460, Math.min(180, 34 + snapshotRows.size() * 28)));
        wrapper.add(scroll, BorderLayout.CENTER);

        return wrapper;
    }

    /**
     * Fallback lama: rekonstruksi rincian barang dengan JOIN live ke
     * tabel transaksi asli, dipakai hanya untuk baris aktivitas_kerja
     * yang dicatat sebelum kolom detail_snapshot ada.
     */
    private JComponent buildLiveDetail(AktivitasKerja a) {
        try {
            String[] columns;
            List<Object[]> rows = new java.util.ArrayList<>();

            switch (a.getJenisAktivitas()) {
                case "INBOUND" -> {
                    columns = new String[]{"Barang", "Lokasi", "Qty"};
                    for (InboundItem it : inboundDAO.findItems(a.getReferenceId())) {
                        rows.add(new Object[]{it.getBarangNama(), it.getLokasiKode(), it.getQty()});
                    }
                }
                case "OUTBOUND" -> {
                    columns = new String[]{"Barang", "Lokasi", "Qty"};
                    for (OutboundItem it : outboundDAO.findItems(a.getReferenceId())) {
                        rows.add(new Object[]{it.getBarangNama(), it.getLokasiKode(), it.getQty()});
                    }
                }
                case "STOCK_OPNAME" -> {
                    columns = new String[]{"Barang", "Lokasi", "Sistem", "Aktual", "Selisih"};
                    for (StockOpnameDetail it : stockOpnameDAO.findItems(a.getReferenceId())) {
                        rows.add(new Object[]{it.getBarangNama(), it.getLokasiKode(),
                                it.getSystemQty(), it.getActualQty(), it.getAdjustmentQty()});
                    }
                }
                case "PERMINTAAN" -> {
                    columns = new String[]{"Barang", "Jumlah", "Lokasi Asal", "Lokasi Tujuan"};
                    for (Permintaan p : permintaanDAO.findAll(a.getNomorTransaksi())) {
                        if (p.getNomor().equalsIgnoreCase(a.getNomorTransaksi())) {
                            rows.add(new Object[]{
                                    p.getBarangNama() == null ? "-" : p.getBarangNama(),
                                    p.getJumlah() == null ? "-" : p.getJumlah(),
                                    p.getLokasiAsalKode() == null ? "-" : p.getLokasiAsalKode(),
                                    p.getLokasiTujuanKode() == null ? "-" : p.getLokasiTujuanKode()
                            });
                            break;
                        }
                    }
                }
                default -> columns = new String[]{"Barang"};
            }

            if (rows.isEmpty()) {
                JLabel empty = new JLabel(
                        "Tidak ada rincian barang untuk transaksi ini " +
                        "(transaksi asli mungkin sudah berubah/terhapus, dan aktivitas ini " +
                        "belum punya snapshot karena dicatat sebelum fitur snapshot ada).");
                empty.setFont(UITheme.FONT_BASE);
                empty.setForeground(UITheme.TEXT_MUTED);
                empty.setBorder(new EmptyBorder(8, 4, 4, 4));
                return empty;
            }

            DefaultTableModel model = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };
            for (Object[] row : rows) {
                model.addRow(row);
            }

            JTable table = new JTable(model);
            TableCopyEnabler.enable(table);
            table.setRowHeight(28);
            table.setFont(UITheme.FONT_BASE);

            JPanel wrapper = new JPanel(new BorderLayout(0, 4));

            JLabel sourceLabel = new JLabel(
                    "Sumber: data transaksi saat ini (belum ada snapshot untuk aktivitas ini)");
            sourceLabel.setFont(UITheme.FONT_LABEL.deriveFont(Font.ITALIC, 11f));
            sourceLabel.setForeground(UITheme.TEXT_MUTED);
            sourceLabel.setBorder(new EmptyBorder(0, 4, 4, 4));
            wrapper.add(sourceLabel, BorderLayout.NORTH);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(460, Math.min(180, 34 + rows.size() * 28)));
            wrapper.add(scroll, BorderLayout.CENTER);
            return wrapper;

        } catch (SQLException ex) {
            JLabel err = new JLabel("Gagal memuat rincian barang: " + ex.getMessage());
            err.setFont(UITheme.FONT_BASE);
            err.setForeground(UITheme.TEXT_MUTED);
            return err;
        }
    }

    private JLabel detailLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_LABEL);
        l.setForeground(UITheme.TEXT_MUTED);
        return l;
    }

    private JLabel detailValue(String text) {
        JLabel l = new JLabel(text == null ? "-" : text);
        l.setFont(UITheme.FONT_BASE.deriveFont(Font.BOLD));
        return l;
    }

    // ------------------------------------------------------------------
    // Filter
    // ------------------------------------------------------------------
    private JPanel buildFilterRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        userCombo.setPreferredSize(new Dimension(160, 36));
        jenisCombo.setPreferredSize(new Dimension(150, 36));
        dariField.setPreferredSize(new Dimension(140, 36));
        sampaiField.setPreferredSize(new Dimension(140, 36));

        RoundedButton searchBtn = new RoundedButton("Cari", RoundedButton.Style.PRIMARY);
        searchBtn.setPreferredSize(new Dimension(90, 36));
        searchBtn.addActionListener(e -> reload());

        RoundedButton resetBtn = new RoundedButton("Reset", RoundedButton.Style.SECONDARY);
        resetBtn.setPreferredSize(new Dimension(90, 36));
        resetBtn.addActionListener(e -> {
            userCombo.setSelectedIndex(0);
            jenisCombo.setSelectedIndex(0);
            dariField.setText("");
            sampaiField.setText("");
            reload();
        });

        RoundedButton excelBtn = new RoundedButton("Excel", RoundedButton.Style.SECONDARY);
        RoundedButton pdfBtn = new RoundedButton("PDF", RoundedButton.Style.SECONDARY);
        excelBtn.addActionListener(e -> exportReport(true));
        pdfBtn.addActionListener(e -> exportReport(false));

        row.add(userCombo);
        row.add(jenisCombo);
        row.add(dariField);
        row.add(sampaiField);
        row.add(searchBtn);
        row.add(resetBtn);
        row.add(excelBtn);
        row.add(pdfBtn);
        return row;
    }

    private void loadUserCombo() {
        userCombo.removeAllItems();
        userCombo.addItem("Semua User");
        try {
            List<User> users = userDAO.findAll("");
            for (User u : users) {
                userCombo.addItem(u.getUsername());
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    // ------------------------------------------------------------------
    // Ringkasan (3 kartu)
    // ------------------------------------------------------------------
    private JPanel buildSummaryRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        row.add(summaryCard("Jumlah Aktivitas", jumlahAktivitasValue, UITheme.ACCENT));
        row.add(summaryCard("Total Item", totalItemValue, UITheme.SUCCESS));
        row.add(summaryCard("Total Qty", totalQtyValue, UITheme.ACCENT));
        return row;
    }

    private JPanel summaryCard(String label, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(accentColor);

        JLabel captionLabel = new JLabel(label);
        captionLabel.setFont(UITheme.FONT_LABEL);
        captionLabel.setForeground(UITheme.TEXT_MUTED);

        card.add(valueLabel, BorderLayout.CENTER);
        card.add(captionLabel, BorderLayout.SOUTH);
        return card;
    }

    // ------------------------------------------------------------------
    // Tabel
    // ------------------------------------------------------------------
    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        table = new JTable(tableModel);
        TableCopyEnabler.enable(table);
        table.setRowHeight(38);
        table.setFont(UITheme.FONT_BASE);
        table.setShowGrid(false);
        table.setToolTipText("Klik dua kali baris untuk melihat detail barang");
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow < 0) return;
                int modelRow = table.convertRowIndexToModel(viewRow);
                if (modelRow < 0 || modelRow >= tableModel.getRowCount()) return;
                String nomor = String.valueOf(tableModel.getValueAt(modelRow, 2));
                for (AktivitasKerja a : currentList) {
                    if (nomor.equals(a.getNomorTransaksi())) {
                        showDetailDialog(a);
                        break;
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(0, 380));
        card.add(scroll, BorderLayout.CENTER);
        pagination = new TablePagination(tableModel, 10);
        card.add(pagination, BorderLayout.SOUTH);
        return card;
    }

    // ------------------------------------------------------------------
    // Reload data
    // ------------------------------------------------------------------

    private void exportReport(boolean excel) {
        String[] columns = {"Username", "Jenis", "No. Transaksi", "Jumlah Item", "Total Qty", "Status", "Waktu Selesai"};
        List<Object[]> rows = new ArrayList<>();
        for (AktivitasKerja a : currentList) rows.add(new Object[]{a.getUsername(), a.getJenisAktivitas(),
                a.getNomorTransaksi(), a.getJumlahItem(), a.getTotalQty(), a.getStatus(),
                a.getWaktuSelesai() == null ? "-" : a.getWaktuSelesai().format(DATETIME_FMT)});
        if (excel) ReportExportService.exportExcel(this, "Laporan Aktivitas Kerja", columns, rows);
        else ReportExportService.exportPdf(this, "Laporan Aktivitas Kerja", columns, rows);
    }

    private void reload() {
        try {
            String username = userCombo.getSelectedIndex() <= 0
                    ? null : (String) userCombo.getSelectedItem();
            String jenis = jenisCombo.getSelectedIndex() <= 0
                    ? null : (String) jenisCombo.getSelectedItem();
            LocalDate dari = parseDate(dariField.getText());
            LocalDate sampai = parseDate(sampaiField.getText());

            List<AktivitasKerja> list = dao.search(username, jenis, dari, sampai);
            currentList = list;

            List<Object[]> rows = new ArrayList<>();
            int totalItem = 0;
            int totalQty = 0;
            for (AktivitasKerja a : list) {
                totalItem += a.getJumlahItem();
                totalQty += a.getTotalQty();
                rows.add(new Object[]{
                        a.getUsername(),
                        a.getJenisAktivitas(),
                        a.getNomorTransaksi(),
                        a.getJumlahItem(),
                        a.getTotalQty(),
                        a.getStatus(),
                        a.getWaktuSelesai() == null ? "-" : a.getWaktuSelesai().format(DATETIME_FMT)
                });
            }
            pagination.setData(rows);

            jumlahAktivitasValue.setText(String.valueOf(list.size()));
            totalItemValue.setText(String.valueOf(totalItem));
            totalQtyValue.setText(String.valueOf(totalQty));
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text.trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Terjadi kesalahan database:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void refresh() {
        loadUserCombo();
        reload();
    }
}
