package com.wms.ui.panel;

import com.wms.dao.AuditLogDAO;
import com.wms.dao.OutboundDAO;
import com.wms.model.OutboundHeader;
import com.wms.model.OutboundItem;
import com.wms.ui.UITheme;
import com.wms.ui.component.TablePagination;
import com.wms.ui.component.PlaceholderTextField;
import com.wms.ui.component.TableCopyEnabler;
import com.wms.ui.component.ActionCellRenderer;
import com.wms.ui.component.RoundedButton;
import com.wms.ui.dialog.OutboundFormDialog;
import com.wms.ui.dialog.RiwayatPanel;
import com.wms.util.ActivityLogger;
import com.wms.util.AuditLogger;
import com.wms.util.Permissions;
import com.wms.util.SnapshotUtil;
import com.wms.util.ReportExportService;
import com.wms.util.DocumentPrintService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Layar daftar transaksi Outbound (barang keluar ke customer). */
public class OutboundPanel extends JPanel implements RefreshablePanel {

    private final OutboundDAO dao = new OutboundDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TablePagination pagination;
    private final PlaceholderTextField searchField;
    private List<OutboundHeader> currentList = new ArrayList<>();

    public OutboundPanel() {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        searchField = new PlaceholderTextField("Cari nomor outbound / customer");
        searchField.setPreferredSize(new Dimension(320, 36));
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(searchField);
        RoundedButton addBtn = new RoundedButton("+ Buat Outbound", RoundedButton.Style.PRIMARY);
        addBtn.addActionListener(e -> openForm());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        RoundedButton excelBtn = new RoundedButton("Excel", RoundedButton.Style.SECONDARY);
        RoundedButton pdfBtn = new RoundedButton("PDF", RoundedButton.Style.SECONDARY);
        excelBtn.addActionListener(e -> exportReport(true));
        pdfBtn.addActionListener(e -> exportReport(false));
        actions.add(excelBtn); actions.add(pdfBtn); actions.add(addBtn);
        toolbar.add(searchWrap, BorderLayout.WEST);
        toolbar.add(actions, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        String[] columns = {"No. Outbound", "Tanggal", "Customer", "Jumlah Item", "Status", "Aksi"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 5; }
        };
        table = new JTable(tableModel);
        TableCopyEnabler.enable(table);
        table.setRowHeight(44);
        table.setFont(UITheme.FONT_BASE);
        table.setShowGrid(false);
        table.getTableHeader().setFont(UITheme.FONT_LABEL);
        table.getTableHeader().setBackground(UITheme.TABLE_HEAD_BG);
        table.getTableHeader().setForeground(UITheme.TEXT_MUTED);
        table.setSelectionBackground(UITheme.ACCENT_LIGHT);
        table.setSelectionForeground(UITheme.TEXT);
        table.getColumnModel().getColumn(5).setCellRenderer(new ActionCellRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new ActionEditor());
        table.getColumnModel().getColumn(5).setMinWidth(230);
        table.getColumnModel().getColumn(5).setMaxWidth(260);

        pagination = new TablePagination(tableModel, 10);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        add(scrollPane, BorderLayout.CENTER);
        add(pagination, BorderLayout.SOUTH);

        searchField.addActionListener(e -> reload());
        reload();
    }
    
    private void voidTransaction(int id) {

    if (!Permissions.canVoidTransaction()) {

        JOptionPane.showMessageDialog(
                this,
                "Anda tidak memiliki izin " +
                "untuk melakukan void.",
                "Akses ditolak",
                JOptionPane.WARNING_MESSAGE);

        return;
    }

    int confirm =
            JOptionPane.showConfirmDialog(
                    this,
                    "Void transaksi ini?\n" +
                    "Stok akan dikembalikan.",
                    "Konfirmasi Void",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

    if (confirm != JOptionPane.YES_OPTION) {
        return;
    }

    try {

        dao.voidCompleted(id);

        AuditLogger.log(
                "OUTBOUND",
                "VOID",
                id,
                "Membatalkan transaksi");

        reload();

    } catch (SQLException ex) {

        showError(ex);
    }
}

    private void openForm() {
        OutboundFormDialog dialog = new OutboundFormDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            reload();
        }
    }

    @Override
    public void refresh() {
        reload();
    }

    private void reload() {
        try {
            List<OutboundHeader> list = dao.findAll(searchField.getText().trim());
            currentList = list;
            List<Object[]> rows = new java.util.ArrayList<>();
            for (OutboundHeader h : list) {
                rows.add(new Object[]{
                        h.getNomor(), h.getTanggal(),
                        h.getCustomerNama() == null ? "-" : h.getCustomerNama(),
                        h.getTotalItem(), h.getStatus(), h.getId()
                });
            }
            pagination.setData(rows);
        } catch (SQLException ex) {
            showError(ex);
        }
    }


