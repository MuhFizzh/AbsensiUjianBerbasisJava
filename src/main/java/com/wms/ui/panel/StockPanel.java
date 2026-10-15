package com.wms.ui.panel;

import com.wms.dao.StockDAO;
import com.wms.model.StockItem;
import com.wms.ui.UITheme;
import com.wms.ui.component.PlaceholderTextField;
import com.wms.ui.component.RoundedButton;
import com.wms.ui.component.TablePagination;
import com.wms.util.ReportExportService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class StockPanel extends JPanel
        implements RefreshablePanel {

    private final StockDAO dao =
            new StockDAO();

    private final PlaceholderTextField searchField =
            new PlaceholderTextField(
                    "Cari barang / kode / lokasi");

    private final TablePagination pagination;
    private List<StockItem> currentList = new ArrayList<>();

    private final DefaultTableModel model =
            new DefaultTableModel(
                    new Object[]{
                            "Kode",
                            "Barang",
                            "Lokasi",
                            "Qty",
                            "Min. Stock",
                            "Status"
                    }, 0) {

        @Override
        public boolean isCellEditable(
                int r,
                int c) {

            return false;
        }
    };

    public StockPanel() {

        setLayout(
                new BorderLayout(0, 14));

        setOpaque(false);

        setBorder(
                BorderFactory.createEmptyBorder(
                        24, 28, 24, 28));

        searchField.setPreferredSize(
                new Dimension(320, 36));

        JPanel top =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT));

        top.setOpaque(false);

        top.add(searchField);
        RoundedButton excelBtn = new RoundedButton("Excel", RoundedButton.Style.SECONDARY);
        RoundedButton pdfBtn = new RoundedButton("PDF", RoundedButton.Style.SECONDARY);
        excelBtn.addActionListener(e -> exportReport(true));
        pdfBtn.addActionListener(e -> exportReport(false));
        top.add(excelBtn);
        top.add(pdfBtn);

        add(top, BorderLayout.NORTH);

        JTable table =
                new JTable(model);

        table.setRowHeight(42);
        table.setFont(UITheme.FONT_BASE);
        table.setShowGrid(false);

        add(
                new JScrollPane(table),
                BorderLayout.CENTER);

        pagination = new TablePagination(model, 10);
        add(pagination, BorderLayout.SOUTH);

        searchField.addActionListener(
                e -> reload());

        reload();
    }

    private void reload() {

        try {

            List<StockItem> list =
                    dao.findAll(
                            searchField
                                    .getText()
                                    .trim());
            currentList = list;

            List<Object[]> rows = new java.util.ArrayList<>();

            for (StockItem s : list) {

                String status;

                if (s.getQty() <= 0) {
                    status = "Habis";

                } else if (
                        s.getQty()
                                <= s.getMinStock()) {

                    status = "Menipis";

                } else {
                    status = "Aman";
                }

                rows.add(
                        new Object[]{
                                s.getBarangKode(),
                                s.getBarangNama(),
                                s.getLokasiKode(),
                                s.getQty(),
                                s.getMinStock(),
                                status
                        });
            }

            pagination.setData(rows);

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    private void exportReport(boolean excel) {
        String[] columns = {"Kode", "Barang", "Lokasi", "Qty", "Min. Stock", "Status"};
        List<Object[]> rows = new ArrayList<>();
        for (StockItem s : currentList) {
            String status = s.getQty() <= 0 ? "Habis" : (s.getQty() <= s.getMinStock() ? "Menipis" : "Aman");
            rows.add(new Object[]{s.getBarangKode(), s.getBarangNama(), s.getLokasiKode(),
                    s.getQty(), s.getMinStock(), status});
        }
        if (excel) ReportExportService.exportExcel(this, "Laporan Stock", columns, rows);
        else ReportExportService.exportPdf(this, "Laporan Stock", columns, rows);
    }

    @Override
    public void refresh() {
        reload();
    }
}