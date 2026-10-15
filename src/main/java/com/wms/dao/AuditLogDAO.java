package com.wms.dao;

import com.wms.db.DatabaseConnection;
import com.wms.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    public void insert(
            String username,
            String module,
            String action,
            Integer referenceId,
            String description)
            throws SQLException {

        String sql =
                "INSERT INTO audit_log " +
                "(username,module,action,reference_id,description) " +
                "VALUES (?,?,?,?,?)";

        try (
                Connection conn =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        conn.prepareStatement(sql)
        ) {

            ps.setString(1, username);
            ps.setString(2, module);
            ps.setString(3, action);

            if (referenceId == null) {
                ps.setNull(
                        4,
                        java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, referenceId);
            }

            ps.setString(5, description);

            ps.executeUpdate();
        }
    }

    /**
     * Mengambil seluruh riwayat aktivitas untuk 1 transaksi tertentu
     * (module + reference_id), urut dari yang paling lama ke terbaru -
     * dipakai untuk menampilkan panel "Riwayat" seperti timeline.
     */
    public List<AuditLog> findHistory(String module, int referenceId) throws SQLException {
        String sql =
                "SELECT * FROM audit_log " +
                "WHERE module = ? AND reference_id = ? " +
                "ORDER BY created_at ASC, id ASC";

        List<AuditLog> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, module);
            ps.setInt(2, referenceId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    /**
     * Mencari username pembuat draft awal (kejadian "Buat Draft" paling
     * pertama tercatat) untuk 1 transaksi tertentu. Dipakai supaya
     * aktivitas_kerja bisa diatribusikan ke pembuat/pengaju transaksi
     * (yang mengerjakan di lapangan), bukan ke siapapun yang kebetulan
     * login saat transaksi itu diselesaikan/divalidasi.
     *
     * Mengembalikan null kalau tidak ditemukan (mis. transaksi lama
     * sebelum fitur audit_log granular ini ada) - pemanggil sebaiknya
     * fallback ke username sesi saat ini kalau hasilnya null.
     */
    public String findCreatorUsername(String module, int referenceId) throws SQLException {
        String sql =
                "SELECT username FROM audit_log " +
                "WHERE module = ? AND reference_id = ? " +
                "AND action IN ('Buat Draft', 'Ajukan Permintaan') " +
                "ORDER BY created_at ASC, id ASC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, module);
            ps.setInt(2, referenceId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("username") : null;
            }
        }
    }

    private AuditLog map(ResultSet rs) throws SQLException {
        AuditLog a = new AuditLog();
        a.setId(rs.getInt("id"));
        a.setUsername(rs.getString("username"));
        a.setModule(rs.getString("module"));
        a.setAction(rs.getString("action"));

        int refId = rs.getInt("reference_id");
        a.setReferenceId(rs.wasNull() ? null : refId);

        a.setDescription(rs.getString("description"));

        Timestamp ts = rs.getTimestamp("created_at");
        a.setCreatedAt(ts == null ? null : ts.toLocalDateTime());

        return a;
    }
}