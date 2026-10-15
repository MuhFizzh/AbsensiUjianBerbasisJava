package com.wms.ui.dialog;

import com.wms.dao.AuditLogDAO;
import com.wms.model.AuditLog;
import com.wms.ui.UITheme;
import com.wms.ui.component.TablePagination;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel "Riwayat" (timeline audit log) untuk 1 transaksi tertentu.
 *
 * Terinspirasi dari panel "Riwayat" pada aplikasi manajemen pesanan
 * (kolom Waktu | Dikerjakan Oleh | Aktivitas | Status), dibangun dari
 * tabel audit_log yang sudah ada (module + reference_id), supaya bisa
 * dipasang berulang di dialog detail transaksi manapun (Inbound,
 * Outbound, Stock Opname, Permintaan, dst).
 */
public final class RiwayatPanel {

    private static final AuditLogDAO DAO = new AuditLogDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private RiwayatPanel() {
    }

    /**
     * Membangun panel siap pakai (bisa ditaruh langsung di tab/dialog)
     * berisi tabel riwayat untuk module + referenceId tertentu.
     */
    public static JComponent build(String module, int referenceId) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(6, 6, 6, 6));

        try {
            List<AuditLog> history = DAO.findHistory(module, referenceId);

            if (history.isEmpty()) {
                JLabel empty = new JLabel("Belum ada riwayat aktivitas untuk transaksi ini.");
                empty.setFont(UITheme.FONT_BASE);
                empty.setForeground(UITheme.TEXT_MUTED);
                empty.setBorder(new EmptyBorder(12, 4, 4, 4));
                wrapper.add(empty, BorderLayout.NORTH);
                return wrapper;
            }

            String[] columns = {"Waktu", "Dikerjakan Oleh", "Aktivitas", "Status / Detail"};
            DefaultTableModel model = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };

            List<Object[]> rows = new java.util.ArrayList<>();
            for (AuditLog a : history) {
                rows.add(new Object[]{
                        a.getCreatedAt() == null ? "-" : a.getCreatedAt().format(FMT),
                        a.getUsername(),
                        a.getAction(),
                        a.getDescription() == null ? "-" : a.getDescription()
                });
            }

            JTable table = new JTable(model);
            table.setRowHeight(30);
            table.setFont(UITheme.FONT_BASE);
            table.getColumnModel().getColumn(0).setPreferredWidth(150);
            table.getColumnModel().getColumn(1).setPreferredWidth(120);
            table.getColumnModel().getColumn(2).setPreferredWidth(140);
            table.getColumnModel().getColumn(3).setPreferredWidth(320);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(760, 320));
            wrapper.add(scroll, BorderLayout.CENTER);

            TablePagination pagination = new TablePagination(model, 10);
            pagination.setData(rows);
            wrapper.add(pagination, BorderLayout.SOUTH);

        } catch (SQLException ex) {
            JLabel err = new JLabel("Gagal memuat riwayat: " + ex.getMessage());
            err.setFont(UITheme.FONT_BASE);
            err.setForeground(UITheme.TEXT_MUTED);
            wrapper.add(err, BorderLayout.NORTH);
        }

        return wrapper;
    }

    /** Menampilkan riwayat sebagai dialog berdiri sendiri (kalau tidak digabung dalam tab). */
    public static void showDialog(Component parent, String module, int referenceId, String judul) {
        JOptionPane.showMessageDialog(parent, build(module, referenceId),
                "Riwayat - " + judul, JOptionPane.PLAIN_MESSAGE);
    }
}
