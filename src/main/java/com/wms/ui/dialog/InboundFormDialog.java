package com.wms.ui.dialog;

import com.wms.dao.BarangDAO;
import com.wms.dao.InboundDAO;
import com.wms.dao.LokasiRakDAO;
import com.wms.dao.SupplierDAO;
import com.wms.model.Barang;
import com.wms.model.InboundHeader;
import com.wms.model.InboundItem;
import com.wms.model.LokasiRak;
import com.wms.model.Supplier;
import com.wms.ui.UITheme;
import com.wms.ui.component.RoundedButton;
import com.wms.util.AuditLogger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Dialog untuk membuat transaksi Inbound baru (header + baris item). */
public class InboundFormDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final InboundDAO inboundDAO = new InboundDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final BarangDAO barangDAO = new BarangDAO();
    private final LokasiRakDAO lokasiDAO = new LokasiRakDAO();

    private final JTextField tanggalField = new JTextField(LocalDate.now().format(DATE_FMT));
    private final JComboBox<Supplier> supplierCombo = new JComboBox<>();
    private final JTextArea catatanArea = new JTextArea(2, 20);

    private final JComboBox<Barang> itemBarangCombo = new JComboBox<>();
    private final JComboBox<LokasiRak> itemLokasiCombo = new JComboBox<>();
    private final JSpinner itemQtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));

    private final DefaultTableModel itemTableModel;
    private final JTable itemTable;
    private final List<InboundItem> items = new ArrayList<>();

    private boolean saved = false;

    public InboundFormDialog(Window owner) {
        super(owner, "Buat Transaksi Inbound", ModalityType.APPLICATION_MODAL);
        setSize(680, 640);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 12));
        getContentPane().setBackground(UITheme.BG);

        JPanel content = new JPanel();
        content.setBackground(UITheme.BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        content.add(buildHeaderCard());
        content.add(Box.createVerticalStrut(14));

        itemTableModel = new DefaultTableModel(new String[]{"Barang", "Lokasi Rak", "Qty", ""}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column == 3; }
        };
        itemTable = new JTable(itemTableModel);
        itemTable.setRowHeight(36);
        itemTable.setFont(UITheme.FONT_BASE);
        itemTable.getTableHeader().setFont(UITheme.FONT_LABEL);
        itemTable.getColumnModel().getColumn(3).setMaxWidth(70);
        itemTable.getColumnModel().getColumn(3).setCellRenderer((table, value, isSelected, hasFocus, row, column) -> {
            RoundedButton btn = new RoundedButton("Hapus", RoundedButton.Style.DANGER);
            btn.setFont(UITheme.FONT_BASE);
            return btn;
        });
        itemTable.getColumnModel().getColumn(3).setCellEditor(new RemoveItemEditor());

        content.add(buildItemInputCard());
        content.add(Box.createVerticalStrut(10));

        JScrollPane itemScroll = new JScrollPane(itemTable);
        itemScroll.setPreferredSize(new Dimension(600, 180));
        itemScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        itemScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        JLabel itemListTitle = new JLabel("Daftar Item");
        itemListTitle.setFont(UITheme.FONT_LABEL);
        itemListTitle.setForeground(UITheme.TEXT_MUTED);
        itemListTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(itemListTitle);
        content.add(Box.createVerticalStrut(6));
        content.add(itemScroll);

        add(new JScrollPane(content), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        loadCombos();
    }

    private JPanel buildHeaderCard() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Informasi Inbound");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card.add(title);
        card.add(Box.createVerticalStrut(12));

        JPanel grid = new JPanel(new GridLayout(1, 2, 14, 0));
        grid.setOpaque(false);
        grid.add(labeledField("Tanggal (yyyy-MM-dd)", tanggalField));
        grid.add(labeledField("Supplier", supplierCombo));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(grid);
        card.add(Box.createVerticalStrut(10));

        catatanArea.setLineWrap(true);
        catatanArea.setWrapStyleWord(true);
        catatanArea.setFont(UITheme.FONT_BASE);
        JScrollPane catatanScroll = new JScrollPane(catatanArea);
        catatanScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        JLabel catatanLabel = new JLabel("Catatan");
        catatanLabel.setFont(UITheme.FONT_LABEL);
        catatanLabel.setForeground(UITheme.TEXT_MUTED);
        catatanLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        catatanScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(catatanLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(catatanScroll);

        return card;
    }

    private JPanel buildItemInputCard() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Tambah Item");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card.add(title);
        card.add(Box.createVerticalStrut(12));

        JPanel grid = new JPanel(new GridLayout(1, 3, 14, 0));
        grid.setOpaque(false);
        grid.add(labeledField("Barang", itemBarangCombo));
        grid.add(labeledField("Lokasi Rak", itemLokasiCombo));
        grid.add(labeledField("Qty", itemQtySpinner));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(grid);
        card.add(Box.createVerticalStrut(10));

        RoundedButton addItemBtn = new RoundedButton("+ Tambah ke Daftar", RoundedButton.Style.SECONDARY);
        addItemBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addItemBtn.addActionListener(e -> addItemRow());
        card.add(addItemBtn);

        return card;
    }

    private JPanel labeledField(String label, JComponent input) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_LABEL);
        l.setForeground(UITheme.TEXT_MUTED);
        input.setFont(UITheme.FONT_BASE);
        if (input instanceof JTextField tf) {
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.BORDER),
                    BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        }
        p.add(l, BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));
        RoundedButton saveBtn = new RoundedButton("Simpan Transaksi", RoundedButton.Style.PRIMARY);
        RoundedButton cancelBtn = new RoundedButton("Batal", RoundedButton.Style.SECONDARY);
        saveBtn.addActionListener(e -> save());
        cancelBtn.addActionListener(e -> dispose());
        bar.add(cancelBtn);
        bar.add(saveBtn);
        return bar;
    }

    private void loadCombos() {
        try {
            supplierCombo.removeAllItems();
            for (Supplier s : supplierDAO.findAll("")) supplierCombo.addItem(s);
            itemBarangCombo.removeAllItems();
            for (Barang b : barangDAO.findAll("")) itemBarangCombo.addItem(b);
            itemLokasiCombo.removeAllItems();
            for (LokasiRak l : lokasiDAO.findAll("")) itemLokasiCombo.addItem(l);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void addItemRow() {
        Barang barang = itemBarangComboSelected();
        LokasiRak lokasi = itemLokasiComboSelected();
        if (barang == null || lokasi == null) {
            JOptionPane.showMessageDialog(this, "Pilih barang dan lokasi rak terlebih dahulu.",
                    "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int qty = (Integer) itemQtySpinner.getValue();

        InboundItem item = new InboundItem();
        item.setBarangId(barang.getId());
        item.setBarangKode(barang.getKode());
        item.setBarangNama(barang.getNama());
        item.setLokasiRakId(lokasi.getId());
        item.setLokasiKode(lokasi.getKode());
        item.setQty(qty);
        items.add(item);

        itemTableModel.addRow(new Object[]{
                barang.getNama() + " (" + barang.getKode() + ")",
                lokasi.getKode(), qty, "Hapus"
        });
        itemQtySpinner.setValue(1);
    }

    private Barang itemBarangComboSelected() {
        return (Barang) itemBarangCombo.getSelectedItem();
    }

    private LokasiRak itemLokasiComboSelected() {
        return (LokasiRak) itemLokasiCombo.getSelectedItem();
    }

    private void save() {
        LocalDate tanggal;
        try {
            tanggal = LocalDate.parse(tanggalField.getText().trim(), DATE_FMT);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Format tanggal salah, gunakan yyyy-MM-dd.",
                    "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tambahkan minimal 1 item barang.",
                    "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        InboundHeader header = new InboundHeader();
        header.setTanggal(tanggal);
        Supplier supplier = (Supplier) supplierCombo.getSelectedItem();
        header.setSupplierId(supplier == null ? null : supplier.getId());
        header.setCatatan(catatanArea.getText().trim());

        try {
            inboundDAO.insert(header, items);
            saved = true;

            AuditLogger.log(
                    "INBOUND", "Buat Draft", header.getId(),
                    "Draft inbound " + header.getNomor() + " dibuat dengan " + items.size()
                            + " item. Status: - -> DRAFT");

            JOptionPane.showMessageDialog(this,
                    "Transaksi " + header.getNomor() + " berhasil disimpan sebagai Draft.\n" +
                    "Klik \"Terima\" pada daftar transaksi untuk memperbarui stok.",
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Terjadi kesalahan database:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    public boolean isSaved() {
        return saved;
    }

    /** Editor tombol "Hapus" pada baris tabel item. */
    private class RemoveItemEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private int currentRow = -1;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            RoundedButton btn = new RoundedButton("Hapus", RoundedButton.Style.DANGER);
            btn.setFont(UITheme.FONT_BASE);
            btn.addActionListener(e -> {
                fireEditingStopped();
                if (currentRow >= 0 && currentRow < items.size()) {
                    items.remove(currentRow);
                    itemTableModel.removeRow(currentRow);
                }
            });
            return btn;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }
}
