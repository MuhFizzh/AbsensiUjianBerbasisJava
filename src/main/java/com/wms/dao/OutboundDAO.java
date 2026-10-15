package com.wms.dao;

import com.wms.db.DatabaseConnection;
import com.wms.model.OutboundHeader;
import com.wms.model.OutboundItem;
import com.wms.util.CodeGenerator;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutboundDAO {

    private final StockDAO stockDAO = new StockDAO();

    public List<OutboundHeader> findAll(String keyword) throws SQLException {
        String sql = "SELECT o.*, c.nama AS customer_nama, " +
                "(SELECT COUNT(*) FROM outbound_detail d WHERE d.outbound_id = o.id) AS total_item " +
                "FROM outbound o LEFT JOIN customer c ON o.customer_id = c.id " +
                "WHERE o.nomor LIKE ? OR c.nama LIKE ? ORDER BY o.id DESC";
        List<OutboundHeader> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + (keyword == null ? "" : keyword) + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapHeader(rs));
            }
        }
        return result;
    }

    public OutboundHeader findById(int id) throws SQLException {
        String sql = "SELECT outbound.*, c.nama AS customer_nama, "
                + "(SELECT COUNT(*) FROM outbound_detail d WHERE d.outbound_id = outbound.id) AS total_item "
                + "FROM outbound outbound LEFT JOIN customer c ON outbound.customer_id = c.id "
                + "WHERE outbound.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapHeader(rs) : null;
            }
        }
    }

    /** N transaksi Outbound paling baru (dipakai di layar Dashboard). */
    public List<OutboundHeader> findRecent(int limit) throws SQLException {
        String sql = "SELECT o.*, c.nama AS customer_nama, " +
                "(SELECT COUNT(*) FROM outbound_detail d WHERE d.outbound_id = o.id) AS total_item " +
                "FROM outbound o LEFT JOIN customer c ON o.customer_id = c.id " +
                "ORDER BY o.id DESC LIMIT ?";
        List<OutboundHeader> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapHeader(rs));
            }
        }
        return result;
    }

    public List<OutboundItem> findItems(int outboundId) throws SQLException {
        String sql = "SELECT d.*, b.kode AS barang_kode, b.nama AS barang_nama, l.kode AS lokasi_kode " +
                "FROM outbound_detail d " +
                "JOIN barang b ON d.barang_id = b.id " +
                "JOIN lokasi_rak l ON d.lokasi_rak_id = l.id " +
                "WHERE d.outbound_id = ? ORDER BY d.id";
        List<OutboundItem> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, outboundId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapItem(rs));
            }
        }
        return result;
    }

    public void insert(OutboundHeader header, List<OutboundItem> items) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (items == null || items.isEmpty()) {
                    throw new SQLException("Outbound harus memiliki minimal satu item.");
                }

                // Validasi server-side sebelum draft dibuat. Jika ada dua baris
                // barang+lokasi yang sama, qty dijumlahkan agar tidak bisa
                // melewati stok hanya karena dibagi menjadi beberapa baris.
                Map<String, Integer> requested = new HashMap<>();
                Map<String, OutboundItem> representatives = new HashMap<>();

                for (OutboundItem item : items) {
                    if (item.getQty() <= 0) {
                        throw new SQLException("Qty outbound harus lebih besar dari 0.");
                    }

                    String key = item.getBarangId() + ":" + item.getLokasiRakId();
                    requested.merge(key, item.getQty(), Integer::sum);
                    representatives.putIfAbsent(key, item);
                }

                StringBuilder shortage = new StringBuilder();
                for (Map.Entry<String, Integer> entry : requested.entrySet()) {
                    OutboundItem item = representatives.get(entry.getKey());
                    int available = stockDAO.getQty(
                            conn, item.getBarangId(), item.getLokasiRakId());

                    if (entry.getValue() > available) {
                        shortage.append(String.format(
                                "- %s di %s: tersedia %d, diminta %d%n",
                                item.getBarangNama(), item.getLokasiKode(),
                                available, entry.getValue()));
                    }
                }

                if (shortage.length() > 0) {
                    throw new SQLException(
                            "Stok tidak mencukupi. Draft tidak disimpan:\n" + shortage);
                }

                String nomor = CodeGenerator.generateNextCode(conn, "outbound", "nomor", "OUT-", 4);
                String sql = "INSERT INTO outbound (nomor, tanggal, customer_id, catatan, status) VALUES (?,?,?,?,'DRAFT')";
                int outboundId;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nomor);
                    ps.setDate(2, Date.valueOf(header.getTanggal()));
                    if (header.getCustomerId() == null) ps.setNull(3, java.sql.Types.INTEGER);
                    else ps.setInt(3, header.getCustomerId());
                    ps.setString(4, header.getCatatan());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        outboundId = keys.getInt(1);
                    }
                }
                String itemSql = "INSERT INTO outbound_detail (outbound_id, barang_id, lokasi_rak_id, qty) VALUES (?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                    for (OutboundItem item : items) {
                        ps.setInt(1, outboundId);
                        ps.setInt(2, item.getBarangId());
                        ps.setInt(3, item.getLokasiRakId());
                        ps.setInt(4, item.getQty());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                header.setId(outboundId);
                header.setNomor(nomor);
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Mengambil nomor transaksi langsung dari database berdasarkan id.
     * Dipakai saat mencatat aktivitas kerja (ActivityLogger), supaya
     * tidak bergantung pada isi tabel UI yang bisa saja sedang
     * terfilter/tidak sinkron saat tombol aksi ditekan.
     */
    public String findNomorById(int outboundId) throws SQLException {
        String sql = "SELECT nomor FROM outbound WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, outboundId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("nomor") : "-";
            }
        }
    }

    /**
     * Jumlah transaksi berstatus DRAFT - dipakai untuk badge notifikasi
     * "menunggu konfirmasi admin" di sidebar.
     */
    public int countDraft() throws SQLException {
        String sql = "SELECT COUNT(*) FROM outbound WHERE status = 'DRAFT'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Finalisasi transaksi Outbound: memvalidasi stok tersedia untuk setiap item
     * (jika ada satu saja yang kurang, seluruh transaksi dibatalkan/rollback dan
     * pesan error berisi rincian barang & lokasi yang kurang), lalu mengurangi
     * qty stok dan mengubah status menjadi COMPLETED.
     */
    public void complete(int outboundId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<OutboundItem> items = findItemsWithConnection(conn, outboundId);

                Map<String, Integer> requested = new HashMap<>();
                Map<String, OutboundItem> representatives = new HashMap<>();

                for (OutboundItem item : items) {
                    String key = item.getBarangId() + ":" + item.getLokasiRakId();
                    requested.merge(key, item.getQty(), Integer::sum);
                    representatives.putIfAbsent(key, item);
                }

                StringBuilder shortage = new StringBuilder();
                for (Map.Entry<String, Integer> entry : requested.entrySet()) {
                    OutboundItem item = representatives.get(entry.getKey());
                    int available = stockDAO.getQty(
                            conn, item.getBarangId(), item.getLokasiRakId());
                    if (available < entry.getValue()) {
                        shortage.append(String.format(
                                "- %s di %s: tersedia %d, diminta %d%n",
                                item.getBarangNama(), item.getLokasiKode(),
                                available, entry.getValue()));
                    }
                }
                if (shortage.length() > 0) {
                    throw new SQLException("Stok tidak mencukupi untuk item berikut:\n" + shortage);
                }

                for (OutboundItem item : items) {
                    stockDAO.adjustQty(conn, item.getBarangId(), item.getLokasiRakId(), -item.getQty());
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE outbound SET status='COMPLETED' WHERE id=? AND status='DRAFT'")) {
                    ps.setInt(1, outboundId);
                    int updated = ps.executeUpdate();
                    if (updated == 0) {
                        throw new SQLException("Transaksi sudah diproses sebelumnya atau tidak ditemukan.");
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
    
    public void voidCompleted(int id)
        throws SQLException {

    try (Connection conn =
                 DatabaseConnection.getConnection()) {

        conn.setAutoCommit(false);

        try {

            List<OutboundItem> items =
                    findItemsWithConnection(
                            conn,
                            id);

            if (items.isEmpty()) {
                throw new SQLException(
                        "Transaksi tidak ditemukan.");
            }

            String status;

            try (PreparedStatement ps =
                         conn.prepareStatement(
                                 "SELECT status " +
                                 "FROM outbound " +
                                 "WHERE id=? FOR UPDATE")) {

                ps.setInt(1, id);

                try (ResultSet rs =
                             ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new SQLException(
                                "Transaksi tidak ditemukan.");
                    }

                    status = rs.getString(1);
                }
            }

            if (!"COMPLETED".equals(status)) {
                throw new SQLException(
                        "Hanya transaksi COMPLETED " +
                        "yang dapat di-void.");
            }

            for (OutboundItem item : items) {

                stockDAO.adjustQty(
                        conn,
                        item.getBarangId(),
                        item.getLokasiRakId(),
                        item.getQty());
            }

            try (PreparedStatement ps =
                         conn.prepareStatement(
                                 "UPDATE outbound " +
                                 "SET status='CANCELLED' " +
                                 "WHERE id=? " +
                                 "AND status='COMPLETED'")) {

                ps.setInt(1, id);

                if (ps.executeUpdate() == 0) {
                    throw new SQLException(
                            "Transaksi sudah berubah status.");
                }
            }

            conn.commit();

        } catch (SQLException ex) {

            conn.rollback();
            throw ex;

        } finally {

            conn.setAutoCommit(true);
        }
    }
}

    public void deleteDraft(int outboundId) throws SQLException {
        String sql = "DELETE FROM outbound WHERE id=? AND status='DRAFT'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, outboundId);
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                throw new SQLException("Transaksi tidak bisa dihapus (bukan status Draft atau tidak ditemukan).");
            }
        }
    }

    private List<OutboundItem> findItemsWithConnection(Connection conn, int outboundId) throws SQLException {
        String sql = "SELECT d.*, b.kode AS barang_kode, b.nama AS barang_nama, l.kode AS lokasi_kode " +
                "FROM outbound_detail d " +
                "JOIN barang b ON d.barang_id = b.id " +
                "JOIN lokasi_rak l ON d.lokasi_rak_id = l.id " +
                "WHERE d.outbound_id = ?";
        List<OutboundItem> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, outboundId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapItem(rs));
            }
        }
        return result;
    }

    private OutboundHeader mapHeader(ResultSet rs) throws SQLException {
        OutboundHeader h = new OutboundHeader();
        h.setId(rs.getInt("id"));
        h.setNomor(rs.getString("nomor"));
        h.setTanggal(rs.getDate("tanggal").toLocalDate());
        int customerId = rs.getInt("customer_id");
        h.setCustomerId(rs.wasNull() ? null : customerId);
        h.setCustomerNama(rs.getString("customer_nama"));
        h.setCatatan(rs.getString("catatan"));
        h.setStatus(rs.getString("status"));
        h.setTotalItem(rs.getInt("total_item"));
        return h;
    }

    private OutboundItem mapItem(ResultSet rs) throws SQLException {
        OutboundItem i = new OutboundItem();
        i.setId(rs.getInt("id"));
        i.setOutboundId(rs.getInt("outbound_id"));
        i.setBarangId(rs.getInt("barang_id"));
        i.setBarangKode(rs.getString("barang_kode"));
        i.setBarangNama(rs.getString("barang_nama"));
        i.setLokasiRakId(rs.getInt("lokasi_rak_id"));
        i.setLokasiKode(rs.getString("lokasi_kode"));
        i.setQty(rs.getInt("qty"));
        return i;
    }
}
