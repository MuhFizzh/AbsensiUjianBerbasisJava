package com.wms.dao;

import com.wms.db.DatabaseConnection;
import com.wms.model.Permintaan;
import com.wms.util.CodeGenerator;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PermintaanDAO {

    private static final String BASE_SELECT =
            "SELECT p.*, b.nama AS barang_nama, " +
            "la.kode AS lokasi_asal_kode, lt.kode AS lokasi_tujuan_kode " +
            "FROM permintaan p " +
            "LEFT JOIN barang b ON p.barang_id = b.id " +
            "LEFT JOIN lokasi_rak la ON p.lokasi_asal_id = la.id " +
            "LEFT JOIN lokasi_rak lt ON p.lokasi_tujuan_id = lt.id ";
    
    private final StockDAO stockDAO =
        new StockDAO();
    
    private int lockedStockQty(
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
    
    private Permintaan findByIdForUpdate(
        Connection conn,
        int id)
        throws SQLException {

    String sql =
            BASE_SELECT +
            "WHERE p.id=? FOR UPDATE";

    try (PreparedStatement ps =
                 conn.prepareStatement(sql)) {

        ps.setInt(1, id);

        try (ResultSet rs =
                     ps.executeQuery()) {

            return rs.next()
                    ? map(rs)
                    : null;
        }
    }
}

    /** Semua permintaan, terbaru dulu. keyword mencari di nomor atau nama pemohon. */
    public List<Permintaan> findAll(String keyword) throws SQLException {
        String sql = BASE_SELECT + "WHERE p.nomor LIKE ? OR p.diajukan_oleh LIKE ? ORDER BY p.id DESC";
        List<Permintaan> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + (keyword == null ? "" : keyword) + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }

    /** Permintaan yang masih menunggu approval, paling lama diajukan dulu (antrian). */
    public List<Permintaan> findPending(String keyword) throws SQLException {
        String sql = BASE_SELECT + "WHERE p.status = 'MENUNGGU' " +
                "AND (p.nomor LIKE ? OR p.diajukan_oleh LIKE ?) ORDER BY p.id ASC";
        List<Permintaan> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + (keyword == null ? "" : keyword) + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }

    public void insert(Permintaan p) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String nomor = CodeGenerator.generateNextCode(conn, "permintaan", "nomor", "REQ-", 4);
            String sql = "INSERT INTO permintaan " +
                    "(nomor, tanggal, jenis, barang_id, jumlah, lokasi_asal_id, lokasi_tujuan_id, " +
                    "keterangan, diajukan_oleh, status) VALUES (?,?,?,?,?,?,?,?,?,'MENUNGGU')";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nomor);
                ps.setDate(2, Date.valueOf(p.getTanggal()));
                ps.setString(3, p.getJenis());
                setNullableInt(ps, 4, p.getBarangId());
                setNullableInt(ps, 5, p.getJumlah());
                setNullableInt(ps, 6, p.getLokasiAsalId());
                setNullableInt(ps, 7, p.getLokasiTujuanId());
                ps.setString(8, p.getKeterangan());
                ps.setString(9, p.getDiajukanOleh());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) p.setId(keys.getInt(1));
                }
                p.setNomor(nomor);
                p.setStatus("MENUNGGU");
            }
        }
    }

    /** Menyetujui permintaan. Gagal (0 baris terupdate) jika status sudah bukan MENUNGGU. */
    public boolean approve(
        int id,
        String disetujuiOleh,
        String catatan)
        throws SQLException {

    try (Connection conn =
                 DatabaseConnection.getConnection()) {

        conn.setAutoCommit(false);

        try {

            Permintaan p =
                    findByIdForUpdate(
                            conn,
                            id);

            if (p == null
                    || !"MENUNGGU".equals(
                    p.getStatus())) {

                conn.rollback();
                return false;
            }

            if ("PINDAH_LOKASI".equals(
                    p.getJenis())) {

                if (p.getBarangId() == null
                        || p.getJumlah() == null
                        || p.getJumlah() <= 0
                        || p.getLokasiAsalId() == null
                        || p.getLokasiTujuanId() == null) {

                    throw new SQLException(
                            "Data pindah lokasi tidak lengkap.");
                }

                if (p.getLokasiAsalId()
                        .equals(
                                p.getLokasiTujuanId())) {

                    throw new SQLException(
                            "Lokasi asal dan tujuan tidak boleh sama.");
                }

                int available =
                        lockedStockQty(
                                conn,
                                p.getBarangId(),
                                p.getLokasiAsalId());

                if (available < p.getJumlah()) {

                    throw new SQLException(
                            "Stok tidak mencukupi. " +
                            "Tersedia " + available +
                            ", diminta " + p.getJumlah() + ".");
                }

                stockDAO.adjustQty(
                        conn,
                        p.getBarangId(),
                        p.getLokasiAsalId(),
                        -p.getJumlah());

                stockDAO.adjustQty(
                        conn,
                        p.getBarangId(),
                        p.getLokasiTujuanId(),
                        p.getJumlah());
            }

            String sql =
                    "UPDATE permintaan " +
                    "SET status='DISETUJUI', " +
                    "disetujui_oleh=?, " +
                    "catatan_approval=? " +
                    "WHERE id=? " +
                    "AND status='MENUNGGU'";

            try (PreparedStatement ps =
                         conn.prepareStatement(sql)) {

                ps.setString(1, disetujuiOleh);
                ps.setString(2, catatan);
                ps.setInt(3, id);

                if (ps.executeUpdate() == 0) {

                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;

        } catch (SQLException ex) {

            conn.rollback();
            throw ex;

        } finally {

            conn.setAutoCommit(true);
        }
    }
}

    /** Menolak permintaan. Gagal (0 baris terupdate) jika status sudah bukan MENUNGGU. */
    public boolean reject(int id, String disetujuiOleh, String catatan) throws SQLException {
        String sql = "UPDATE permintaan SET status='DITOLAK', disetujui_oleh=?, catatan_approval=? " +
                "WHERE id=? AND status='MENUNGGU'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, disetujuiOleh);
            ps.setString(2, catatan);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Membatalkan permintaan sendiri, hanya boleh selama masih MENUNGGU. */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM permintaan WHERE id=? AND status='MENUNGGU'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.INTEGER);
        else ps.setInt(index, value);
    }

    private Permintaan map(ResultSet rs) throws SQLException {
        Permintaan p = new Permintaan();
        p.setId(rs.getInt("id"));
        p.setNomor(rs.getString("nomor"));
        Date tanggal = rs.getDate("tanggal");
        p.setTanggal(tanggal == null ? null : tanggal.toLocalDate());
        p.setJenis(rs.getString("jenis"));

        int barangId = rs.getInt("barang_id");
        p.setBarangId(rs.wasNull() ? null : barangId);
        p.setBarangNama(rs.getString("barang_nama"));

        int jumlah = rs.getInt("jumlah");
        p.setJumlah(rs.wasNull() ? null : jumlah);

        int lokasiAsalId = rs.getInt("lokasi_asal_id");
        p.setLokasiAsalId(rs.wasNull() ? null : lokasiAsalId);
        p.setLokasiAsalKode(rs.getString("lokasi_asal_kode"));

        int lokasiTujuanId = rs.getInt("lokasi_tujuan_id");
        p.setLokasiTujuanId(rs.wasNull() ? null : lokasiTujuanId);
        p.setLokasiTujuanKode(rs.getString("lokasi_tujuan_kode"));

        p.setKeterangan(rs.getString("keterangan"));
        p.setDiajukanOleh(rs.getString("diajukan_oleh"));
        p.setStatus(rs.getString("status"));
        p.setDisetujuiOleh(rs.getString("disetujui_oleh"));
        p.setCatatanApproval(rs.getString("catatan_approval"));
        return p;
    }
}
