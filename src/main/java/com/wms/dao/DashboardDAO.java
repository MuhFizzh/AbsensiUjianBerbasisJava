package com.wms.dao;

import com.wms.db.DatabaseConnection;
import com.wms.model.LowStockItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Query gabungan lintas-tabel untuk kebutuhan layar Dashboard: ringkasan
 * angka, dan daftar barang yang total stoknya (dijumlah dari semua lokasi
 * rak) sudah di bawah nilai minimum stock yang diatur di Master Data Barang.
 */
public class DashboardDAO {

    public record Summary(int totalBarangAktif, int inboundBulanIni, int outboundBulanIni, int totalLowStock) {
    }

    public Summary getSummary() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            int totalBarang = scalar(conn, "SELECT COUNT(*) FROM barang WHERE status='AKTIF'");
            int inboundBulanIni = scalar(conn,
                    "SELECT COUNT(*) FROM inbound WHERE MONTH(tanggal)=MONTH(CURDATE()) AND YEAR(tanggal)=YEAR(CURDATE())");
            int outboundBulanIni = scalar(conn,
                    "SELECT COUNT(*) FROM outbound WHERE MONTH(tanggal)=MONTH(CURDATE()) AND YEAR(tanggal)=YEAR(CURDATE())");
            int lowStock = findLowStock(conn).size();
            return new Summary(totalBarang, inboundBulanIni, outboundBulanIni, lowStock);
        }
    }

    /** Barang aktif yang total stoknya (dijumlah semua lokasi rak) di bawah minimum. */
    public List<LowStockItem> findLowStock() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findLowStock(conn);
        }
    }

    private List<LowStockItem> findLowStock(Connection conn) throws SQLException {
        String sql = "SELECT b.id, b.kode, b.nama, b.satuan, b.min_stock, COALESCE(SUM(s.qty),0) AS total_stock " +
                "FROM barang b LEFT JOIN stock s ON s.barang_id = b.id " +
                "WHERE b.status = 'AKTIF' " +
                "GROUP BY b.id, b.kode, b.nama, b.satuan, b.min_stock " +
                "HAVING COALESCE(SUM(s.qty),0) < b.min_stock " +
                "ORDER BY total_stock ASC, b.nama ASC";
        List<LowStockItem> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LowStockItem item = new LowStockItem();
                item.setBarangId(rs.getInt("id"));
                item.setKode(rs.getString("kode"));
                item.setNama(rs.getString("nama"));
                item.setSatuan(rs.getString("satuan"));
                item.setMinStock(rs.getInt("min_stock"));
                item.setTotalStock(rs.getInt("total_stock"));
                result.add(item);
            }
        }
        return result;
    }

    private int scalar(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
