package com.wms.dao;

import com.wms.db.DatabaseConnection;
import com.wms.model.StockItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Akses data stok (qty barang per lokasi rak). Sebagian method menerima
 * Connection dari luar supaya bisa ikut dalam transaksi yang sama dengan
 * proses Inbound/Outbound (commit/rollback bersamaan).
 */
public class StockDAO {

    /** Daftar stok gabungan (dipakai untuk layar ringkasan / validasi). */
    public List<StockItem> findAll(String keyword) throws SQLException {
        String sql = "SELECT s.barang_id, b.kode AS barang_kode, b.nama AS barang_nama, " +
                "s.lokasi_rak_id, l.kode AS lokasi_kode, l.area AS lokasi_area, s.qty, b.min_stock " +
                "FROM stock s " +
                "JOIN barang b ON s.barang_id = b.id " +
                "JOIN lokasi_rak l ON s.lokasi_rak_id = l.id " +
                "WHERE (b.nama LIKE ? OR b.kode LIKE ?) AND s.qty > 0 " +
                "ORDER BY b.nama, l.kode";
        List<StockItem> result = new ArrayList<>();
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

    /** Qty stok barang tertentu di lokasi tertentu (0 bila belum ada baris stok). */
    /**
     * Mengambil stok tersedia untuk satu barang pada satu lokasi.
     * Dipakai oleh form transaksi untuk validasi sebelum draft disimpan.
     */
    public int getAvailableQty(int barangId, int lokasiRakId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getQty(conn, barangId, lokasiRakId);
        }
    }

    public int getQty(Connection conn, int barangId, int lokasiRakId) throws SQLException {
        String sql = "SELECT qty FROM stock WHERE barang_id=? AND lokasi_rak_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, barangId);
            ps.setInt(2, lokasiRakId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("qty") : 0;
            }
        }
    }

    /**
     * Menambah/mengurangi qty stok pada baris (barang, lokasi) tertentu.
     * Menggunakan UPSERT atomik supaya benar meski baris stok sudah ada
     * dengan qty persis 0 (mis. setelah outbound menghabiskan stok) -
     * skenario yang sebelumnya bisa memicu error UNIQUE constraint bila
     * dicek manual lebih dulu dengan SELECT.
     * Dipakai di dalam transaksi Inbound (delta positif) / Outbound (delta negatif).
     */
    public void adjustQty(Connection conn, int barangId, int lokasiRakId, int delta) throws SQLException {
        String sql = "INSERT INTO stock (barang_id, lokasi_rak_id, qty) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE qty = qty + VALUES(qty)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, barangId);
            ps.setInt(2, lokasiRakId);
            ps.setInt(3, delta);
            ps.executeUpdate();
        }
    }
    
public List<StockItem> findByBarang(int barangId) throws SQLException {
    String sql =
            "SELECT s.barang_id, b.kode AS barang_kode, b.nama AS barang_nama, " +
            "s.lokasi_rak_id, l.kode AS lokasi_kode, l.area AS lokasi_area, s.qty, b.min_stock " +
            "FROM stock s " +
            "JOIN barang b ON s.barang_id = b.id " +
            "JOIN lokasi_rak l ON s.lokasi_rak_id = l.id " +
            "WHERE s.barang_id = ? AND s.qty > 0 " +
            "ORDER BY l.kode";

    List<StockItem> result = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setInt(1, barangId);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
    }

    return result;
}

    /** Semua baris stok (barang) yang tercatat pada satu lokasi rak tertentu. Dipakai untuk memuat item Stock Opname. */
    public List<StockItem> findByLokasi(int lokasiRakId) throws SQLException {
        String sql =
                "SELECT s.barang_id, b.kode AS barang_kode, b.nama AS barang_nama, " +
                "s.lokasi_rak_id, l.kode AS lokasi_kode, l.area AS lokasi_area, s.qty, b.min_stock " +
                "FROM stock s " +
                "JOIN barang b ON s.barang_id = b.id " +
                "JOIN lokasi_rak l ON s.lokasi_rak_id = l.id " +
                "WHERE s.lokasi_rak_id = ? " +
                "ORDER BY b.nama";

        List<StockItem> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lokasiRakId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }

    private StockItem map(ResultSet rs) throws SQLException {
        StockItem s = new StockItem();
        s.setBarangId(rs.getInt("barang_id"));
        s.setBarangKode(rs.getString("barang_kode"));
        s.setBarangNama(rs.getString("barang_nama"));
        s.setLokasiRakId(rs.getInt("lokasi_rak_id"));
        s.setLokasiKode(rs.getString("lokasi_kode"));
        s.setLokasiArea(rs.getString("lokasi_area"));
        s.setQty(rs.getInt("qty"));
        s.setMinStock(rs.getInt("min_stock"));
        return s;
    }
}
