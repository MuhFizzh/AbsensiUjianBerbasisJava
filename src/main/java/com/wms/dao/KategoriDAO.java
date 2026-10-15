package com.wms.dao;

import com.wms.db.DatabaseConnection;
import com.wms.model.Kategori;
import com.wms.util.CodeGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class KategoriDAO {

    public List<Kategori> findAll(String keyword) throws SQLException {
        String sql = "SELECT * FROM kategori WHERE nama LIKE ? OR kode LIKE ? ORDER BY id DESC";
        List<Kategori> result = new ArrayList<>();
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

    public void insert(Kategori k) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String kode = (k.getKode() == null || k.getKode().isBlank())
                    ? CodeGenerator.generateNextCode(conn, "kategori", "KTG-", 2)
                    : k.getKode().trim().toUpperCase();
            String sql = "INSERT INTO kategori (kode, nama, deskripsi, status) VALUES (?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, kode);
                ps.setString(2, k.getNama());
                ps.setString(3, k.getDeskripsi());
                ps.setString(4, k.getStatus());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        k.setId(keys.getInt(1));
                    }
                }
                k.setKode(kode);
            }
        }
    }

    public void update(Kategori k) throws SQLException {
        String sql = "UPDATE kategori SET nama=?, deskripsi=?, status=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, k.getNama());
            ps.setString(2, k.getDeskripsi());
            ps.setString(3, k.getStatus());
            ps.setInt(4, k.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM kategori WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Kategori map(ResultSet rs) throws SQLException {
        Kategori k = new Kategori();
        k.setId(rs.getInt("id"));
        k.setKode(rs.getString("kode"));
        k.setNama(rs.getString("nama"));
        k.setDeskripsi(rs.getString("deskripsi"));
        k.setStatus(rs.getString("status"));
        return k;
    }
}