    private void exportReport(boolean excel) {
        String[] columns = {"No. Outbound", "Tanggal", "Customer", "Jumlah Item", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (OutboundHeader h : currentList) rows.add(new Object[]{h.getNomor(), h.getTanggal(),
                h.getCustomerNama() == null ? "-" : h.getCustomerNama(), h.getTotalItem(), h.getStatus()});
        if (excel) ReportExportService.exportExcel(this, "Laporan Outbound", columns, rows);
        else ReportExportService.exportPdf(this, "Laporan Outbound", columns, rows);
    }

    private void cetakDokumen(int id) {
        try {
            OutboundHeader h = dao.findById(id);
            if (h == null) throw new SQLException("Transaksi outbound tidak ditemukan.");
            DocumentPrintService.printOutbound(this, h, dao.findItems(id));
        } catch (SQLException ex) { showError(ex); }
    }

    private void showDetail(int id) {
        try {
            List<OutboundItem> items = dao.findItems(id);
            StringBuilder sb = new StringBuilder();
            if (items.isEmpty()) {
                sb.append("Tidak ada item.");
            } else {
                for (OutboundItem it : items) {
                    sb.append(String.format("- %s (%s) @ %s : %d unit%n",
                            it.getBarangNama(), it.getBarangKode(), it.getLokasiKode(), it.getQty()));
                }
            }
            JTextArea area = new JTextArea(sb.toString());
            area.setEditable(false);
            area.setFont(UITheme.FONT_MONO);
            JScrollPane sp = new JScrollPane(area);
            sp.setPreferredSize(new Dimension(420, 220));

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Item Barang", sp);
            tabs.addTab("Riwayat", RiwayatPanel.build("OUTBOUND", id));

            JOptionPane.showMessageDialog(this, tabs, "Detail Outbound", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void kirim(int id) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Kirim barang untuk transaksi ini?\nStok akan berkurang sesuai item yang tercatat, dan akan divalidasi terhadap stok yang tersedia.",
                "Konfirmasi Kirim", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            List<OutboundItem> items = dao.findItems(id);
            dao.complete(id);

            int totalQty = 0;
            List<LinkedHashMap<String, String>> snapshotRows = new ArrayList<>();
            for (OutboundItem item : items) {
                totalQty += item.getQty();
                LinkedHashMap<String, String> row = new LinkedHashMap<>();
                row.put("Barang", item.getBarangNama());
                row.put("Kode", item.getBarangKode());
                row.put("Lokasi", item.getLokasiKode());
                row.put("Qty", String.valueOf(item.getQty()));
                snapshotRows.add(row);
            }
            ActivityLogger.log(
                    "OUTBOUND", id, dao.findNomorById(id),
                    items.size(), totalQty, "COMPLETED",
                    SnapshotUtil.toJson(snapshotRows),
                    new AuditLogDAO().findCreatorUsername("OUTBOUND", id));

            AuditLogger.log(
                    "OUTBOUND", "Kirim Barang", id,
                    "Barang dikirim, stok dikurangi (" + items.size() + " item, total qty "
                            + totalQty + "). Status: DRAFT -> COMPLETED");

            JOptionPane.showMessageDialog(this, "Barang telah dikirim, stok telah diperbarui.",
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            reload();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void hapus(int id) {
        int confirm = JOptionPane.showConfirmDialog(this, "Hapus transaksi draft ini?", "Konfirmasi",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            dao.deleteDraft(id);
            reload();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Terjadi kesalahan:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Kolom Aksi: tombol Detail selalu ada; Kirim & Hapus hanya untuk status DRAFT. */
    private class ActionEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private int currentId;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentId = (Integer) value;
            String status = (String) tableModel.getValueAt(row, 4);

            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
            panel.setOpaque(true);
            panel.setBackground(Color.WHITE);

            RoundedButton detailBtn = new RoundedButton("Detail", RoundedButton.Style.SECONDARY);
            detailBtn.setFont(UITheme.FONT_BASE);
            detailBtn.addActionListener(e -> { fireEditingStopped(); showDetail(currentId); });
            panel.add(detailBtn);

            if ("COMPLETED".equals(status)) {
                RoundedButton printBtn = new RoundedButton("Cetak", RoundedButton.Style.SECONDARY);
                printBtn.setFont(UITheme.FONT_BASE);
                printBtn.addActionListener(e -> { fireEditingStopped(); cetakDokumen(currentId); });
                panel.add(printBtn);
            }

            if ("DRAFT".equals(status)) {
                if (Permissions.canConfirmTransaction()) {
                    RoundedButton kirimBtn = new RoundedButton("Kirim", RoundedButton.Style.PRIMARY);
                    kirimBtn.setFont(UITheme.FONT_BASE);
                    kirimBtn.addActionListener(e -> { fireEditingStopped(); kirim(currentId); });
                    panel.add(kirimBtn);
                } else {
                    JLabel menungguLabel = new JLabel("Menunggu konfirmasi admin");
                    menungguLabel.setFont(UITheme.FONT_LABEL);
                    menungguLabel.setForeground(UITheme.TEXT_MUTED);
                    panel.add(menungguLabel);
                }

                RoundedButton hapusBtn = new RoundedButton("Hapus", RoundedButton.Style.DANGER);
                hapusBtn.setFont(UITheme.FONT_BASE);
                hapusBtn.addActionListener(e -> { fireEditingStopped(); hapus(currentId); });
                panel.add(hapusBtn);
            }
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return currentId;
        }
    }
}
