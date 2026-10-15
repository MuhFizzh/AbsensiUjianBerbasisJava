package com.wms.ui.panel;

import com.wms.dao.BarangDAO;
import com.wms.dao.KategoriDAO;
import com.wms.model.Barang;
import com.wms.model.Kategori;
import com.wms.ui.UITheme;
import com.wms.ui.component.TablePagination;
import com.wms.ui.component.BadgeCellRenderer;
import com.wms.ui.component.PlaceholderTextField;
import com.wms.ui.component.ActionCellRenderer;
import com.wms.ui.component.RoundedButton;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class BarangPanel extends JPanel implements RefreshablePanel {

    private final BarangDAO dao = new BarangDAO();
    private final KategoriDAO kategoriDAO = new KategoriDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TablePagination pagination;
    private final PlaceholderTextField searchField;

    private final JTextField kodeField = new JTextField();
    private final JTextField namaField = new JTextField();
    private final JComboBox<Kategori> kategoriCombo = new JComboBox<>();
    private final JComboBox<String> satuanCombo = new JComboBox<>(new String[]{"Pcs", "Box", "Unit"});
    private final JSpinner minStockSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Aktif", "Nonaktif"});
    private final JTextArea deskripsiArea = new JTextArea(3, 20);

    private Integer editingId = null;
    private JDialog formDialog;

    public BarangPanel() {
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        searchField = new PlaceholderTextField("Cari kode atau nama barang");
        searchField.setPreferredSize(new Dimension(320, 36));
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(searchField);
        RoundedButton addBtn = new RoundedButton("+ Tambah Barang", RoundedButton.Style.PRIMARY);
        addBtn.addActionListener(e -> { resetForm(); openFormDialog(); });
        toolbar.add(searchWrap, BorderLayout.WEST);
        toolbar.add(addBtn, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        String[] columns = {"Kode", "Nama Barang", "Kategori", "Satuan", "Min. Stock", "Status", "Aksi"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6;
            }
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
        table.getColumnModel().getColumn(5).setCellRenderer(new BadgeCellRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(new ActionCellRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new ActionEditor());
        table.getColumnModel().getColumn(6).setMaxWidth(90);
        table.getColumnModel().getColumn(0).setMaxWidth(90);
        table.getColumnModel().getColumn(5).setMaxWidth(110);

        pagination = new TablePagination(tableModel, 10);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(scrollPane, BorderLayout.CENTER);
        center.add(pagination, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        searchField.addActionListener(e -> reload());
        loadKategoriCombo();
        reload();
    }

    private JPanel buildFormCard() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)));
        card.setLayout(new BorderLayout(0, 16));
        card.setMaximumSize(new Dimension(640, 420));

        JLabel title = new JLabel("Tambah / Edit Barang");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 14, 12));
        grid.setOpaque(false);
        kodeField.setToolTipText("Kosongkan untuk dibuatkan otomatis oleh sistem");
        grid.add(field("Kode barang", kodeField, false));
        grid.add(field("Nama barang", namaField, false));
        grid.add(field("Kategori", kategoriCombo, false));
        grid.add(field("Satuan", satuanCombo, false));
        grid.add(field("Minimum stock", minStockSpinner, false));
        grid.add(field("Status", statusCombo, false));

        deskripsiArea.setLineWrap(true);
        deskripsiArea.setWrapStyleWord(true);
        deskripsiArea.setFont(UITheme.FONT_BASE);
        JScrollPane descScroll = new JScrollPane(deskripsiArea);
        descScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        JPanel descField = new JPanel(new BorderLayout(0, 5));
        descField.setOpaque(false);
        JLabel descLabel = new JLabel("Deskripsi");
        descLabel.setFont(UITheme.FONT_LABEL);
        descLabel.setForeground(UITheme.TEXT_MUTED);
        descField.add(descLabel, BorderLayout.NORTH);
        descField.add(descScroll, BorderLayout.CENTER);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        descField.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(grid);
        body.add(Box.createVerticalStrut(12));
        body.add(descField);
        card.add(body, BorderLayout.CENTER);

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

    private void loadKategoriCombo() {
        kategoriCombo.removeAllItems();
        try {
            List<Kategori> list = kategoriDAO.findAll("");
            for (Kategori k : list) kategoriCombo.addItem(k);
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    @Override
    public void refresh() {
        loadKategoriCombo();
        reload();
    }

    private void reload() {
        try {
            List<Barang> list = dao.findAll(searchField.getText().trim());
            List<Object[]> rows = new java.util.ArrayList<>();
            for (Barang b : list) {
                rows.add(new Object[]{
                        b.getKode(), b.getNama(),
                        b.getKategoriNama() == null ? "-" : b.getKategoriNama(),
                        b.getSatuan(), b.getMinStock(), b.getStatus(), b.getId()
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
            formDialog = new JDialog(owner, "Tambah / Edit Barang", Dialog.ModalityType.APPLICATION_MODAL);
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
        namaField.setText("");
        loadKategoriCombo();
        if (kategoriCombo.getItemCount() > 0) kategoriCombo.setSelectedIndex(0);
        satuanCombo.setSelectedIndex(0);
        minStockSpinner.setValue(0);
        statusCombo.setSelectedIndex(0);
        deskripsiArea.setText("");
    }

    private void save() {
        String nama = namaField.getText().trim();
        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama barang wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Barang b = new Barang();
        b.setKode(kodeField.getText().trim());
        b.setNama(nama);
        Kategori selected = (Kategori) kategoriCombo.getSelectedItem();
        b.setKategoriId(selected == null ? null : selected.getId());
        b.setSatuan((String) satuanCombo.getSelectedItem());
        b.setMinStock((Integer) minStockSpinner.getValue());
        b.setStatus(statusCombo.getSelectedItem().equals("Aktif") ? "AKTIF" : "NONAKTIF");
        b.setDeskripsi(deskripsiArea.getText().trim());
        try {
            if (editingId == null) {
                dao.insert(b);
            } else {
                b.setId(editingId);
                dao.update(b);
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
            List<Barang> list = dao.findAll("");
            for (Barang b : list) {
                if (b.getId() == id) {
                    editingId = id;
                    kodeField.setText(b.getKode());
                    kodeField.setEditable(false);
                    namaField.setText(b.getNama());
                    loadKategoriCombo();
                    for (int i = 0; i < kategoriCombo.getItemCount(); i++) {
                        Kategori k = kategoriCombo.getItemAt(i);
                        if (b.getKategoriId() != null && k.getId() == b.getKategoriId()) {
                            kategoriCombo.setSelectedIndex(i);
                            break;
                        }
                    }
                    satuanCombo.setSelectedItem(b.getSatuan());
                    minStockSpinner.setValue(b.getMinStock());
                    statusCombo.setSelectedItem("AKTIF".equals(b.getStatus()) ? "Aktif" : "Nonaktif");
                    deskripsiArea.setText(b.getDeskripsi());
                    break;
                }
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void deleteRow(int id) {
        int confirm = JOptionPane.showConfirmDialog(this, "Hapus barang ini?", "Konfirmasi",
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
        message.contains("Barang tidak dapat dihapus")) {

        JOptionPane.showMessageDialog(
                this,
                message +
                "\n\nGunakan Edit → ubah status menjadi NONAKTIF " +
                "jika barang sudah tidak digunakan.",
                "Barang Tidak Dapat Dihapus",
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

    private class ActionEditor extends AbstractCellEditorPanel {
        @Override protected void onEdit(int id) { loadForEdit(id); openFormDialog(); }
        @Override protected void onDelete(int id) { deleteRow(id); }
    }
}
