package com.wms.ui.panel;

import com.wms.dao.BarangDAO;
import com.wms.dao.LokasiRakDAO;
import com.wms.dao.PermintaanDAO;
import com.wms.dao.StockDAO;
import com.wms.model.StockItem;
import com.wms.util.AuditLogger;
import com.wms.util.Session;
import com.wms.model.Barang;
import com.wms.model.LokasiRak;
import com.wms.model.Permintaan;
import com.wms.ui.UITheme;
import com.wms.ui.component.TablePagination;
import com.wms.ui.component.PlaceholderTextField;
import com.wms.ui.component.TableCopyEnabler;
import com.wms.ui.component.RequestStatusBadgeCellRenderer;
import com.wms.ui.component.RoundedButton;
import com.wms.ui.dialog.RiwayatPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layar "Create Request": staff mengajukan permintaan (pindah lokasi stok,
 * permintaan barang baru, atau lain-lain) yang nanti diproses di layar
 * Approval. Modul ini berdiri sendiri, tidak menyentuh data Inbound/Outbound.
 */
public class CreateRequestPanel extends JPanel implements RefreshablePanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final PermintaanDAO dao = new PermintaanDAO();
    private final BarangDAO barangDAO = new BarangDAO();
    private final LokasiRakDAO lokasiDAO = new LokasiRakDAO();
    private final StockDAO stockDAO = new StockDAO();

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TablePagination pagination;
    private final PlaceholderTextField searchField;
    private final Map<Integer, Permintaan> byId = new HashMap<>();

    private final JComboBox<String> jenisCombo =
            new JComboBox<>(new String[]{"Pindah Lokasi Stok", "Permintaan Barang Baru", "Lainnya"});
    private final JComboBox<Barang> barangCombo = new JComboBox<>();
    private final JSpinner jumlahSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
    private final JComboBox<LokasiRak> lokasiAsalCombo = new JComboBox<>();
    private final JComboBox<LokasiRak> lokasiTujuanCombo = new JComboBox<>();
            
    private final JTextField diajukanOlehField =
        new JTextField(
                Session.getUsername() == null
                        ? ""
                        : Session.getUsername());
    
    
    private final JTextArea keteranganArea = new JTextArea(3, 20);

    public CreateRequestPanel() {
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
            
            diajukanOlehField.setEditable(false);
        
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        searchField = new PlaceholderTextField("Cari nomor atau pemohon");
        searchField.setPreferredSize(new Dimension(320, 36));
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(searchField);
        toolbar.add(searchWrap, BorderLayout.WEST);
        add(toolbar, BorderLayout.NORTH);

        String[] columns = {"No. Permintaan", "Tanggal", "Jenis", "Barang", "Jumlah", "Pemohon", "Status", "Aksi"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 7; }
        };
        table = new JTable(tableModel);
        TableCopyEnabler.enable(table);
        table.setRowHeight(42);
        table.setFont(UITheme.FONT_BASE);
        table.setShowGrid(false);
        table.getTableHeader().setFont(UITheme.FONT_LABEL);
        table.getTableHeader().setBackground(UITheme.TABLE_HEAD_BG);
        table.getTableHeader().setForeground(UITheme.TEXT_MUTED);
        table.setSelectionBackground(UITheme.ACCENT_LIGHT);
        table.setSelectionForeground(UITheme.TEXT);
        table.getColumnModel().getColumn(6).setCellRenderer(new RequestStatusBadgeCellRenderer());
        table.getColumnModel().getColumn(6).setMaxWidth(120);
        table.getColumnModel().getColumn(7).setCellRenderer(new RiwayatCellRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new RiwayatCellEditor());
        table.getColumnModel().getColumn(7).setMinWidth(100);
        table.getColumnModel().getColumn(7).setMaxWidth(110);
        table.getColumnModel().getColumn(0).setMaxWidth(120);
        table.getColumnModel().getColumn(1).setMaxWidth(100);

        pagination = new TablePagination(tableModel, 10);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel formCard = buildFormCard();

        // Area utama dibuat BorderLayout agar kartu form dan tabel benar-benar
        // menggunakan lebar konten yang tersedia. Sebelumnya BoxLayout +
        // maximum width membuat seluruh isi terlihat bergeser ke tengah/kanan.
        JPanel historyPanel = new JPanel(new BorderLayout(0, 10));
        historyPanel.setOpaque(false);

        JLabel riwayatLabel = new JLabel("Riwayat Permintaan Saya");
        riwayatLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        historyPanel.add(riwayatLabel, BorderLayout.NORTH);
        historyPanel.add(scrollPane, BorderLayout.CENTER);
        historyPanel.add(pagination, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        center.add(formCard, BorderLayout.NORTH);
        center.add(historyPanel, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        searchField.addActionListener(e -> reload());
        jenisCombo.addActionListener(e -> updateFieldAvailability());
        barangCombo.addActionListener(e -> {
            updateLokasiAsal();
            updateLokasiTujuan();
        });
        updateFieldAvailability();

        loadCombos();
        reload();
    }

    private JPanel buildFormCard() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)));
        card.setLayout(new BorderLayout(0, 16));
        // Jangan batasi lebar kartu. Form mengikuti lebar area konten agar
        // dua kolom input lebih seimbang dan tidak menyisakan ruang kosong.
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 520));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Ajukan Permintaan Baru");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("Isi data permintaan kemudian kirim untuk diproses pada menu Approval.");
        subtitle.setFont(UITheme.FONT_SUBTITLE);
        subtitle.setForeground(UITheme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(subtitle);
        card.add(titleBox, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(3, 2, 16, 12));
        grid.setOpaque(false);
        grid.add(field("Jenis permintaan", jenisCombo));
        grid.add(field("Barang", barangCombo));
        grid.add(field("Jumlah", jumlahSpinner));
        grid.add(field("Diajukan oleh", diajukanOlehField));
        grid.add(field("Lokasi asal", lokasiAsalCombo));
        grid.add(field("Lokasi tujuan", lokasiTujuanCombo));

        keteranganArea.setLineWrap(true);
        keteranganArea.setWrapStyleWord(true);
        keteranganArea.setFont(UITheme.FONT_BASE);
        JScrollPane ketScroll = new JScrollPane(keteranganArea);
        ketScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        JPanel ketField = new JPanel(new BorderLayout(0, 5));
        ketField.setOpaque(false);
        ketField.setPreferredSize(new Dimension(0, 78));
        JLabel ketLabel = new JLabel("Keterangan");
        ketLabel.setFont(UITheme.FONT_LABEL);
        ketLabel.setForeground(UITheme.TEXT_MUTED);
        ketField.add(ketLabel, BorderLayout.NORTH);
        ketField.add(ketScroll, BorderLayout.CENTER);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        ketField.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(grid);
        body.add(Box.createVerticalStrut(12));
        body.add(ketField);
        card.add(body, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        RoundedButton submitBtn = new RoundedButton("Ajukan Permintaan", RoundedButton.Style.PRIMARY);
        RoundedButton resetBtn = new RoundedButton("Reset", RoundedButton.Style.SECONDARY);
        submitBtn.addActionListener(e -> submit());
        resetBtn.addActionListener(e -> resetForm());
        actions.add(submitBtn);
        actions.add(resetBtn);
        card.add(actions, BorderLayout.SOUTH);

        return card;
    }

    private JPanel field(String label, JComponent input) {
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

    /** Field yang relevan bergantung pada jenis permintaan yang dipilih. */
    private void updateFieldAvailability() {
        String jenis = jenisText();
        boolean butuhBarang = jenis.equals("PINDAH_LOKASI") || jenis.equals("PERMINTAAN_BARANG");
        boolean butuhLokasi = jenis.equals("PINDAH_LOKASI");
        barangCombo.setEnabled(butuhBarang);
        jumlahSpinner.setEnabled(butuhBarang);
        lokasiAsalCombo.setEnabled(false);
        lokasiTujuanCombo.setEnabled(butuhLokasi);
    }

    private String jenisText() {
        String selected = (String) jenisCombo.getSelectedItem();
        if ("Pindah Lokasi Stok".equals(selected)) return "PINDAH_LOKASI";
        if ("Permintaan Barang Baru".equals(selected)) return "PERMINTAAN_BARANG";
        return "LAINNYA";
    }

    private void loadCombos() {
    try {
        barangCombo.removeAllItems();

        for (Barang b : barangDAO.findAll("")) {
            barangCombo.addItem(b);
        }

        lokasiAsalCombo.removeAllItems();
        lokasiTujuanCombo.removeAllItems();

        for (LokasiRak l : lokasiDAO.findAll("")) {
            lokasiTujuanCombo.addItem(l);
        }

        updateLokasiAsal();
        updateLokasiTujuan();

    } catch (SQLException ex) {
        showDbError(ex);
    }
}
    
    private void updateLokasiAsal() {
    try {
        lokasiAsalCombo.removeAllItems();

        Barang barang = (Barang) barangCombo.getSelectedItem();

        if (barang == null) {
            return;
        }

        List<StockItem> stockList = stockDAO.findByBarang(barang.getId());

        for (StockItem stock : stockList) {
            LokasiRak lokasi = new LokasiRak();
            lokasi.setId(stock.getLokasiRakId());
            lokasi.setKode(stock.getLokasiKode());
            lokasi.setArea(stock.getLokasiArea());

            lokasiAsalCombo.addItem(lokasi);
        }

        if (lokasiAsalCombo.getItemCount() > 0) {
            lokasiAsalCombo.setSelectedIndex(0);
        }

    } catch (SQLException ex) {
        showDbError(ex);
    }
}

    private void reload() {
        try {
            List<Permintaan> list = dao.findAll(searchField.getText().trim());
            List<Object[]> rows = new java.util.ArrayList<>();
            byId.clear();
            for (Permintaan p : list) {
                byId.put(p.getId(), p);
                rows.add(new Object[]{
                        p.getNomor(),
                        p.getTanggal() == null ? "-" : p.getTanggal().format(DATE_FMT),
                        p.getJenisLabel(),
                        p.getBarangNama() == null ? "-" : p.getBarangNama(),
                        p.getJumlah() == null ? "-" : p.getJumlah(),
                        p.getDiajukanOleh(),
                        p.getStatus(),
                        p.getId()
                });
            }
            pagination.setData(rows);
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }
    
    private void updateLokasiTujuan() {
    try {
        lokasiTujuanCombo.removeAllItems();

        LokasiRak asal =
                (LokasiRak) lokasiAsalCombo.getSelectedItem();

        for (LokasiRak l : lokasiDAO.findAll("")) {

            if (asal == null || l.getId() != asal.getId()) {
                lokasiTujuanCombo.addItem(l);
            }
        }

        if (lokasiTujuanCombo.getItemCount() > 0) {
            lokasiTujuanCombo.setSelectedIndex(0);
        }

    } catch (SQLException ex) {
        showDbError(ex);
    }
}

    private void resetForm() {
        jenisCombo.setSelectedIndex(0);

        if (barangCombo.getItemCount() > 0) {
            barangCombo.setSelectedIndex(0);
        }

        jumlahSpinner.setValue(1);

        if (lokasiTujuanCombo.getItemCount() > 0) {
            lokasiTujuanCombo.setSelectedIndex(0);
        }

        diajukanOlehField.setText(
        Session.getUsername() == null
                ? ""
                : Session.getUsername());

        keteranganArea.setText("");

        updateLokasiAsal();
        updateFieldAvailability();
    }

    private void submit() {
        String jenis = jenisText();
        String diajukanOleh = Session.getUsername();
        if (diajukanOleh == null || diajukanOleh.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Tidak ada pengguna yang sedang login.",
                    "Validasi",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Barang selectedBarang = (Barang) barangCombo.getSelectedItem();
        LokasiRak asal = (LokasiRak) lokasiAsalCombo.getSelectedItem();
        LokasiRak tujuan = (LokasiRak) lokasiTujuanCombo.getSelectedItem();

        if (jenis.equals("PINDAH_LOKASI")) {
            if (selectedBarang == null || asal == null || tujuan == null) {
                JOptionPane.showMessageDialog(this, "Barang, lokasi asal, dan lokasi tujuan wajib dipilih.",
                        "Validasi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (asal.getId() == tujuan.getId()) {
                JOptionPane.showMessageDialog(this, "Lokasi asal dan lokasi tujuan tidak boleh sama.",
                        "Validasi", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else if (jenis.equals("PERMINTAAN_BARANG")) {
            if (selectedBarang == null) {
                JOptionPane.showMessageDialog(this, "Barang wajib dipilih.",
                        "Validasi", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            if (keteranganArea.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Keterangan wajib diisi untuk permintaan jenis Lainnya.",
                        "Validasi", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        Permintaan p = new Permintaan();
        p.setTanggal(LocalDate.now());
        p.setJenis(jenis);
        p.setDiajukanOleh(diajukanOleh);
        p.setKeterangan(keteranganArea.getText().trim());

        boolean butuhBarang = jenis.equals("PINDAH_LOKASI") || jenis.equals("PERMINTAAN_BARANG");
        if (butuhBarang && selectedBarang != null) {
            p.setBarangId(selectedBarang.getId());
            p.setJumlah((Integer) jumlahSpinner.getValue());
        }
        if (jenis.equals("PINDAH_LOKASI") && asal != null && tujuan != null) {
            p.setLokasiAsalId(asal.getId());
            p.setLokasiTujuanId(tujuan.getId());
        }

        try {
            dao.insert(p);
            AuditLogger.log(
                    "PERMINTAAN",
                    "Ajukan Permintaan",
                    p.getId(),
                    "Permintaan " + p.getNomor() + " diajukan, menunggu approval. Status: - -> MENUNGGU"
            );
            JOptionPane.showMessageDialog(this,
                    "Permintaan " + p.getNomor() + " berhasil diajukan dan menunggu approval.",
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            resetForm();
            reload();
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void showDbError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Terjadi kesalahan database:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void refresh() {
        loadCombos();
        reload();
    }

    /** Renderer kolom Aksi: menampilkan label "Riwayat" (tampilan tombol) saat sel tidak sedang diedit. */
    private class RiwayatCellRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private final RoundedButton label = new RoundedButton("Riwayat", RoundedButton.Style.SECONDARY);

        RiwayatCellRenderer() {
            super(new FlowLayout(FlowLayout.CENTER, 4, 4));
            setOpaque(true);
            label.setPreferredSize(new Dimension(78, 28));
            add(label);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            setBackground(Color.WHITE);
            return this;
        }
    }

    /** Editor kolom Aksi: tombol "Riwayat" yang benar-benar bisa diklik, membuka riwayat permintaan ini. */
    private class RiwayatCellEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JPanel panel;
        private int currentId;

        RiwayatCellEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
            panel.setOpaque(true);

            RoundedButton riwayatBtn = new RoundedButton("Riwayat", RoundedButton.Style.SECONDARY);
            riwayatBtn.setPreferredSize(new Dimension(78, 28));
            riwayatBtn.addActionListener(e -> {
                fireEditingStopped();
                Permintaan p = byId.get(currentId);
                String nomor = p == null ? "-" : p.getNomor();
                JOptionPane.showMessageDialog(
                        CreateRequestPanel.this,
                        RiwayatPanel.build("PERMINTAAN", currentId),
                        "Riwayat Permintaan - " + nomor,
                        JOptionPane.PLAIN_MESSAGE);
            });

            panel.add(riwayatBtn);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentId = value == null ? -1 : (Integer) value;
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return currentId;
        }
    }
}
