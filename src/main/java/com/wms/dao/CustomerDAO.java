package com.wms.dao;

import com.wms.db.DatabaseConnection;
import com.wms.model.Customer;
import com.wms.util.CodeGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    public List<Customer> findAll(String keyword) throws SQLException {
        String sql = "SELECT * FROM customer WHERE nama LIKE ? OR kode LIKE ? ORDER BY id DESC";
        List<Customer> result = new ArrayList<>();
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

    public void insert(Customer s) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String kode = (s.getKode() == null || s.getKode().isBlank())
                    ? CodeGenerator.generateNextCode(conn, "customer", "CUS-", 2)
                    : s.getKode().trim().toUpperCase();
            String sql = "INSERT INTO customer (kode, nama, telepon, email, alamat, status) VALUES (?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, kode);
                ps.setString(2, s.getNama());
                ps.setString(3, s.getTelepon());
                ps.setString(4, s.getEmail());
                ps.setString(5, s.getAlamat());
                ps.setString(6, s.getStatus());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) s.setId(keys.getInt(1));
                }
                s.setKode(kode);
            }
        }
    }

    public void update(Customer s) throws SQLException {
        String sql = "UPDATE customer SET nama=?, telepon=?, email=?, alamat=?, status=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getNama());
            ps.setString(2, s.getTelepon());
            ps.setString(3, s.getEmail());
            ps.setString(4, s.getAlamat());
            ps.setString(5, s.getStatus());
            ps.setInt(6, s.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM customer WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Customer map(ResultSet rs) throws SQLException {
        Customer s = new Customer();
        s.setId(rs.getInt("id"));
        s.setKode(rs.getString("kode"));
        s.setNama(rs.getString("nama"));
        s.setTelepon(rs.getString("telepon"));
        s.setEmail(rs.getString("email"));
        s.setAlamat(rs.getString("alamat"));
        s.setStatus(rs.getString("status"));
        return s;
    }
}
