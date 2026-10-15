package com.wms.ui.dialog;

import com.wms.dao.LokasiRakDAO;
import com.wms.dao.StockDAO;
import com.wms.dao.StockOpnameDAO;
import com.wms.model.LokasiRak;
import com.wms.model.StockItem;
import com.wms.model.StockOpname;
import com.wms.model.StockOpnameDetail;
import com.wms.ui.UITheme;
import com.wms.ui.component.RoundedButton;
import com.wms.util.AuditLogger;
import com.wms.util.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog untuk membuat transaksi Stock Opname baru. Alurnya: pilih lokasi
 * rak lalu klik "Muat Stok Lokasi" untuk menarik semua barang yang saat ini
 * tercatat pada lokasi tersebut (qty sistem). Petugas lalu mengisi qty hasil
 * hitung fisik untuk tiap barang sebelum menyimpan sebagai Draft. Selisih
 * baru diterapkan ke stok setelah transaksi diproses ("Proses") dari daftar.
 */
public class StockOpnameFormDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StockOpnameDAO opnameDAO = new StockOpnameDAO();
    private final StockDAO stockDAO = new StockDAO();
    private final LokasiRakDAO lokasiDAO = new LokasiRakDAO();

    private final JTextField tanggalField = new JTextField(LocalDate.now().format(DATE_FMT));
    private final JComboBox<LokasiRak> lokasiCombo = new JComboBox<>();
    private final JTextArea catatanArea = new JTextArea(2, 20);

    private final DefaultTableModel itemTableModel;
    private final JTable itemTable;
    private final List<StockOpnameDetail> items = new ArrayList<>();

    private boolean saved = false;

    public StockOpnameFormDialog(Window owner) {
        super(owner, "Buat Stock Opname", ModalityType.APPLICATION_MODAL);
        setSize(680, 620);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 12));
        getContentPane().setBackground(UITheme.BG);

        JPanel content = new JPanel();
        content.setBackground(UITheme.BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        content.add(buildHeaderCard());
        content.add(Box.createVerticalStrut(14));

        itemTableModel = new DefaultTableModel(new String[]{"Barang", "Qty Sistem", "Qty Fisik"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return column == 2; }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 2 ? Integer.class : String.class;
            }
        };
        itemTable = new JTable(itemTableModel);
        itemTable.setRowHeight(36);
        itemTable.setFont(UITheme.FONT_BASE);
        itemTable.getTableHeader().setFont(UITheme.FONT_LABEL);
        itemTable.getColumnModel().getColumn(1).setMaxWidth(100);
        itemTable.getColumnModel().getColumn(2).setMaxWidth(100);

        JLabel itemListTitle = new JLabel("Daftar Barang di Lokasi");
        itemListTitle.setFont(UITheme.FONT_LABEL);
        itemListTitle.setForeground(UITheme.TEXT_MUTED);
        itemListTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(itemListTitle);
        content.add(Box.createVerticalStrut(6));

        JScrollPane itemScroll = new JScrollPane(itemTable);
        itemScroll.setPreferredSize(new Dimension(600, 260));
        itemScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        itemScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        content.add(itemScroll);

        add(new JScrollPane(content), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        loadLokasiCombo();
    }

    private JPanel buildHeaderCard() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Informasi Stock Opname");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card.add(title);
        card.add(Box.createVerticalStrut(12));

        JPanel grid = new JPanel(new GridLayout(1, 2, 14, 0));
        grid.setOpaque(false);
        grid.add(labeledField("Tanggal (yyyy-MM-dd)", tanggalField));
        grid.add(labeledField("Lokasi Rak", lokasiCombo));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(grid);
        card.add(Box.createVerticalStrut(10));

        RoundedButton loadBtn = new RoundedButton("Muat Stok Lokasi", RoundedButton.Style.SECONDARY);
        loadBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loadBtn.addActionListener(e -> loadStockForLokasi());
        card.add(loadBtn);
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

    private void loadLokasiCombo() {
        try {
            lokasiCombo.removeAllItems();
            for (LokasiRak l : lokasiDAO.findAll("")) lokasiCombo.addItem(l);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void loadStockForLokasi() {
        LokasiRak lokasi = (LokasiRak) lokasiCombo.getSelectedItem();
        if (lokasi == null) {
            JOptionPane.showMessageDialog(this, "Pilih lokasi rak terlebih dahulu.",
                    "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            List<StockItem> stockItems = stockDAO.findByLokasi(lokasi.getId());
            items.clear();
            itemTableModel.setRowCount(0);

            if (stockItems.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Belum ada stok barang yang tercatat pada lokasi ini.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            for (StockItem s : stockItems) {
                StockOpnameDetail detail = new StockOpnameDetail();
                detail.setBarangId(s.getBarangId());
                detail.setBarangKode(s.getBarangKode());
                detail.setBarangNama(s.getBarangNama());
                detail.setSystemQty(s.getQty());
                detail.setActualQty(s.getQty());
                items.add(detail);

                itemTableModel.addRow(new Object[]{
                        s.getBarangNama() + " (" + s.getBarangKode() + ")",
                        s.getQty(), s.getQty()
                });
            }
        } catch (SQLException ex) {
            showError(ex);
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
        LokasiRak lokasi = (LokasiRak) lokasiCombo.getSelectedItem();
        if (lokasi == null) {
            JOptionPane.showMessageDialog(this, "Pilih lokasi rak terlebih dahulu.",
                    "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Muat stok lokasi terlebih dahulu sebelum menyimpan.",
                    "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Ambil nilai qty fisik terbaru dari tabel (user bisa mengedit sel langsung).
        for (int i = 0; i < items.size(); i++) {
            Object val = itemTableModel.getValueAt(i, 2);
            int actual = (val instanceof Integer) ? (Integer) val : items.get(i).getActualQty();
            items.get(i).setActualQty(actual);
        }

        StockOpname header = new StockOpname();
        header.setTanggal(tanggal);
        header.setLokasiRakId(lokasi.getId());
        header.setDibuatOleh(Session.getUsername());
        header.setCatatan(catatanArea.getText().trim());

        try {
            opnameDAO.insert(header, items);
            saved = true;

            AuditLogger.log(
                    "STOCK_OPNAME", "Buat Draft", header.getId(),
                    "Draft stock opname " + header.getNomor() + " dibuat dengan " + items.size()
                            + " item. Status: - -> DRAFT");

            JOptionPane.showMessageDialog(this,
                    "Transaksi " + header.getNomor() + " berhasil disimpan sebagai Draft.\n" +
                    "Klik \"Proses\" pada daftar transaksi untuk menerapkan selisih ke stok.",
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
}
