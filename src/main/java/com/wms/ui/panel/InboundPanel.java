package com.wms.ui.panel;

import com.wms.dao.AuditLogDAO;
import com.wms.dao.InboundDAO;
import com.wms.model.InboundHeader;
import com.wms.model.InboundItem;
import com.wms.ui.UITheme;
import com.wms.ui.component.TablePagination;
import com.wms.ui.component.PlaceholderTextField;
import com.wms.ui.component.TableCopyEnabler;
import com.wms.ui.component.ActionCellRenderer;
import com.wms.ui.component.RoundedButton;
import com.wms.ui.dialog.InboundFormDialog;
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

/** Layar daftar transaksi Inbound (barang masuk dari supplier). */
public class InboundPanel extends JPanel implements RefreshablePanel {

    private final InboundDAO dao = new InboundDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TablePagination pagination;
    private final PlaceholderTextField searchField;
    private List<InboundHeader> currentList = new ArrayList<>();

    public InboundPanel() {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        searchField = new PlaceholderTextField("Cari nomor inbound / supplier");
        searchField.setPreferredSize(new Dimension(320, 36));
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(searchField);
        RoundedButton addBtn = new RoundedButton("+ Buat Inbound", RoundedButton.Style.PRIMARY);
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

        String[] columns = {"No. Inbound", "Tanggal", "Supplier", "Jumlah Item", "Status", "Aksi"};
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
                "untuk melakukan void transaksi.",
                "Akses Ditolak",
                JOptionPane.WARNING_MESSAGE);

