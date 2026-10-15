package com.wms.ui.panel;

import com.wms.dao.KategoriDAO;
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

public class KategoriPanel extends JPanel implements RefreshablePanel {

    private final KategoriDAO dao = new KategoriDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TablePagination pagination;
    private final PlaceholderTextField searchField;

    private final JTextField kodeField = new JTextField();
    private final JTextField namaField = new JTextField();
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Aktif", "Nonaktif"});
    private final JTextArea deskripsiArea = new JTextArea(3, 20);

    private Integer editingId = null;
    private JDialog formDialog;

    public KategoriPanel() {
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        searchField = new PlaceholderTextField("Cari kategori");
        searchField.setPreferredSize(new Dimension(320, 36));
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(searchField);

        RoundedButton addBtn = new RoundedButton("+ Tambah Kategori", RoundedButton.Style.PRIMARY);
        addBtn.addActionListener(e -> { resetForm(); openFormDialog(); });

        toolbar.add(searchWrap, BorderLayout.WEST);
        toolbar.add(addBtn, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        String[] columns = {"Kode", "Nama Kategori", "Deskripsi", "Status", "Aksi"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4;
            }
        };
        table = buildTable();

        pagination = new TablePagination(tableModel, 10);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(scrollPane, BorderLayout.CENTER);
        center.add(pagination, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);

        searchField.addActionListener(e -> reload());
        reload();
    }

    private JTable buildTable() {
        JTable t = new JTable(tableModel);
        t.setRowHeight(42);
        t.setFont(UITheme.FONT_BASE);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.getTableHeader().setFont(UITheme.FONT_LABEL);
        t.getTableHeader().setBackground(UITheme.TABLE_HEAD_BG);
        t.getTableHeader().setForeground(UITheme.TEXT_MUTED);
        t.setSelectionBackground(UITheme.ACCENT_LIGHT);
        t.setSelectionForeground(UITheme.TEXT);
        t.getColumnModel().getColumn(3).setCellRenderer(new BadgeCellRenderer());
        t.getColumnModel().getColumn(4).setCellRenderer(new ActionCellRenderer());
        t.getColumnModel().getColumn(4).setCellEditor(new ActionEditor());
        t.getColumnModel().getColumn(4).setMaxWidth(90);
        t.getColumnModel().getColumn(0).setMaxWidth(90);
        t.getColumnModel().getColumn(3).setMaxWidth(110);
        return t;
    }

    private JPanel buildFormCard() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)));
        card.setLayout(new BorderLayout(0, 16));
        card.setMaximumSize(new Dimension(640, 320));

        JLabel title = new JLabel("Tambah / Edit Kategori");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 14, 12));
        grid.setOpaque(false);
        kodeField.setToolTipText("Kosongkan untuk dibuatkan otomatis oleh sistem");
        grid.add(field("Kode kategori", kodeField, false));
        grid.add(field("Nama kategori", namaField, false));
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
        JPanel fullRowWrap = new JPanel(new BorderLayout());
        fullRowWrap.setOpaque(false);
        fullRowWrap.add(descField, BorderLayout.CENTER);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        fullRowWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(grid);
        body.add(Box.createVerticalStrut(12));
        body.add(fullRowWrap);
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
        } else if (input instanceof JComboBox<?> cb) {
            cb.setFont(UITheme.FONT_BASE);
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
            List<Kategori> list = dao.findAll(searchField.getText().trim());
            List<Object[]> rows = new java.util.ArrayList<>();
            for (Kategori k : list) {
                rows.add(new Object[]{k.getKode(), k.getNama(), k.getDeskripsi(), k.getStatus(), k.getId()});
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
            formDialog = new JDialog(owner, "Tambah / Edit Kategori", Dialog.ModalityType.APPLICATION_MODAL);
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
        deskripsiArea.setText("");
        statusCombo.setSelectedIndex(0);
    }

    private void save() {
        String nama = namaField.getText().trim();
        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama kategori wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Kategori k = new Kategori();
        k.setKode(kodeField.getText().trim());
        k.setNama(nama);
        k.setDeskripsi(deskripsiArea.getText().trim());
        k.setStatus(statusCombo.getSelectedItem().equals("Aktif") ? "AKTIF" : "NONAKTIF");
        try {
            if (editingId == null) {
                dao.insert(k);
            } else {
                k.setId(editingId);
                dao.update(k);
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
            List<Kategori> list = dao.findAll("");
            for (Kategori k : list) {
                if (k.getId() == id) {
                    editingId = id;
                    kodeField.setText(k.getKode());
                    kodeField.setEditable(false);
                    namaField.setText(k.getNama());
                    deskripsiArea.setText(k.getDeskripsi());
                    statusCombo.setSelectedItem("AKTIF".equals(k.getStatus()) ? "Aktif" : "Nonaktif");
                    break;
                }
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void deleteRow(int id) {
        int confirm = JOptionPane.showConfirmDialog(this, "Hapus kategori ini?", "Konfirmasi",
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
        JOptionPane.showMessageDialog(this,
                "Terjadi kesalahan database:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    private class ActionEditor extends AbstractCellEditorPanel {
        @Override protected void onEdit(int id) { loadForEdit(id); openFormDialog(); }
        @Override protected void onDelete(int id) { deleteRow(id); }
    }
}
