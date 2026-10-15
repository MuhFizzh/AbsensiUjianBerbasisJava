package com.wms.dao;

import com.wms.db.DatabaseConnection;
import com.wms.model.StockOpname;
import com.wms.model.StockOpnameDetail;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockOpnameDAO {

    public List<StockOpname> findAll(String keyword) throws SQLException {

        String sql =
                "SELECT so.id, so.nomor, so.tanggal, " +
                "so.lokasi_rak_id, lr.kode AS lokasi_kode, " +
                "so.dibuat_oleh, so.status, so.catatan, " +
                "COUNT(d.id) AS total_item, " +
                "COALESCE(SUM(d.selisih), 0) AS total_selisih " +
                "FROM stock_opname so " +
                "JOIN lokasi_rak lr ON lr.id = so.lokasi_rak_id " +
                "LEFT JOIN stock_opname_detail d ON d.opname_id = so.id " +
                "WHERE so.nomor LIKE ? " +
                "OR lr.kode LIKE ? " +
                "OR so.dibuat_oleh LIKE ? " +
                "GROUP BY so.id " +
                "ORDER BY so.id DESC";

        List<StockOpname> result = new ArrayList<>();

        String like = "%" + (keyword == null ? "" : keyword) + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    StockOpname o = new StockOpname();

                    o.setId(rs.getInt("id"));
                    o.setNomor(rs.getString("nomor"));

                    Date tanggal = rs.getDate("tanggal");
                    if (tanggal != null) {
                        o.setTanggal(tanggal.toLocalDate());
                    }

                    o.setLokasiRakId(
                            rs.getInt("lokasi_rak_id"));

                    o.setLokasiKode(
                            rs.getString("lokasi_kode"));

                    o.setDibuatOleh(
                            rs.getString("dibuat_oleh"));

                    o.setStatus(
                            rs.getString("status"));

                    o.setCatatan(
                            rs.getString("catatan"));

                    o.setTotalItem(
                            rs.getInt("total_item"));

                    o.setTotalSelisih(
                            rs.getInt("total_selisih"));

                    result.add(o);
                }
            }
        }

        return result;
    }


    public List<StockOpnameDetail> findItems(int opnameId)
            throws SQLException {

        String sql =
                "SELECT d.id, d.opname_id, d.barang_id, " +
                "b.kode AS barang_kode, " +
                "b.nama AS barang_nama, " +
                "d.system_qty, " +
                "d.counted_qty, " +
                "d.selisih " +
                "FROM stock_opname_detail d " +
                "JOIN barang b ON b.id = d.barang_id " +
                "WHERE d.opname_id = ? " +
                "ORDER BY b.nama";

        List<StockOpnameDetail> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, opnameId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    StockOpnameDetail d =
                            new StockOpnameDetail();

                    d.setId(rs.getInt("id"));

                    d.setStockOpnameId(
                            rs.getInt("opname_id"));

                    d.setBarangId(
                            rs.getInt("barang_id"));

                    d.setBarangKode(
                            rs.getString("barang_kode"));

                    d.setBarangNama(
                            rs.getString("barang_nama"));

                    d.setSystemQty(
                            rs.getInt("system_qty"));

                    d.setActualQty(
                            rs.getInt("counted_qty"));

                    d.setAdjustmentQty(
                            rs.getInt("selisih"));

                    result.add(d);
                }
            }
        }

        return result;
    }


    public void insert(
            StockOpname header,
            List<StockOpnameDetail> items) throws SQLException {

        String headerSql =
                "INSERT INTO stock_opname " +
                "(nomor, tanggal, lokasi_rak_id, dibuat_oleh, catatan) " +
                "VALUES (?, ?, ?, ?, ?)";

        String detailSql =
                "INSERT INTO stock_opname_detail " +
                "(opname_id, barang_id, system_qty, counted_qty, selisih) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn =
                     DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                String nomor =
                        generateNomor(conn);

                header.setNomor(nomor);

                int opnameId;

                try (PreparedStatement ps =
                             conn.prepareStatement(
                                     headerSql,
                                     Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, nomor);
                    ps.setDate(
                            2,
                            Date.valueOf(header.getTanggal()));
                    ps.setInt(
                            3,
                            header.getLokasiRakId());
                    ps.setString(
                            4,
                            header.getDibuatOleh());
                    ps.setString(
                            5,
                            header.getCatatan());

                    ps.executeUpdate();

                    try (ResultSet rs =
                                 ps.getGeneratedKeys()) {

                        if (!rs.next()) {
                            throw new SQLException(
                                    "Gagal mendapatkan ID Stock Opname.");
                        }

                        opnameId = rs.getInt(1);
                    }
                }

                header.setId(opnameId);

                try (PreparedStatement ps =
                             conn.prepareStatement(detailSql)) {

                    for (StockOpnameDetail item : items) {

                        int actual =
                                item.getActualQty();

                        int system =
                                item.getSystemQty();

                        int selisih =
                                actual - system;

                        item.setAdjustmentQty(selisih);

                        ps.setInt(1, opnameId);
                        ps.setInt(2, item.getBarangId());
                        ps.setInt(3, system);
                        ps.setInt(4, actual);
                        ps.setInt(5, selisih);

                        ps.addBatch();
                    }

                    ps.executeBatch();
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


    private String generateNomor(Connection conn)
            throws SQLException {

        String prefix = "SO-" +
                new java.text.SimpleDateFormat(
                        "yyyyMMdd").format(
                        new java.util.Date()) +
                "-";

        String sql =
                "SELECT nomor FROM stock_opname " +
                "WHERE nomor LIKE ? " +
                "ORDER BY id DESC LIMIT 1";

        try (PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setString(1, prefix + "%");

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (rs.next()) {

                    String last =
                            rs.getString("nomor");

                    int number =
                            Integer.parseInt(
                                    last.substring(
                                            last.lastIndexOf('-') + 1));

                    return String.format(
                            "%s%03d",
                            prefix,
                            number + 1);
                }
            }
        }

        return prefix + "001";
    }


    /**
     * Mengambil nomor transaksi langsung dari database berdasarkan id.
     * Dipakai saat mencatat aktivitas kerja (ActivityLogger), supaya
     * tidak bergantung pada isi tabel UI yang bisa saja sedang
     * terfilter/tidak sinkron saat tombol aksi ditekan.
     */
    public String findNomorById(int opnameId) throws SQLException {
        String sql = "SELECT nomor FROM stock_opname WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, opnameId);
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
        String sql = "SELECT COUNT(*) FROM stock_opname WHERE status = 'DRAFT'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void complete(int opnameId)
            throws SQLException {

        String selectSql =
                "SELECT lokasi_rak_id " +
                "FROM stock_opname " +
                "WHERE id = ? AND status = 'DRAFT'";

        String detailSql =
                "SELECT barang_id, system_qty, " +
                "counted_qty, selisih " +
                "FROM stock_opname_detail " +
                "WHERE opname_id = ?";

        String updateStockSql =
                "INSERT INTO stock " +
                "(barang_id, lokasi_rak_id, qty) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "qty = VALUES(qty)";

        String updateHeaderSql =
                "UPDATE stock_opname " +
                "SET status = 'COMPLETED' " +
                "WHERE id = ? AND status = 'DRAFT'";

        try (Connection conn =
                     DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                int lokasiRakId;

                try (PreparedStatement ps =
                             conn.prepareStatement(selectSql)) {

                    ps.setInt(1, opnameId);

                    try (ResultSet rs =
                                 ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException(
                                    "Stock Opname tidak ditemukan atau sudah diproses.");
                        }

                        lokasiRakId =
                                rs.getInt("lokasi_rak_id");
                    }
                }

                try (PreparedStatement psDetail =
                             conn.prepareStatement(detailSql);
                     PreparedStatement psStock =
                             conn.prepareStatement(updateStockSql)) {

                    psDetail.setInt(1, opnameId);

                    try (ResultSet rs =
                                 psDetail.executeQuery()) {

                        while (rs.next()) {

                            int barangId =
                                    rs.getInt("barang_id");

                            int countedQty =
                                    rs.getInt("counted_qty");

                            psStock.setInt(1, barangId);
                            psStock.setInt(2, lokasiRakId);
                            psStock.setInt(3, countedQty);

                            psStock.addBatch();
                        }
                    }

                    psStock.executeBatch();
                }

                try (PreparedStatement ps =
                             conn.prepareStatement(
                                     updateHeaderSql)) {

                    ps.setInt(1, opnameId);

                    if (ps.executeUpdate() == 0) {
                        throw new SQLException(
                                "Stock Opname gagal diproses.");
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


    public void deleteDraft(int opnameId)
            throws SQLException {

        String sql =
                "DELETE FROM stock_opname " +
                "WHERE id = ? AND status = 'DRAFT'";

        try (Connection conn =
                     DatabaseConnection.getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql)) {

            ps.setInt(1, opnameId);

            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new SQLException(
                        "Stock Opname tidak dapat dihapus. " +
                        "Pastikan status masih DRAFT.");
            }
        }
    }
}