        return;
    }

    int confirm =
            JOptionPane.showConfirmDialog(
                    this,
                    "Apakah Anda yakin ingin melakukan VOID " +
                    "transaksi inbound ini?\n\n" +
                    "Stok yang sebelumnya ditambahkan oleh " +
                    "transaksi ini akan dikurangi kembali.",
                    "Konfirmasi Void",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

    if (confirm != JOptionPane.YES_OPTION) {
        return;
    }

    try {

        String nomor = dao.findNomorById(id);
        dao.voidCompleted(id);

        AuditLogger.log(
                "INBOUND",
                "Void",
                id,
                "Membatalkan transaksi inbound " + nomor +
                        ", stok yang sebelumnya ditambahkan dikurangi kembali. " +
                        "Status: COMPLETED -> CANCELLED"
        );

        JOptionPane.showMessageDialog(
                this,
                "Transaksi inbound berhasil di-void.",
                "Berhasil",
                JOptionPane.INFORMATION_MESSAGE);

        reload();

    } catch (SQLException ex) {

        JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Gagal Void",
                JOptionPane.ERROR_MESSAGE);
    }
}
    
    private void openForm() {
        InboundFormDialog dialog = new InboundFormDialog(SwingUtilities.getWindowAncestor(this));
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
            List<InboundHeader> list = dao.findAll(searchField.getText().trim());
            currentList = list;
            List<Object[]> rows = new java.util.ArrayList<>();
            for (InboundHeader h : list) {
                rows.add(new Object[]{
                        h.getNomor(), h.getTanggal(),
                        h.getSupplierNama() == null ? "-" : h.getSupplierNama(),
                        h.getTotalItem(), h.getStatus(), h.getId()
                });
            }
            pagination.setData(rows);
        } catch (SQLException ex) {
            showError(ex);
        }
    }


    private void exportReport(boolean excel) {
        String[] columns = {"No. Inbound", "Tanggal", "Supplier", "Jumlah Item", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (InboundHeader h : currentList) rows.add(new Object[]{h.getNomor(), h.getTanggal(),
                h.getSupplierNama() == null ? "-" : h.getSupplierNama(), h.getTotalItem(), h.getStatus()});
        if (excel) ReportExportService.exportExcel(this, "Laporan Inbound", columns, rows);
        else ReportExportService.exportPdf(this, "Laporan Inbound", columns, rows);
    }

    private void cetakDokumen(int id) {
        try {
            InboundHeader h = dao.findById(id);
            if (h == null) throw new SQLException("Transaksi inbound tidak ditemukan.");
            DocumentPrintService.printInbound(this, h, dao.findItems(id));
        } catch (SQLException ex) { showError(ex); }
    }

    private void showDetail(int id) {
        try {
            List<InboundItem> items = dao.findItems(id);
            StringBuilder sb = new StringBuilder();
            if (items.isEmpty()) {
                sb.append("Tidak ada item.");
            } else {
                for (InboundItem it : items) {
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
            tabs.addTab("Riwayat", RiwayatPanel.build("INBOUND", id));

            JOptionPane.showMessageDialog(this, tabs, "Detail Inbound", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void terima(int id) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Terima barang untuk transaksi ini?\nStok akan bertambah sesuai item yang tercatat.",
                "Konfirmasi Terima", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            List<InboundItem> items = dao.findItems(id);
            dao.complete(id);

            int totalQty = 0;
            List<LinkedHashMap<String, String>> snapshotRows = new ArrayList<>();
            for (InboundItem item : items) {
                totalQty += item.getQty();
                LinkedHashMap<String, String> row = new LinkedHashMap<>();
                row.put("Barang", item.getBarangNama());
                row.put("Kode", item.getBarangKode());
                row.put("Lokasi", item.getLokasiKode());
                row.put("Qty", String.valueOf(item.getQty()));
                snapshotRows.add(row);
            }
            ActivityLogger.log(
                    "INBOUND", id, dao.findNomorById(id),
                    items.size(), totalQty, "COMPLETED",
                    SnapshotUtil.toJson(snapshotRows),
                    new AuditLogDAO().findCreatorUsername("INBOUND", id));

            AuditLogger.log(
                    "INBOUND", "Terima Barang", id,
                    "Barang diterima, stok diperbarui (" + items.size() + " item, total qty "
                            + totalQty + "). Status: DRAFT -> COMPLETED");

            JOptionPane.showMessageDialog(this, "Barang diterima, stok telah diperbarui.",
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
            String nomor = dao.findNomorById(id);
            dao.deleteDraft(id);
            AuditLogger.log(
                    "INBOUND", "Hapus Draft", id,
                    "Draft inbound " + nomor + " dihapus. Status: DRAFT -> DIHAPUS");
            reload();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Terjadi kesalahan:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Kolom Aksi: tombol Detail selalu ada; Terima & Hapus hanya untuk status DRAFT. */
    private class ActionEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
    private int currentId;

    @Override
    public Component getTableCellEditorComponent(
            JTable table,
            Object value,
            boolean isSelected,
            int row,
            int column) {

        currentId = (Integer) value;

        String status =
                (String) tableModel.getValueAt(row, 4);

        JPanel panel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.CENTER,
                                4,
                                2));

        panel.setOpaque(true);
        panel.setBackground(Color.WHITE);

        RoundedButton detailBtn =
                new RoundedButton(
                        "Detail",
                        RoundedButton.Style.SECONDARY);

        detailBtn.setFont(UITheme.FONT_BASE);

        detailBtn.addActionListener(e -> {
            fireEditingStopped();
            showDetail(currentId);
        });

        panel.add(detailBtn);

        if ("DRAFT".equals(status)) {

            if (Permissions.canConfirmTransaction()) {

                RoundedButton terimaBtn =
                        new RoundedButton(
                                "Kirim",
                                RoundedButton.Style.PRIMARY);

                terimaBtn.setFont(UITheme.FONT_BASE);

                terimaBtn.addActionListener(e -> {
                    fireEditingStopped();
                    terima(currentId);
                });

                panel.add(terimaBtn);

            } else {

                JLabel menungguLabel =
                        new JLabel("Menunggu konfirmasi admin");

                menungguLabel.setFont(UITheme.FONT_LABEL);
                menungguLabel.setForeground(UITheme.TEXT_MUTED);

                panel.add(menungguLabel);
            }

            RoundedButton hapusBtn =
                    new RoundedButton(
                            "Hapus",
                            RoundedButton.Style.DANGER);

            hapusBtn.setFont(UITheme.FONT_BASE);

            hapusBtn.addActionListener(e -> {
                fireEditingStopped();
                hapus(currentId);
            });

            panel.add(hapusBtn);
        }

        // TAMBAHKAN BAGIAN INI
        if ("COMPLETED".equals(status)) {
            RoundedButton printBtn = new RoundedButton("Cetak", RoundedButton.Style.SECONDARY);
            printBtn.setFont(UITheme.FONT_BASE);
            printBtn.addActionListener(e -> { fireEditingStopped(); cetakDokumen(currentId); });
            panel.add(printBtn);
        }

        if ("COMPLETED".equals(status)
                && Permissions.canVoidTransaction()) {

            RoundedButton voidBtn =
                    new RoundedButton(
                            "Void",
                            RoundedButton.Style.DANGER);

            voidBtn.setFont(UITheme.FONT_BASE);

            voidBtn.addActionListener(e -> {
                fireEditingStopped();
                voidTransaction(currentId);
            });

            panel.add(voidBtn);
        }

        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return currentId;
    }
}
}
