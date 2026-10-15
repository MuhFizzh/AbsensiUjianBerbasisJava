package com.wms.ui.dialog;

import com.wms.dao.BarangDAO;
import com.wms.dao.OutboundDAO;
import com.wms.dao.StockDAO;
import com.wms.dao.LokasiRakDAO;
import com.wms.dao.CustomerDAO;
import com.wms.model.Barang;
import com.wms.model.OutboundHeader;
import com.wms.model.OutboundItem;
import com.wms.model.LokasiRak;
import com.wms.model.Customer;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dialog untuk membuat transaksi Outbound baru (header + baris item). */
public class OutboundFormDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OutboundDAO outboundDAO = new OutboundDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final BarangDAO barangDAO = new BarangDAO();
    private final LokasiRakDAO lokasiDAO = new LokasiRakDAO();
    private final StockDAO stockDAO = new StockDAO();

    private final JTextField tanggalField = new JTextField(LocalDate.now().format(DATE_FMT));
    private final JComboBox<Customer> customerCombo = new JComboBox<>();
    private final JTextArea catatanArea = new JTextArea(2, 20);

    private final JComboBox<Barang> itemBarangCombo = new JComboBox<>();
    private final JComboBox<LokasiRak> itemLokasiCombo = new JComboBox<>();
    private final JSpinner itemQtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
    private final JLabel stockInfoLabel = new JLabel("Stok tersedia: -");
    private RoundedButton addItemBtn;

    private final DefaultTableModel itemTableModel;
    private final JTable itemTable;
    private final List<OutboundItem> items = new ArrayList<>();

    private boolean saved = false;

    public OutboundFormDialog(Window owner) {
        super(owner, "Buat Transaksi Outbound", ModalityType.APPLICATION_MODAL);
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

        itemBarangCombo.addActionListener(e -> updateStockAvailability());
        itemLokasiCombo.addActionListener(e -> updateStockAvailability());
        updateStockAvailability();
    }

    private JPanel buildHeaderCard() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Informasi Outbound");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card.add(title);
        card.add(Box.createVerticalStrut(12));

        JPanel grid = new JPanel(new GridLayout(1, 2, 14, 0));
        grid.setOpaque(false);
        grid.add(labeledField("Tanggal (yyyy-MM-dd)", tanggalField));
        grid.add(labeledField("Customer", customerCombo));
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

        stockInfoLabel.setFont(UITheme.FONT_LABEL);
        stockInfoLabel.setForeground(UITheme.TEXT_MUTED);
        stockInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(stockInfoLabel);
        card.add(Box.createVerticalStrut(8));

        addItemBtn = new RoundedButton("+ Tambah ke Daftar", RoundedButton.Style.SECONDARY);
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
            customerCombo.removeAllItems();
            for (Customer s : customerDAO.findAll("")) customerCombo.addItem(s);
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

        int available = getRemainingStock(barang.getId(), lokasi.getId());
        int qty = (Integer) itemQtySpinner.getValue();

        if (available <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Stok barang tersebut di " + lokasi.getKode() + " sudah habis atau sudah seluruhnya dimasukkan ke daftar.",
                    "Stok Tidak Tersedia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (qty > available) {
            JOptionPane.showMessageDialog(this,
                    "Qty tidak boleh melebihi stok yang tersedia.\n"
                    + "Tersedia: " + available + " unit",
                    "Stok Tidak Mencukupi", JOptionPane.WARNING_MESSAGE);
            itemQtySpinner.setValue(available);
            return;
        }

        OutboundItem item = new OutboundItem();
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
        updateStockAvailability();
    }

    private Barang itemBarangComboSelected() {
        return (Barang) itemBarangCombo.getSelectedItem();
    }

    private LokasiRak itemLokasiComboSelected() {
        return (LokasiRak) itemLokasiCombo.getSelectedItem();
    }

    /**
     * Mengembalikan stok yang masih boleh dimasukkan ke daftar untuk kombinasi
     * barang + lokasi. Stok dikurangi dengan qty yang sudah ada di draft form.
     */
    private int getRemainingStock(int barangId, int lokasiRakId) {
        try {
            int stock = stockDAO.getAvailableQty(barangId, lokasiRakId);
            int alreadyAdded = 0;

            for (OutboundItem item : items) {
                if (item.getBarangId() == barangId
                        && item.getLokasiRakId() == lokasiRakId) {
                    alreadyAdded += item.getQty();
                }
            }

            return Math.max(0, stock - alreadyAdded);
        } catch (SQLException ex) {
            stockInfoLabel.setText("Stok tersedia: gagal dibaca");
            addItemBtn.setEnabled(false);
            return 0;
        }
    }

    private void updateStockAvailability() {
        Barang barang = itemBarangComboSelected();
        LokasiRak lokasi = itemLokasiComboSelected();

        if (barang == null || lokasi == null) {
            stockInfoLabel.setText("Stok tersedia: -");
            addItemBtn.setEnabled(false);
            return;
        }

        int available = getRemainingStock(barang.getId(), lokasi.getId());
        stockInfoLabel.setText("Stok tersedia di " + lokasi.getKode() + ": " + available + " unit");
        addItemBtn.setEnabled(available > 0);

        int max = Math.max(0, available);
        int current = (Integer) itemQtySpinner.getValue();

        if (max == 0) {
            itemQtySpinner.setModel(new SpinnerNumberModel(0, 0, 0, 1));
        } else {
            int value = Math.min(Math.max(current, 1), max);
            itemQtySpinner.setModel(new SpinnerNumberModel(value, 1, max, 1));
        }
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

        // Validasi ulang terhadap database tepat sebelum draft disimpan.
        // Ini menangani kondisi stok berubah setelah form dibuka.
        try {
            Map<String, Integer> requested = new HashMap<>();
            Map<String, OutboundItem> representatives = new HashMap<>();

            for (OutboundItem item : items) {
                String key = item.getBarangId() + ":" + item.getLokasiRakId();
                requested.merge(key, item.getQty(), Integer::sum);
                representatives.putIfAbsent(key, item);
            }

            for (Map.Entry<String, Integer> entry : requested.entrySet()) {
                OutboundItem item = representatives.get(entry.getKey());
                int available = stockDAO.getAvailableQty(
                        item.getBarangId(), item.getLokasiRakId());

                if (entry.getValue() > available) {
                    JOptionPane.showMessageDialog(this,
                            "Stok tidak mencukupi untuk " + item.getBarangNama()
                            + " di " + item.getLokasiKode() + ".\n"
                            + "Tersedia: " + available + " unit\n"
                            + "Total diminta: " + entry.getValue() + " unit",
                            "Stok Tidak Mencukupi", JOptionPane.WARNING_MESSAGE);
                    updateStockAvailability();
                    return;
                }
            }
        } catch (SQLException ex) {
            showError(ex);
            return;
        }

        OutboundHeader header = new OutboundHeader();
        header.setTanggal(tanggal);
        Customer customer = (Customer) customerCombo.getSelectedItem();
        header.setCustomerId(customer == null ? null : customer.getId());
        header.setCatatan(catatanArea.getText().trim());

        try {
            outboundDAO.insert(header, items);
            saved = true;

            AuditLogger.log(
                    "OUTBOUND", "Buat Draft", header.getId(),
                    "Draft outbound " + header.getNomor() + " dibuat dengan " + items.size()
                            + " item. Status: - -> DRAFT");

            JOptionPane.showMessageDialog(this,
                    "Transaksi " + header.getNomor() + " berhasil disimpan sebagai Draft.\n" +
                    "Klik \"Kirim\" pada daftar transaksi untuk memperbarui stok.",
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
                    updateStockAvailability();
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
