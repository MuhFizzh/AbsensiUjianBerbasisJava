package com.wms.ui.panel;

import com.wms.dao.PermintaanDAO;
import com.wms.model.Permintaan;
import com.wms.ui.UITheme;
import com.wms.ui.component.TablePagination;
import com.wms.ui.component.PlaceholderTextField;
import com.wms.ui.component.TableCopyEnabler;
import com.wms.ui.component.RequestStatusBadgeCellRenderer;
import com.wms.ui.component.RoundedButton;
import com.wms.ui.dialog.RiwayatPanel;
import com.wms.util.ActivityLogger;
import com.wms.util.AuditLogger;
import com.wms.util.Permissions;
import com.wms.util.Session;
import com.wms.util.SnapshotUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Layar "Approval": menampilkan seluruh permintaan (default: yang masih
 * menunggu) dan menyediakan tombol Setujui/Tolak untuk memprosesnya.
 * Belum ada pembatasan role - siapa pun bisa memproses (sesuai kesepakatan).
 */
public class ApprovalPanel extends JPanel implements RefreshablePanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final PermintaanDAO dao = new PermintaanDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TablePagination pagination;
    private final PlaceholderTextField searchField;
    private final JCheckBox onlyPendingCheck = new JCheckBox("Hanya tampilkan yang menunggu", true);

    /** Cache status per id, dipakai oleh renderer/editor kolom Aksi. */
    private final Map<Integer, Permintaan> byId = new HashMap<>();

    public ApprovalPanel() {
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        searchField = new PlaceholderTextField("Cari nomor atau pemohon");
        searchField.setPreferredSize(new Dimension(320, 36));
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(searchField);
        onlyPendingCheck.setOpaque(false);
        onlyPendingCheck.setFont(UITheme.FONT_BASE);
        onlyPendingCheck.addActionListener(e -> reload());
        searchWrap.add(onlyPendingCheck);
        toolbar.add(searchWrap, BorderLayout.WEST);
        add(toolbar, BorderLayout.NORTH);

        String[] columns = {"No. Permintaan", "Tanggal", "Jenis", "Barang", "Jumlah", "Detail", "Pemohon", "Status", "Aksi"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 8; }
        };
        table = new JTable(tableModel);
        TableCopyEnabler.enable(table);
        table.setRowHeight(46);
        table.setFont(UITheme.FONT_BASE);
        table.setShowGrid(false);
        table.getTableHeader().setFont(UITheme.FONT_LABEL);
        table.getTableHeader().setBackground(UITheme.TABLE_HEAD_BG);
        table.getTableHeader().setForeground(UITheme.TEXT_MUTED);
        table.setSelectionBackground(UITheme.ACCENT_LIGHT);
        table.setSelectionForeground(UITheme.TEXT);
        table.getColumnModel().getColumn(7).setCellRenderer(new RequestStatusBadgeCellRenderer());
        table.getColumnModel().getColumn(7).setMaxWidth(120);
        table.getColumnModel().getColumn(8).setCellRenderer(new ActionCellRenderer());
        table.getColumnModel().getColumn(8).setCellEditor(new ActionCellEditor());
        table.getColumnModel().getColumn(8).setMinWidth(230);
        table.getColumnModel().getColumn(8).setMaxWidth(260);
        table.getColumnModel().getColumn(0).setMaxWidth(120);
        table.getColumnModel().getColumn(1).setMaxWidth(100);

        pagination = new TablePagination(tableModel, 10);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
        add(pagination, BorderLayout.SOUTH);

        searchField.addActionListener(e -> reload());
        reload();
    }

    private void reload() {
        try {
            List<Permintaan> list = onlyPendingCheck.isSelected()
                    ? dao.findPending(searchField.getText().trim())
                    : dao.findAll(searchField.getText().trim());

            List<Object[]> rows = new java.util.ArrayList<>();
            byId.clear();
            for (Permintaan p : list) {
                byId.put(p.getId(), p);
                rows.add(new Object[]{
                        p.getNomor(),
                        p.getTanggal() == null ? "-" : p.getTanggal().format(DATE_FMT),
                        p.getJenisLabel(),
                        p.getBarangNama() == null ? "-" : p.getBarangNama(),
                        p.getJumlah() == null ? "-" : p.getJumlah(),
                        detailText(p),
                        p.getDiajukanOleh(),
                        p.getStatus(),
                        p.getId()
                });
            }
            pagination.setData(rows);
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private String detailText(Permintaan p) {
        if ("PINDAH_LOKASI".equals(p.getJenis())) {
            String asal = p.getLokasiAsalKode() == null ? "-" : p.getLokasiAsalKode();
            String tujuan = p.getLokasiTujuanKode() == null ? "-" : p.getLokasiTujuanKode();
            return asal + " \u2192 " + tujuan;
        }
        if (p.getKeterangan() != null && !p.getKeterangan().isBlank()) {
            String k = p.getKeterangan();
            return k.length() > 40 ? k.substring(0, 40) + "..." : k;
        }
        return "-";
    }

    /**
     * Membuat snapshot JSON 1 baris (barang, jumlah, lokasi asal/tujuan)
     * dari permintaan yang baru saja disetujui/ditolak, supaya rincian
     * ini tetap bisa dilihat utuh di Aktivitas Kerja meski data barang
     * atau lokasi rak terkait belakangan berubah/dihapus.
     */
    private String permintaanSnapshotJson(Permintaan p) {
        LinkedHashMap<String, String> row = new LinkedHashMap<>();
        row.put("Barang", p.getBarangNama() == null ? "-" : p.getBarangNama());
        row.put("Jumlah", p.getJumlah() == null ? "-" : String.valueOf(p.getJumlah()));
        row.put("Lokasi Asal", p.getLokasiAsalKode() == null ? "-" : p.getLokasiAsalKode());
        row.put("Lokasi Tujuan", p.getLokasiTujuanKode() == null ? "-" : p.getLokasiTujuanKode());

        List<LinkedHashMap<String, String>> rows = new ArrayList<>();
        rows.add(row);
        return SnapshotUtil.toJson(rows);
    }

    private void approve(int id) {
        Permintaan p = byId.get(id);
        if (p == null) return;
        String[] input =
        askApprovalNote("Setujui Permintaan " + p.getNomor());
            if (input == null) return;

            String diprosesOleh = Session.getUsername();

            if (diprosesOleh == null || diprosesOleh.isBlank()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Tidak ada pengguna yang sedang login.",
                        "Validasi",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            try {
                boolean ok = dao.approve(id, diprosesOleh, input[0]);
            if (!ok) {
                JOptionPane.showMessageDialog(this,
                        "Permintaan ini sudah diproses sebelumnya oleh pengguna lain.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            } else {
                ActivityLogger.log(
                        "PERMINTAAN", id, p.getNomor(),
                        1, p.getJumlah() == null ? 0 : p.getJumlah(),
                        "DISETUJUI", permintaanSnapshotJson(p), p.getDiajukanOleh());

                AuditLogger.log(
                        "PERMINTAAN", "Setujui Permintaan", id,
                        "Permintaan " + p.getNomor() + " disetujui. Status: MENUNGGU -> DISETUJUI");
            }
            reload();
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void reject(int id) {
        Permintaan p = byId.get(id);
        if (p == null) return;
        String[] input = askApprovalNote("Tolak Permintaan " + p.getNomor());
            if (input == null) return;

            String diprosesOleh = Session.getUsername();

            if (diprosesOleh == null || diprosesOleh.isBlank()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Tidak ada pengguna yang sedang login.",
                        "Validasi",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            try {
                boolean ok = dao.reject(id, diprosesOleh, input[0]);
            if (!ok) {
                JOptionPane.showMessageDialog(this,
                        "Permintaan ini sudah diproses sebelumnya oleh pengguna lain.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            } else {
                ActivityLogger.log(
                        "PERMINTAAN", id, p.getNomor(),
                        1, p.getJumlah() == null ? 0 : p.getJumlah(),
                        "DITOLAK", permintaanSnapshotJson(p), p.getDiajukanOleh());

                AuditLogger.log(
                        "PERMINTAAN", "Tolak Permintaan", id,
                        "Permintaan " + p.getNomor() + " ditolak. Status: MENUNGGU -> DITOLAK");
            }
            reload();
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

private String[] askApprovalNote(String title) {

    JTextArea noteArea = new JTextArea(3, 20);
    noteArea.setLineWrap(true);
    noteArea.setWrapStyleWord(true);

    JPanel panel = new JPanel(new BorderLayout(0, 10));

    JLabel userLabel = new JLabel(
            "Diproses oleh: " + Session.getUsername()
    );

    JPanel top = new JPanel(new BorderLayout(0, 4));
    top.add(userLabel, BorderLayout.NORTH);

    JPanel bottom = new JPanel(new BorderLayout(0, 4));
    bottom.add(
            new JLabel("Catatan (opsional)"),
            BorderLayout.NORTH
    );
    bottom.add(
            new JScrollPane(noteArea),
            BorderLayout.CENTER
    );

    panel.add(top, BorderLayout.NORTH);
    panel.add(bottom, BorderLayout.CENTER);

    panel.setPreferredSize(new Dimension(320, 160));

    int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            title,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
    );

    if (result != JOptionPane.OK_OPTION) {
        return null;
    }

    return new String[]{
            noteArea.getText().trim()
    };
}

    private void showDbError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Terjadi kesalahan database:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void refresh() {
        reload();
    }

    /** Tampilan kolom Aksi: tombol Setujui/Tolak hanya untuk permintaan berstatus MENUNGGU. */
    private class ActionCellRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private final JLabel doneLabel = new JLabel("Sudah diproses");

        ActionCellRenderer() {
            super(new FlowLayout(FlowLayout.CENTER, 4, 4));
            setOpaque(true);
            doneLabel.setFont(UITheme.FONT_BASE);
            doneLabel.setForeground(UITheme.TEXT_MUTED);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            removeAll();
            int id = value == null ? -1 : (Integer) value;
            Permintaan p = byId.get(id);
            setBackground(Color.WHITE);
            if (p != null && "MENUNGGU".equals(p.getStatus())) {
                add(new JLabel("\u2713 Setujui"));
                add(new JLabel("\u2717 Tolak"));
            } else {
                add(doneLabel);
            }
            add(new JLabel("| \uD83D\uDCC4 Riwayat"));
            return this;
        }
    }

    /** Editor kolom Aksi: tombol nyata yang bisa diklik, hanya untuk permintaan MENUNGGU. */
    private class ActionCellEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JPanel activePanel;
        private final JPanel donePanel;
        private int currentId;

        ActionCellEditor() {
            RoundedButton riwayatBtn1 = buildRiwayatButton();
            RoundedButton riwayatBtn2 = buildRiwayatButton();

            activePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
            activePanel.setOpaque(true);

            RoundedButton approveBtn = new RoundedButton("Setujui", RoundedButton.Style.PRIMARY);
            approveBtn.setPreferredSize(new Dimension(80, 28));
            approveBtn.addActionListener(e -> {
                fireEditingStopped();
                approve(currentId);
            });

            RoundedButton rejectBtn = new RoundedButton("Tolak", RoundedButton.Style.DANGER);
            rejectBtn.setPreferredSize(new Dimension(70, 28));
            rejectBtn.addActionListener(e -> {
                fireEditingStopped();
                reject(currentId);
            });

            activePanel.add(approveBtn);
            activePanel.add(rejectBtn);
            activePanel.add(riwayatBtn1);

            donePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
            donePanel.setOpaque(true);
            JLabel doneLabel = new JLabel("Sudah diproses");
            doneLabel.setFont(UITheme.FONT_BASE);
            doneLabel.setForeground(UITheme.TEXT_MUTED);
            donePanel.add(doneLabel);
            donePanel.add(riwayatBtn2);
        }

        /** Tombol "Riwayat" - selalu bisa diklik, terlepas dari status permintaan (Menunggu/Disetujui/Ditolak). */
        private RoundedButton buildRiwayatButton() {
            RoundedButton btn = new RoundedButton("Riwayat", RoundedButton.Style.SECONDARY);
            btn.setPreferredSize(new Dimension(78, 28));
            btn.addActionListener(e -> {
                fireEditingStopped();
                Permintaan p = byId.get(currentId);
                String nomor = p == null ? "-" : p.getNomor();
                JOptionPane.showMessageDialog(
                        ApprovalPanel.this,
                        RiwayatPanel.build("PERMINTAAN", currentId),
                        "Riwayat Permintaan - " + nomor,
                        JOptionPane.PLAIN_MESSAGE);
            });
            return btn;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentId = value == null ? -1 : (Integer) value;
            Permintaan p = byId.get(currentId);
            if (p != null && "MENUNGGU".equals(p.getStatus())) {
                return activePanel;
            }
            return donePanel;
        }

        @Override
        public Object getCellEditorValue() {
            return currentId;
        }
    }
}
