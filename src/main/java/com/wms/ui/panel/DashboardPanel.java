package com.wms.ui.panel;

import com.wms.dao.DashboardDAO;
import com.wms.dao.InboundDAO;
import com.wms.dao.OutboundDAO;
import com.wms.model.InboundHeader;
import com.wms.model.LowStockItem;
import com.wms.model.OutboundHeader;
import com.wms.ui.UITheme;
import com.wms.ui.component.BadgeLabel;
import com.wms.ui.component.TablePagination;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Layar Dashboard: ringkasan angka, riwayat transaksi Inbound & Outbound
 * terakhir, dan daftar barang yang stoknya sudah di bawah nilai minimum
 * (min_stock) yang diatur di Master Data Barang.
 */
public class DashboardPanel extends JPanel implements RefreshablePanel {

    private static final int RECENT_LIMIT = 5;

    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final InboundDAO inboundDAO = new InboundDAO();
    private final OutboundDAO outboundDAO = new OutboundDAO();

    private JLabel cardBarangValue, cardInboundValue, cardOutboundValue, cardLowStockValue;
    private DefaultTableModel inboundTableModel;
    private DefaultTableModel outboundTableModel;
    private DefaultTableModel lowStockTableModel;
    private TablePagination inboundPagination;
    private TablePagination outboundPagination;
    private TablePagination lowStockPagination;

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 18));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(buildSummaryRow());
        content.add(Box.createVerticalStrut(18));
        content.add(buildRecentTransactionsRow());
        content.add(Box.createVerticalStrut(18));
        content.add(buildLowStockCard());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        reload();
    }

    // ------------------------------------------------------------------
    // Ringkasan angka (4 kartu)
    // ------------------------------------------------------------------
    private JPanel buildSummaryRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        cardBarangValue = new JLabel("0");
        cardInboundValue = new JLabel("0");
        cardOutboundValue = new JLabel("0");
        cardLowStockValue = new JLabel("0");

        row.add(summaryCard("Barang Aktif", cardBarangValue, UITheme.ACCENT));
        row.add(summaryCard("Inbound Bulan Ini", cardInboundValue, UITheme.SUCCESS));
        row.add(summaryCard("Outbound Bulan Ini", cardOutboundValue, UITheme.ACCENT));
        row.add(summaryCard("Stok Di Bawah Minimum", cardLowStockValue, UITheme.DANGER));
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
    // Riwayat Inbound & Outbound terakhir (berdampingan)
    // ------------------------------------------------------------------
    private JPanel buildRecentTransactionsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        inboundTableModel = new DefaultTableModel(new String[]{"No. Inbound", "Tanggal", "Supplier", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        outboundTableModel = new DefaultTableModel(new String[]{"No. Outbound", "Tanggal", "Customer", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        row.add(recentTransactionCard("Inbound Terakhir", inboundTableModel));
        row.add(recentTransactionCard("Outbound Terakhir", outboundTableModel));
        return row;
    }

    private JPanel recentTransactionCard(String title, DefaultTableModel model) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card.add(titleLabel, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.setFont(UITheme.FONT_BASE);
        table.setShowGrid(false);
        table.setEnabled(false);
        table.getTableHeader().setFont(UITheme.FONT_LABEL);
        table.getTableHeader().setBackground(UITheme.TABLE_HEAD_BG);
        table.getTableHeader().setForeground(UITheme.TEXT_MUTED);
        table.getColumnModel().getColumn(3).setCellRenderer((t, value, isSelected, hasFocus, row, column) -> {
            boolean completed = "COMPLETED".equals(value);
            BadgeLabel badge = new BadgeLabel(completed ? "Selesai" : "Draft", completed);
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            wrap.setOpaque(true);
            wrap.setBackground(Color.WHITE);
            wrap.add(badge);
            return wrap;
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        card.add(scroll, BorderLayout.CENTER);

        TablePagination pagination = new TablePagination(model, 5);
        if (model == inboundTableModel) {
            inboundPagination = pagination;
        } else if (model == outboundTableModel) {
            outboundPagination = pagination;
        }
        card.add(pagination, BorderLayout.SOUTH);
        return card;
    }

    // ------------------------------------------------------------------
    // Barang dengan stok di bawah minimum
    // ------------------------------------------------------------------
    private JPanel buildLowStockCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel title = new JLabel("Barang dengan Stok Di Bawah Minimum");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card.add(title, BorderLayout.NORTH);

        lowStockTableModel = new DefaultTableModel(
                new String[]{"Kode", "Nama Barang", "Satuan", "Stok Saat Ini", "Stok Minimum", "Kekurangan"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(lowStockTableModel);
        table.setRowHeight(36);
        table.setFont(UITheme.FONT_BASE);
        table.setShowGrid(false);
        table.setEnabled(false);
        table.getTableHeader().setFont(UITheme.FONT_LABEL);
        table.getTableHeader().setBackground(UITheme.TABLE_HEAD_BG);
        table.getTableHeader().setForeground(UITheme.TEXT_MUTED);
        table.getColumnModel().getColumn(5).setCellRenderer((t, value, isSelected, hasFocus, row, column) -> {
            JLabel l = new JLabel(String.valueOf(value));
            l.setForeground(UITheme.DANGER);
            l.setFont(UITheme.FONT_BOLD);
            l.setOpaque(true);
            l.setBackground(Color.WHITE);
            return l;
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        scroll.setPreferredSize(new Dimension(100, 220));
        card.add(scroll, BorderLayout.CENTER);
        lowStockPagination = new TablePagination(lowStockTableModel, 10);
        card.add(lowStockPagination, BorderLayout.SOUTH);
        return card;
    }

    // ------------------------------------------------------------------
    // Reload data
    // ------------------------------------------------------------------
    @Override
    public void refresh() {
        reload();
    }

    private void reload() {
        try {
            DashboardDAO.Summary summary = dashboardDAO.getSummary();
            cardBarangValue.setText(String.valueOf(summary.totalBarangAktif()));
            cardInboundValue.setText(String.valueOf(summary.inboundBulanIni()));
            cardOutboundValue.setText(String.valueOf(summary.outboundBulanIni()));
            cardLowStockValue.setText(String.valueOf(summary.totalLowStock()));

            List<InboundHeader> recentInbound = inboundDAO.findRecent(RECENT_LIMIT);
            List<Object[]> inboundRows = new java.util.ArrayList<>();
            for (InboundHeader h : recentInbound) {
                inboundRows.add(new Object[]{
                        h.getNomor(), h.getTanggal(),
                        h.getSupplierNama() == null ? "-" : h.getSupplierNama(),
                        h.getStatus()
                });
            }
            inboundPagination.setData(inboundRows);

            List<OutboundHeader> recentOutbound = outboundDAO.findRecent(RECENT_LIMIT);
            List<Object[]> outboundRows = new java.util.ArrayList<>();
            for (OutboundHeader h : recentOutbound) {
                outboundRows.add(new Object[]{
                        h.getNomor(), h.getTanggal(),
                        h.getCustomerNama() == null ? "-" : h.getCustomerNama(),
                        h.getStatus()
                });
            }
            outboundPagination.setData(outboundRows);

            List<LowStockItem> lowStock = dashboardDAO.findLowStock();
            List<Object[]> lowStockRows = new java.util.ArrayList<>();
            for (LowStockItem item : lowStock) {
                int shortage = item.getMinStock() - item.getTotalStock();
                lowStockRows.add(new Object[]{
                        item.getKode(), item.getNama(), item.getSatuan(),
                        item.getTotalStock(), item.getMinStock(), shortage
                });
            }
            lowStockPagination.setData(lowStockRows);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Gagal memuat data dashboard:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
