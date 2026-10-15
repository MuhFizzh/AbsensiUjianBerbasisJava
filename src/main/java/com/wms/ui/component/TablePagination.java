package com.wms.ui.component;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pagination ringan di sisi UI untuk JTable.
 * Data tetap diambil oleh DAO seperti sebelumnya; komponen ini hanya
 * membatasi jumlah baris yang ditampilkan per halaman.
 */
public class TablePagination extends JPanel {

    private final DefaultTableModel model;
    private final int pageSize;
    private final JButton previousButton;
    private final JButton nextButton;
    private final JLabel infoLabel;

    private List<Object[]> allRows = Collections.emptyList();
    private int currentPage = 0;

    public TablePagination(DefaultTableModel model) {
        this(model, 10);
    }

    public TablePagination(DefaultTableModel model, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize harus lebih besar dari 0");
        }

        this.model = model;
        this.pageSize = pageSize;

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        infoLabel = new JLabel("0 data");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        previousButton = new JButton("‹ Sebelumnya");
        nextButton = new JButton("Berikutnya ›");

        previousButton.addActionListener(e -> previousPage());
        nextButton.addActionListener(e -> nextPage());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(previousButton);
        buttons.add(nextButton);

        add(infoLabel, BorderLayout.WEST);
        add(buttons, BorderLayout.EAST);

        refresh();
    }

    public void setData(List<Object[]> rows) {
        allRows = rows == null
                ? Collections.emptyList()
                : new ArrayList<>(rows);

        currentPage = 0;
        refresh();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageCount() {
        return Math.max(1, (int) Math.ceil(allRows.size() / (double) pageSize));
    }

    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            refresh();
        }
    }

    private void nextPage() {
        if (currentPage + 1 < getPageCount()) {
            currentPage++;
            refresh();
        }
    }

    private void refresh() {
        model.setRowCount(0);

        int from = currentPage * pageSize;
        int to = Math.min(from + pageSize, allRows.size());

        for (int i = from; i < to; i++) {
            model.addRow(allRows.get(i));
        }

        if (allRows.isEmpty()) {
            infoLabel.setText("0 data");
        } else {
            infoLabel.setText("Halaman " + (currentPage + 1)
                    + " / " + getPageCount()
                    + "  •  " + allRows.size() + " data");
        }

        previousButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage + 1 < getPageCount());

        revalidate();
        repaint();
    }
}
