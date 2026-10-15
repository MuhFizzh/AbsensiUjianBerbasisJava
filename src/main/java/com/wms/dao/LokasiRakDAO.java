package com.wms.dao;

import com.wms.db.DatabaseConnection;
import com.wms.model.LokasiRak;
import com.wms.util.CodeGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LokasiRakDAO {

    public List<LokasiRak> findAll(String keyword) throws SQLException {
        String sql = "SELECT * FROM lokasi_rak WHERE area LIKE ? OR kode LIKE ? ORDER BY id DESC";
        List<LokasiRak> result = new ArrayList<>();
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

    public void insert(LokasiRak l) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String kode = (l.getKode() == null || l.getKode().isBlank())
                    ? CodeGenerator.generateLokasiKode(l.getArea(), l.getRak())
                    : l.getKode().trim().toUpperCase();
            String sql = "INSERT INTO lokasi_rak (kode, area, rak, kolom, posisi, kapasitas, status) " +
                    "VALUES (?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, kode);
                ps.setString(2, l.getArea());
                ps.setString(3, l.getRak());
                ps.setString(4, l.getKolom());
                ps.setString(5, l.getPosisi());
                ps.setInt(6, l.getKapasitas());
                ps.setString(7, l.getStatus());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) l.setId(keys.getInt(1));
                }
                l.setKode(kode);
            }
        }
    }

    public void update(LokasiRak l) throws SQLException {
        String sql = "UPDATE lokasi_rak SET area=?, rak=?, kolom=?, posisi=?, kapasitas=?, status=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, l.getArea());
            ps.setString(2, l.getRak());
            ps.setString(3, l.getKolom());
            ps.setString(4, l.getPosisi());
            ps.setInt(5, l.getKapasitas());
            ps.setString(6, l.getStatus());
            ps.setInt(7, l.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {

    String checkSql =
            "SELECT " +
            "(SELECT COUNT(*) FROM inbound_detail WHERE lokasi_rak_id = ?) AS inbound_count, " +
            "(SELECT COUNT(*) FROM outbound_detail WHERE lokasi_rak_id = ?) AS outbound_count, " +
            "(SELECT COUNT(*) FROM permintaan " +
            " WHERE lokasi_asal_id = ? OR lokasi_tujuan_id = ?) AS request_count";

    String deleteSql = "DELETE FROM lokasi_rak WHERE id=?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement checkPs = conn.prepareStatement(checkSql)) {

        checkPs.setInt(1, id);
        checkPs.setInt(2, id);
        checkPs.setInt(3, id);
        checkPs.setInt(4, id);

        try (ResultSet rs = checkPs.executeQuery()) {

            if (rs.next()) {

                int inboundCount = rs.getInt("inbound_count");
                int outboundCount = rs.getInt("outbound_count");
                int requestCount = rs.getInt("request_count");

                if (inboundCount > 0 ||
                    outboundCount > 0 ||
                    requestCount > 0) {

                    throw new SQLException(
                            "Lokasi rak tidak dapat dihapus karena " +
                            "sudah digunakan dalam transaksi/history."
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

    private LokasiRak map(ResultSet rs) throws SQLException {
        LokasiRak l = new LokasiRak();
        l.setId(rs.getInt("id"));
        l.setKode(rs.getString("kode"));
        l.setArea(rs.getString("area"));
        l.setRak(rs.getString("rak"));
        l.setKolom(rs.getString("kolom"));
        l.setPosisi(rs.getString("posisi"));
        l.setKapasitas(rs.getInt("kapasitas"));
        l.setStatus(rs.getString("status"));
        return l;
    }
}
