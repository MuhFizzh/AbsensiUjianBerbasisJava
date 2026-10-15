package com.wms.ui.panel;

import com.wms.dao.LokasiRakDAO;
import com.wms.model.LokasiRak;
import com.wms.ui.UITheme;
import com.wms.ui.component.TablePagination;
import com.wms.ui.component.BadgeCellRenderer;
import com.wms.ui.component.PlaceholderTextField;
import com.wms.ui.component.ActionCellRenderer;
import com.wms.ui.component.RoundedButton;
import com.wms.util.DocumentPrintService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class LokasiRakPanel extends JPanel implements RefreshablePanel {

    private final LokasiRakDAO dao = new LokasiRakDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TablePagination pagination;
    private final PlaceholderTextField searchField;

    private final JTextField kodeField = new JTextField();
    private final JTextField areaField = new JTextField();
    private final JTextField rakField = new JTextField();
    private final JTextField kolomField = new JTextField();
    private final JTextField posisiField = new JTextField();
    private final JSpinner kapasitasSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Aktif", "Nonaktif"});

    private Integer editingId = null;
    private JDialog formDialog;

    public LokasiRakPanel() {
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        searchField = new PlaceholderTextField("Cari lokasi rak");
        searchField.setPreferredSize(new Dimension(320, 36));
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(searchField);
        RoundedButton addBtn = new RoundedButton("+ Tambah Lokasi", RoundedButton.Style.PRIMARY);
        addBtn.addActionListener(e -> { resetForm(); openFormDialog(); });
        toolbar.add(searchWrap, BorderLayout.WEST);
        toolbar.add(addBtn, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        String[] columns = {"Kode Lokasi", "Area", "Rak", "Kolom", "Posisi", "Kapasitas", "Status", "Aksi"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 7; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(42);
        table.setFont(UITheme.FONT_BASE);
        table.setShowGrid(false);
        table.getTableHeader().setFont(UITheme.FONT_LABEL);
        table.getTableHeader().setBackground(UITheme.TABLE_HEAD_BG);
        table.getTableHeader().setForeground(UITheme.TEXT_MUTED);
        table.setSelectionBackground(UITheme.ACCENT_LIGHT);
        table.setSelectionForeground(UITheme.TEXT);
        table.getColumnModel().getColumn(6).setCellRenderer(new BadgeCellRenderer());
        table.getColumnModel().getColumn(7).setCellRenderer(new ActionCellRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new ActionEditor());
        table.getColumnModel().getColumn(7).setMinWidth(180);
        table.getColumnModel().getColumn(7).setMaxWidth(220);
        table.getColumnModel().getColumn(0).setMaxWidth(100);
        table.getColumnModel().getColumn(6).setMaxWidth(110);

        pagination = new TablePagination(tableModel, 10);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(scrollPane, BorderLayout.CENTER);
        center.add(pagination, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        searchField.addActionListener(e -> reload());
        reload();
    }

    private JPanel buildFormCard() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)));
        card.setLayout(new BorderLayout(0, 16));
        card.setMaximumSize(new Dimension(640, 340));

        JLabel title = new JLabel("Tambah / Edit Lokasi Rak");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 14, 12));
        grid.setOpaque(false);
        kodeField.setToolTipText("Kosongkan untuk dibuatkan otomatis oleh sistem");
        grid.add(field("Kode lokasi", kodeField, false));
        grid.add(field("Area", areaField, false));
        grid.add(field("Nomor rak", rakField, false));
        grid.add(field("Nomor kolom", kolomField, false));
        grid.add(field("Nomor posisi", posisiField, false));
        grid.add(field("Kapasitas", kapasitasSpinner, false));
        grid.add(field("Status", statusCombo, false));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(grid, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        RoundedButton saveBtn = new RoundedButton("Simpan", RoundedButton.Style.PRIMARY);
        RoundedButton cancelBtn = new RoundedButton("Batal", RoundedButton.Style.SECONDARY);
        saveBtn.addActionListener(e -> save());
        cancelBtn.addActionListener(e -> { resetForm(); closeFormDialog(); });
        actions.add(saveBtn);
        actions.add(cancelBtn);
        card.add(actions, BorderLayout.SOUTH);

        return card;
    }

    private JPanel field(String label, JComponent input, boolean readOnly) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_LABEL);
        l.setForeground(UITheme.TEXT_MUTED);
        if (input instanceof JTextField tf) {
            tf.setFont(UITheme.FONT_BASE);
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.BORDER),
                    BorderFactory.createEmptyBorder(7, 9, 7, 9)));
            if (readOnly) {
                tf.setEditable(false);
                tf.setBackground(UITheme.BG);
                tf.setText("Terisi otomatis");
            }
        } else {
            input.setFont(UITheme.FONT_BASE);
        }
        p.add(l, BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void refresh() {
        reload();
    }

    private void reload() {
        try {
            List<LokasiRak> list = dao.findAll(searchField.getText().trim());
            List<Object[]> rows = new java.util.ArrayList<>();
            for (LokasiRak l : list) {
                rows.add(new Object[]{
                        l.getKode(), l.getArea(), l.getRak(), l.getKolom(), l.getPosisi(),
                        l.getKapasitas() + " unit", l.getStatus(), l.getId()
                });
            }
            pagination.setData(rows);
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    /** Membuka form Tambah/Edit sebagai dialog terpisah, bukan panel yang selalu terlihat. */
    private void openFormDialog() {
        if (formDialog == null) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            formDialog = new JDialog(owner, "Tambah / Edit Lokasi Rak", Dialog.ModalityType.APPLICATION_MODAL);
            formDialog.setContentPane(buildFormCard());
            formDialog.pack();
        }
        formDialog.setLocationRelativeTo(this);
        formDialog.setVisible(true);
    }

    private void closeFormDialog() {
        if (formDialog != null) {
            formDialog.setVisible(false);
        }
    }

    private void resetForm() {
        editingId = null;
        kodeField.setText("");
        kodeField.setEditable(true);
        areaField.setText("");
        rakField.setText("");
        kolomField.setText("");
        posisiField.setText("");
        kapasitasSpinner.setValue(0);
        statusCombo.setSelectedIndex(0);
    }

    private void save() {
        String area = areaField.getText().trim();
        String rak = rakField.getText().trim();
        if (area.isEmpty() || rak.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Area dan nomor rak wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LokasiRak l = new LokasiRak();
        l.setKode(kodeField.getText().trim());
        l.setArea(area);
        l.setRak(rak);
        l.setKolom(kolomField.getText().trim());
        l.setPosisi(posisiField.getText().trim());
        l.setKapasitas((Integer) kapasitasSpinner.getValue());
        l.setStatus(statusCombo.getSelectedItem().equals("Aktif") ? "AKTIF" : "NONAKTIF");
        try {
            if (editingId == null) {
                dao.insert(l);
            } else {
                l.setId(editingId);
                dao.update(l);
            }
            resetForm();
            closeFormDialog();
            reload();
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void loadForEdit(int id) {
        try {
            List<LokasiRak> list = dao.findAll("");
            for (LokasiRak l : list) {
                if (l.getId() == id) {
                    editingId = id;
                    kodeField.setText(l.getKode());
                    kodeField.setEditable(false);
                    areaField.setText(l.getArea());
                    rakField.setText(l.getRak());
                    kolomField.setText(l.getKolom());
                    posisiField.setText(l.getPosisi());
                    kapasitasSpinner.setValue(l.getKapasitas());
                    statusCombo.setSelectedItem("AKTIF".equals(l.getStatus()) ? "Aktif" : "Nonaktif");
                    break;
                }
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void deleteRow(int id) {
        int confirm = JOptionPane.showConfirmDialog(this, "Hapus lokasi rak ini?", "Konfirmasi",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dao.delete(id);
                reload();
            } catch (SQLException ex) {
                showDbError(ex);
            }
        }
    }

    private void showDbError(SQLException ex) {

        String message = ex.getMessage();

        if (message != null &&
            message.contains("Lokasi rak tidak dapat dihapus")) {

            JOptionPane.showMessageDialog(
                    this,
                    message +
                    "\n\nGunakan Edit → ubah status menjadi NONAKTIF " +
                    "jika lokasi rak sudah tidak digunakan.",
                    "Lokasi Rak Tidak Dapat Dihapus",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "Terjadi kesalahan database:\n" + message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private class ActionEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {

    protected void onEdit(int id) {
        loadForEdit(id);
        openFormDialog();
    }

    protected void onDelete(int id) {
        deleteRow(id);
    }

    @Override
    public Component getTableCellEditorComponent(
            JTable table,
            Object value,
            boolean isSelected,
            int row,
            int column) {

        int id = (Integer) value;

        JPanel panel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 3, 2)
        );

        panel.setOpaque(true);
        panel.setBackground(Color.WHITE);

        RoundedButton edit =
                new RoundedButton("Edit", RoundedButton.Style.SECONDARY);

        RoundedButton barcode =
                new RoundedButton("Barcode", RoundedButton.Style.PRIMARY);

        RoundedButton delete =
                new RoundedButton("Hapus", RoundedButton.Style.DANGER);

        edit.addActionListener(e -> {
            fireEditingStopped();
            onEdit(id);
        });

        barcode.addActionListener(e -> {
            fireEditingStopped();

            try {
                for (LokasiRak l : dao.findAll("")) {
                    if (l.getId() == id) {
                        DocumentPrintService.printRackLabel(
                                LokasiRakPanel.this, l
                        );
                        break;
                    }
                }
            } catch (SQLException ex) {
                showDbError(ex);
            }
        });

        delete.addActionListener(e -> {
            fireEditingStopped();
            onDelete(id);
        });

        panel.add(edit);
        panel.add(barcode);
        panel.add(delete);

        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }
}
}
