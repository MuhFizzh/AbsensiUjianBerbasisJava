package com.wms.dao;

import com.wms.db.DatabaseConnection;
import com.wms.model.User;
import com.wms.util.CodeGenerator;
import com.wms.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public List<User> findAll(String keyword) throws SQLException {
        String sql = "SELECT * FROM users WHERE nama LIKE ? OR username LIKE ? OR kode LIKE ? ORDER BY id DESC";
        List<User> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + (keyword == null ? "" : keyword) + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }
    
    public User authenticate(String username, String plainPassword)
        throws SQLException {

    String sql =
            "SELECT * FROM users " +
            "WHERE username=? AND status='AKTIF' " +
            "LIMIT 1";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setString(1, username);

        try (ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) {
                return null;
            }

            User user = map(rs);

            if (PasswordUtil.matches(
                    plainPassword,
                    user.getPassword())) {

                return user;
            }

            return null;
        }
    }
}

    public void insert(User u, String plainPassword) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String kode = (u.getKode() == null || u.getKode().isBlank())
                    ? CodeGenerator.generateNextCode(conn, "users", "USR-", 2)
                    : u.getKode().trim().toUpperCase();
            String sql = "INSERT INTO users (kode, nama, username, password, role, status) VALUES (?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, kode);
                ps.setString(2, u.getNama());
                ps.setString(3, u.getUsername());
                ps.setString(4, PasswordUtil.hash(plainPassword));
                ps.setString(5, u.getRole());
                ps.setString(6, u.getStatus());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) u.setId(keys.getInt(1));
                }
                u.setKode(kode);
            }
        }
    }

    public void update(User u, String plainPassword) throws SQLException {
        boolean changePassword = plainPassword != null && !plainPassword.isBlank();
        String sql = changePassword
                ? "UPDATE users SET nama=?, username=?, password=?, role=?, status=? WHERE id=?"
                : "UPDATE users SET nama=?, username=?, role=?, status=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (changePassword) {
                ps.setString(1, u.getNama());
                ps.setString(2, u.getUsername());
                ps.setString(3, PasswordUtil.hash(plainPassword));
                ps.setString(4, u.getRole());
                ps.setString(5, u.getStatus());
                ps.setInt(6, u.getId());
            } else {
                ps.setString(1, u.getNama());
                ps.setString(2, u.getUsername());
                ps.setString(3, u.getRole());
                ps.setString(4, u.getStatus());
                ps.setInt(5, u.getId());
            }
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setKode(rs.getString("kode"));
        u.setNama(rs.getString("nama"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setStatus(rs.getString("status"));
        return u;
    }
}
