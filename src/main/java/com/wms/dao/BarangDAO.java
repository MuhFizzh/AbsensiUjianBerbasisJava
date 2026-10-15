package com.wms.dao;

import com.wms.db.DatabaseConnection;
import com.wms.model.Barang;
import com.wms.util.CodeGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BarangDAO {

    private static final String BASE_SELECT =
            "SELECT b.*, k.nama AS kategori_nama FROM barang b " +
            "LEFT JOIN kategori k ON b.kategori_id = k.id ";

    public List<Barang> findAll(String keyword) throws SQLException {
        String sql = BASE_SELECT + "WHERE b.nama LIKE ? OR b.kode LIKE ? ORDER BY b.id DESC";
        List<Barang> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + (keyword == null ? "" : keyword) + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public void insert(Barang b) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String kode = (b.getKode() == null || b.getKode().isBlank())
                    ? CodeGenerator.generateNextCode(conn, "barang", "BRG-", 3)
                    : b.getKode().trim().toUpperCase();
            String sql = "INSERT INTO barang (kode, nama, kategori_id, satuan, min_stock, deskripsi, status) " +
                    "VALUES (?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, kode);
                ps.setString(2, b.getNama());
                if (b.getKategoriId() == null) ps.setNull(3, java.sql.Types.INTEGER);
                else ps.setInt(3, b.getKategoriId());
                ps.setString(4, b.getSatuan());
                ps.setInt(5, b.getMinStock());
                ps.setString(6, b.getDeskripsi());
                ps.setString(7, b.getStatus());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) b.setId(keys.getInt(1));
                }
                b.setKode(kode);
            }
        }
    }

    public void update(Barang b) throws SQLException {
        String sql = "UPDATE barang SET nama=?, kategori_id=?, satuan=?, min_stock=?, deskripsi=?, status=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getNama());
            if (b.getKategoriId() == null) ps.setNull(2, java.sql.Types.INTEGER);
            else ps.setInt(2, b.getKategoriId());
            ps.setString(3, b.getSatuan());
            ps.setInt(4, b.getMinStock());
            ps.setString(5, b.getDeskripsi());
            ps.setString(6, b.getStatus());
            ps.setInt(7, b.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {

    String checkSql =
            "SELECT " +
            "(SELECT COUNT(*) FROM inbound_detail WHERE barang_id = ?) AS inbound_count, " +
            "(SELECT COUNT(*) FROM outbound_detail WHERE barang_id = ?) AS outbound_count, " +
            "(SELECT COUNT(*) FROM permintaan WHERE barang_id = ?) AS request_count";

    String deleteSql = "DELETE FROM barang WHERE id=?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement checkPs = conn.prepareStatement(checkSql)) {

        checkPs.setInt(1, id);
        checkPs.setInt(2, id);
        checkPs.setInt(3, id);

        try (ResultSet rs = checkPs.executeQuery()) {

            if (rs.next()) {

                int inboundCount = rs.getInt("inbound_count");
                int outboundCount = rs.getInt("outbound_count");
                int requestCount = rs.getInt("request_count");

                if (inboundCount > 0 ||
                    outboundCount > 0 ||
                    requestCount > 0) {

                    throw new SQLException(
                            "Barang tidak dapat dihapus karena sudah digunakan " +
                            "dalam transaksi/history."
                    );
                }
            }
        }

        try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
            deletePs.setInt(1, id);
            deletePs.executeUpdate();
        }
    }
}

    private Barang map(ResultSet rs) throws SQLException {
        Barang b = new Barang();
        b.setId(rs.getInt("id"));
        b.setKode(rs.getString("kode"));
        b.setNama(rs.getString("nama"));
        int katId = rs.getInt("kategori_id");
        b.setKategoriId(rs.wasNull() ? null : katId);
        b.setKategoriNama(rs.getString("kategori_nama"));
        b.setSatuan(rs.getString("satuan"));
        b.setMinStock(rs.getInt("min_stock"));
        b.setDeskripsi(rs.getString("deskripsi"));
        b.setStatus(rs.getString("status"));
        return b;
    }
}
