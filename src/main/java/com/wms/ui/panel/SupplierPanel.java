package com.wms.ui.panel;

import com.wms.dao.SupplierDAO;
import com.wms.model.Supplier;
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

public class SupplierPanel extends JPanel implements RefreshablePanel {

    private final SupplierDAO dao = new SupplierDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TablePagination pagination;
    private final PlaceholderTextField searchField;

    private final JTextField kodeField = new JTextField();
    private final JTextField namaField = new JTextField();
    private final JTextField teleponField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Aktif", "Nonaktif"});
    private final JTextArea alamatArea = new JTextArea(3, 20);

    private Integer editingId = null;
    private JDialog formDialog;

    public SupplierPanel() {
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        searchField = new PlaceholderTextField("Cari supplier");
        searchField.setPreferredSize(new Dimension(320, 36));
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(searchField);
        RoundedButton addBtn = new RoundedButton("+ Tambah Supplier", RoundedButton.Style.PRIMARY);
        addBtn.addActionListener(e -> { resetForm(); openFormDialog(); });
        toolbar.add(searchWrap, BorderLayout.WEST);
        toolbar.add(addBtn, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);

        String[] columns = {"Kode", "Nama Supplier", "No. Telepon", "Email", "Status", "Aksi"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 5; }
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
        table.getColumnModel().getColumn(4).setCellRenderer(new BadgeCellRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new ActionCellRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new ActionEditor());
        table.getColumnModel().getColumn(5).setMaxWidth(90);
        table.getColumnModel().getColumn(0).setMaxWidth(90);
        table.getColumnModel().getColumn(4).setMaxWidth(110);

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
        card.setMaximumSize(new Dimension(640, 380));

        JLabel title = new JLabel("Tambah / Edit Supplier");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 14, 12));
        grid.setOpaque(false);
        kodeField.setToolTipText("Kosongkan untuk dibuatkan otomatis oleh sistem");
        grid.add(field("Kode supplier", kodeField, false));
        grid.add(field("Nama supplier", namaField, false));
        grid.add(field("No. telepon", teleponField, false));
        grid.add(field("Email", emailField, false));
        grid.add(field("Status", statusCombo, false));

        alamatArea.setLineWrap(true);
        alamatArea.setWrapStyleWord(true);
        alamatArea.setFont(UITheme.FONT_BASE);
        JScrollPane alamatScroll = new JScrollPane(alamatArea);
        alamatScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        JPanel alamatField = new JPanel(new BorderLayout(0, 5));
        alamatField.setOpaque(false);
        JLabel alamatLabel = new JLabel("Alamat");
        alamatLabel.setFont(UITheme.FONT_LABEL);
        alamatLabel.setForeground(UITheme.TEXT_MUTED);
        alamatField.add(alamatLabel, BorderLayout.NORTH);
        alamatField.add(alamatScroll, BorderLayout.CENTER);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        alamatField.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(grid);
        body.add(Box.createVerticalStrut(12));
        body.add(alamatField);
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

    @Override
    public void refresh() {
        reload();
    }

    private void reload() {
        try {
            List<Supplier> list = dao.findAll(searchField.getText().trim());
            List<Object[]> rows = new java.util.ArrayList<>();
            for (Supplier s : list) {
                rows.add(new Object[]{s.getKode(), s.getNama(), s.getTelepon(), s.getEmail(), s.getStatus(), s.getId()});
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
            formDialog = new JDialog(owner, "Tambah / Edit Supplier", Dialog.ModalityType.APPLICATION_MODAL);
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
        teleponField.setText("");
        emailField.setText("");
        alamatArea.setText("");
        statusCombo.setSelectedIndex(0);
    }

    private void save() {
        String nama = namaField.getText().trim();
        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama supplier wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Supplier s = new Supplier();
        s.setKode(kodeField.getText().trim());
        s.setNama(nama);
        s.setTelepon(teleponField.getText().trim());
        s.setEmail(emailField.getText().trim());
        s.setAlamat(alamatArea.getText().trim());
        s.setStatus(statusCombo.getSelectedItem().equals("Aktif") ? "AKTIF" : "NONAKTIF");
        try {
            if (editingId == null) {
                dao.insert(s);
            } else {
                s.setId(editingId);
                dao.update(s);
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
            List<Supplier> list = dao.findAll("");
            for (Supplier s : list) {
                if (s.getId() == id) {
                    editingId = id;
                    kodeField.setText(s.getKode());
                    kodeField.setEditable(false);
                    namaField.setText(s.getNama());
                    teleponField.setText(s.getTelepon());
                    emailField.setText(s.getEmail());
                    alamatArea.setText(s.getAlamat());
                    statusCombo.setSelectedItem("AKTIF".equals(s.getStatus()) ? "Aktif" : "Nonaktif");
                    break;
                }
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void deleteRow(int id) {
        int confirm = JOptionPane.showConfirmDialog(this, "Hapus supplier ini?", "Konfirmasi",
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
        JOptionPane.showMessageDialog(this, "Terjadi kesalahan database:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    private class ActionEditor extends AbstractCellEditorPanel {
        @Override protected void onEdit(int id) { loadForEdit(id); openFormDialog(); }
        @Override protected void onDelete(int id) { deleteRow(id); }
    }
}
