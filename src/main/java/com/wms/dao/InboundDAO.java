package com.wms.dao;

import com.wms.db.DatabaseConnection;
import com.wms.model.InboundHeader;
import com.wms.model.InboundItem;
import com.wms.util.CodeGenerator;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class InboundDAO {

    private final StockDAO stockDAO = new StockDAO();

    public List<InboundHeader> findAll(String keyword) throws SQLException {
        String sql = "SELECT i.*, s.nama AS supplier_nama, " +
                "(SELECT COUNT(*) FROM inbound_detail d WHERE d.inbound_id = i.id) AS total_item " +
                "FROM inbound i LEFT JOIN supplier s ON i.supplier_id = s.id " +
                "WHERE i.nomor LIKE ? OR s.nama LIKE ? ORDER BY i.id DESC";
        List<InboundHeader> result = new ArrayList<>();
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

    public InboundHeader findById(int id) throws SQLException {
        String sql = "SELECT inbound.*, s.nama AS supplier_nama, "
                + "(SELECT COUNT(*) FROM inbound_detail d WHERE d.inbound_id = inbound.id) AS total_item "
                + "FROM inbound inbound LEFT JOIN supplier s ON inbound.supplier_id = s.id "
                + "WHERE inbound.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapHeader(rs) : null;
            }
        }
    }

    /** N transaksi Inbound paling baru (dipakai di layar Dashboard). */
    public List<InboundHeader> findRecent(int limit) throws SQLException {
        String sql = "SELECT i.*, s.nama AS supplier_nama, " +
                "(SELECT COUNT(*) FROM inbound_detail d WHERE d.inbound_id = i.id) AS total_item " +
                "FROM inbound i LEFT JOIN supplier s ON i.supplier_id = s.id " +
                "ORDER BY i.id DESC LIMIT ?";
        List<InboundHeader> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapHeader(rs));
            }
        }
        return result;
    }

    public List<InboundItem> findItems(int inboundId) throws SQLException {
        String sql = "SELECT d.*, b.kode AS barang_kode, b.nama AS barang_nama, l.kode AS lokasi_kode " +
                "FROM inbound_detail d " +
                "JOIN barang b ON d.barang_id = b.id " +
                "JOIN lokasi_rak l ON d.lokasi_rak_id = l.id " +
                "WHERE d.inbound_id = ? ORDER BY d.id";
        List<InboundItem> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, inboundId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapItem(rs));
            }
        }
        return result;
    }

    /** Membuat transaksi Inbound baru berstatus DRAFT beserta item-itemnya (1 transaksi DB). */
    public void insert(InboundHeader header, List<InboundItem> items) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String nomor = CodeGenerator.generateNextCode(conn, "inbound", "nomor", "IN-", 4);
                String sql = "INSERT INTO inbound (nomor, tanggal, supplier_id, catatan, status) VALUES (?,?,?,?,'DRAFT')";
                int inboundId;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nomor);
                    ps.setDate(2, Date.valueOf(header.getTanggal()));
                    if (header.getSupplierId() == null) ps.setNull(3, java.sql.Types.INTEGER);
                    else ps.setInt(3, header.getSupplierId());
                    ps.setString(4, header.getCatatan());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        inboundId = keys.getInt(1);
                    }
                }
                String itemSql = "INSERT INTO inbound_detail (inbound_id, barang_id, lokasi_rak_id, qty) VALUES (?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                    for (InboundItem item : items) {
                        ps.setInt(1, inboundId);
                        ps.setInt(2, item.getBarangId());
                        ps.setInt(3, item.getLokasiRakId());
                        ps.setInt(4, item.getQty());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                header.setId(inboundId);
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
    public String findNomorById(int inboundId) throws SQLException {
        String sql = "SELECT nomor FROM inbound WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, inboundId);
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
        String sql = "SELECT COUNT(*) FROM inbound WHERE status = 'DRAFT'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Finalisasi transaksi Inbound: menambah qty stok untuk setiap item, lalu
     * mengubah status menjadi COMPLETED. Hanya bisa dilakukan pada transaksi berstatus DRAFT.
     */
    public void complete(int inboundId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<InboundItem> items = findItemsWithConnection(conn, inboundId);
                for (InboundItem item : items) {
                    stockDAO.adjustQty(conn, item.getBarangId(), item.getLokasiRakId(), item.getQty());
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE inbound SET status='COMPLETED' WHERE id=? AND status='DRAFT'")) {
                    ps.setInt(1, inboundId);
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

                List<InboundItem> items =
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
                                     "FROM inbound " +
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

                for (InboundItem item : items) {

                    int available =
                            lockedQty(
                                    conn,
                                    item.getBarangId(),
                                    item.getLokasiRakId());

                    if (available < item.getQty()) {

                        throw new SQLException(
                                "Stok saat ini tidak cukup " +
                                "untuk membatalkan inbound.");
                    }

                    stockDAO.adjustQty(
                            conn,
                            item.getBarangId(),
                            item.getLokasiRakId(),
                            -item.getQty());
                }

                try (PreparedStatement ps =
                             conn.prepareStatement(
                                     "UPDATE inbound " +
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
    
    private int lockedQty(
        Connection conn,
        int barangId,
        int lokasiRakId)
        throws SQLException {

    String sql =
            "SELECT qty FROM stock " +
            "WHERE barang_id=? " +
            "AND lokasi_rak_id=? " +
            "FOR UPDATE";

    try (PreparedStatement ps =
                 conn.prepareStatement(sql)) {

        ps.setInt(1, barangId);
        ps.setInt(2, lokasiRakId);

        try (ResultSet rs =
                     ps.executeQuery()) {

            return rs.next()
                    ? rs.getInt(1)
                    : 0;
        }
    }
}
    
    /** Menghapus transaksi Inbound yang masih berstatus DRAFT (item ikut terhapus via ON DELETE CASCADE). */
    public void deleteDraft(int inboundId) throws SQLException {
        String sql = "DELETE FROM inbound WHERE id=? AND status='DRAFT'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, inboundId);
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                throw new SQLException("Transaksi tidak bisa dihapus (bukan status Draft atau tidak ditemukan).");
            }
        }
    }

    private List<InboundItem> findItemsWithConnection(Connection conn, int inboundId) throws SQLException {
        String sql = "SELECT d.*, b.kode AS barang_kode, b.nama AS barang_nama, l.kode AS lokasi_kode " +
                "FROM inbound_detail d " +
                "JOIN barang b ON d.barang_id = b.id " +
                "JOIN lokasi_rak l ON d.lokasi_rak_id = l.id " +
                "WHERE d.inbound_id = ?";
        List<InboundItem> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, inboundId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapItem(rs));
            }
        }
        return result;
    }

    private InboundHeader mapHeader(ResultSet rs) throws SQLException {
        InboundHeader h = new InboundHeader();
        h.setId(rs.getInt("id"));
        h.setNomor(rs.getString("nomor"));
        h.setTanggal(rs.getDate("tanggal").toLocalDate());
        int supplierId = rs.getInt("supplier_id");
        h.setSupplierId(rs.wasNull() ? null : supplierId);
        h.setSupplierNama(rs.getString("supplier_nama"));
        h.setCatatan(rs.getString("catatan"));
        h.setStatus(rs.getString("status"));
        h.setTotalItem(rs.getInt("total_item"));
        return h;
    }

    private InboundItem mapItem(ResultSet rs) throws SQLException {
        InboundItem i = new InboundItem();
        i.setId(rs.getInt("id"));
        i.setInboundId(rs.getInt("inbound_id"));
        i.setBarangId(rs.getInt("barang_id"));
        i.setBarangKode(rs.getString("barang_kode"));
        i.setBarangNama(rs.getString("barang_nama"));
        i.setLokasiRakId(rs.getInt("lokasi_rak_id"));
        i.setLokasiKode(rs.getString("lokasi_kode"));
        i.setQty(rs.getInt("qty"));
        return i;
    }
}
