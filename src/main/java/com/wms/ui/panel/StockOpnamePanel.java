package com.wms.ui.panel;

import com.wms.dao.AuditLogDAO;
import com.wms.dao.StockOpnameDAO;
import com.wms.model.StockOpname;
import com.wms.model.StockOpnameDetail;
import com.wms.ui.UITheme;
import com.wms.ui.component.TablePagination;
import com.wms.ui.component.PlaceholderTextField;
import com.wms.ui.component.TableCopyEnabler;
import com.wms.ui.component.ActionCellRenderer;
import com.wms.ui.component.RoundedButton;
import com.wms.ui.dialog.RiwayatPanel;
import com.wms.ui.dialog.StockOpnameFormDialog;
import com.wms.util.ActivityLogger;
import com.wms.util.AuditLogger;
import com.wms.util.Permissions;
import com.wms.util.SnapshotUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Layar daftar transaksi Stock Opname (cycle count & penyesuaian stok per lokasi rak). */
public class StockOpnamePanel extends JPanel implements RefreshablePanel {

    private final StockOpnameDAO dao = new StockOpnameDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TablePagination pagination;
    private final PlaceholderTextField searchField;

    public StockOpnamePanel() {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        searchField = new PlaceholderTextField("Cari nomor opname / lokasi");
        searchField.setPreferredSize(new Dimension(320, 36));
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(searchField);
        RoundedButton addBtn = new RoundedButton("+ Buat Stock Opname", RoundedButton.Style.PRIMARY);
        addBtn.addActionListener(e -> openForm());
        toolbar.add(searchWrap, BorderLayout.WEST);
        toolbar.add(addBtn, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        String[] columns = {"No. Opname", "Tanggal", "Lokasi", "Jumlah Item", "Total Selisih", "Status", "Aksi"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 6; }
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
        table.getColumnModel().getColumn(6).setCellRenderer(new ActionCellRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new ActionEditor());
        table.getColumnModel().getColumn(6).setMinWidth(230);
        table.getColumnModel().getColumn(6).setMaxWidth(260);

        pagination = new TablePagination(tableModel, 10);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        add(scrollPane, BorderLayout.CENTER);
        add(pagination, BorderLayout.SOUTH);

        searchField.addActionListener(e -> reload());
        reload();
    }

    private void openForm() {
        if (!Permissions.canDoStockOpname()) {
            JOptionPane.showMessageDialog(this,
                    "Anda tidak memiliki izin untuk membuat Stock Opname.",
                    "Akses Ditolak", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StockOpnameFormDialog dialog = new StockOpnameFormDialog(SwingUtilities.getWindowAncestor(this));
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
            List<StockOpname> list = dao.findAll(searchField.getText().trim());
            List<Object[]> rows = new java.util.ArrayList<>();
            for (StockOpname o : list) {
                rows.add(new Object[]{
                        o.getNomor(), o.getTanggal(),
                        o.getLokasiKode() == null ? "-" : o.getLokasiKode(),
                        o.getTotalItem(), o.getTotalSelisih(), o.getStatus(), o.getId()
                });
            }
            pagination.setData(rows);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showDetail(int id) {
        try {
            List<StockOpnameDetail> items = dao.findItems(id);
            StringBuilder sb = new StringBuilder();
            if (items.isEmpty()) {
                sb.append("Tidak ada item.");
            } else {
                for (StockOpnameDetail it : items) {
                    sb.append(String.format("- %s (%s) : sistem %d, fisik %d, selisih %+d%n",
                            it.getBarangNama(), it.getBarangKode(),
                            it.getSystemQty(), it.getActualQty(), it.getAdjustmentQty()));
                }
            }
            JTextArea area = new JTextArea(sb.toString());
            area.setEditable(false);
            area.setFont(UITheme.FONT_MONO);
            JScrollPane sp = new JScrollPane(area);
            sp.setPreferredSize(new Dimension(460, 240));

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Item Barang", sp);
            tabs.addTab("Riwayat", RiwayatPanel.build("STOCK_OPNAME", id));

            JOptionPane.showMessageDialog(this, tabs, "Detail Stock Opname", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void proses(int id) {
        if (!Permissions.canDoStockOpname()) {
            JOptionPane.showMessageDialog(this,
                    "Anda tidak memiliki izin untuk memproses Stock Opname.",
                    "Akses Ditolak", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Proses transaksi Stock Opname ini?\nSelisih hasil hitung akan diterapkan ke stok dan tidak dapat dibatalkan.",
                "Konfirmasi Proses", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            List<StockOpnameDetail> items = dao.findItems(id);
            dao.complete(id);

            int totalQty = 0;
            List<LinkedHashMap<String, String>> snapshotRows = new ArrayList<>();
            for (StockOpnameDetail item : items) {
                totalQty += item.getActualQty();
                LinkedHashMap<String, String> row = new LinkedHashMap<>();
                row.put("Barang", item.getBarangNama());
                row.put("Kode", item.getBarangKode());
                row.put("Lokasi", item.getLokasiKode());
                row.put("Sistem", String.valueOf(item.getSystemQty()));
                row.put("Aktual", String.valueOf(item.getActualQty()));
                row.put("Selisih", String.valueOf(item.getAdjustmentQty()));
                snapshotRows.add(row);
            }
            ActivityLogger.log(
                    "STOCK_OPNAME", id, dao.findNomorById(id),
                    items.size(), totalQty, "COMPLETED",
                    SnapshotUtil.toJson(snapshotRows),
                    new AuditLogDAO().findCreatorUsername("STOCK_OPNAME", id));

            AuditLogger.log(
                    "STOCK_OPNAME", "Proses Opname", id,
                    "Stock opname diselesaikan (" + items.size() + " item dihitung). Status: DRAFT -> COMPLETED");

            JOptionPane.showMessageDialog(this, "Stock Opname berhasil diproses, stok telah disesuaikan.",
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

    /** Kolom Aksi: tombol Detail selalu ada; Proses & Hapus hanya untuk status DRAFT. */
    private class ActionEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private int currentId;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentId = (Integer) value;
            String status = (String) tableModel.getValueAt(row, 5);

            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
            panel.setOpaque(true);
            panel.setBackground(Color.WHITE);

            RoundedButton detailBtn = new RoundedButton("Detail", RoundedButton.Style.SECONDARY);
            detailBtn.setFont(UITheme.FONT_BASE);
            detailBtn.addActionListener(e -> {
                fireEditingStopped();
                showDetail(currentId);
            });
            panel.add(detailBtn);

            if ("DRAFT".equals(status)) {
                if (Permissions.canConfirmTransaction()) {
                    RoundedButton prosesBtn = new RoundedButton("Proses", RoundedButton.Style.PRIMARY);
                    prosesBtn.setFont(UITheme.FONT_BASE);
                    prosesBtn.addActionListener(e -> {
                        fireEditingStopped();
                        proses(currentId);
                    });
                    panel.add(prosesBtn);
                } else {
                    JLabel menungguLabel = new JLabel("Menunggu konfirmasi admin");
                    menungguLabel.setFont(UITheme.FONT_LABEL);
                    menungguLabel.setForeground(UITheme.TEXT_MUTED);
                    panel.add(menungguLabel);
                }

                RoundedButton hapusBtn = new RoundedButton("Hapus", RoundedButton.Style.DANGER);
                hapusBtn.setFont(UITheme.FONT_BASE);
                hapusBtn.addActionListener(e -> {
                    fireEditingStopped();
                    hapus(currentId);
                });
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
