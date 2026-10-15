package com.wms.dao;

import com.wms.db.DatabaseConnection;
import com.wms.model.AktivitasKerja;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AktivitasKerjaDAO {

    /**
     * Menyimpan satu aktivitas kerja.
     *
     * 1 transaksi selesai = 1 aktivitas.
     */
    public void insert(AktivitasKerja aktivitas) throws SQLException {

        String sql =
                "INSERT INTO aktivitas_kerja (" +
                "username, jenis_aktivitas, reference_id, nomor_transaksi, " +
                "waktu_mulai, waktu_selesai, jumlah_item, total_qty, " +
                "detail_snapshot, status" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, aktivitas.getUsername());
            ps.setString(2, aktivitas.getJenisAktivitas());
            ps.setInt(3, aktivitas.getReferenceId());
            ps.setString(4, aktivitas.getNomorTransaksi());

            if (aktivitas.getWaktuMulai() == null) {
                ps.setTimestamp(5, null);
            } else {
                ps.setTimestamp(
                        5,
                        Timestamp.valueOf(aktivitas.getWaktuMulai())
                );
            }

            ps.setTimestamp(
                    6,
                    Timestamp.valueOf(aktivitas.getWaktuSelesai())
            );

            ps.setInt(7, aktivitas.getJumlahItem());
            ps.setInt(8, aktivitas.getTotalQty());
            ps.setString(9, aktivitas.getDetailSnapshot());
            ps.setString(10, aktivitas.getStatus());

            ps.executeUpdate();
        }
    }

    /**
     * Mengambil seluruh aktivitas.
     */
    public List<AktivitasKerja> findAll() throws SQLException {

        String sql =
                "SELECT * FROM aktivitas_kerja " +
                "ORDER BY waktu_selesai DESC, id DESC";

        List<AktivitasKerja> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(map(rs));
            }
        }

        return result;
    }

    /**
     * Mencari aktivitas berdasarkan username.
     */
    public List<AktivitasKerja> findByUsername(String username)
            throws SQLException {

        String sql =
                "SELECT * FROM aktivitas_kerja " +
                "WHERE username = ? " +
                "ORDER BY waktu_selesai DESC, id DESC";

        List<AktivitasKerja> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }

        return result;
    }

    /**
     * Mencari satu laporan berdasarkan nomor transaksi.
     *
     * Contoh:
     * OUT-00015
     * IN-00010
     */
    public AktivitasKerja findByNomorTransaksi(String nomorTransaksi)
            throws SQLException {

        String sql =
                "SELECT * FROM aktivitas_kerja " +
                "WHERE nomor_transaksi = ? " +
                "LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nomorTransaksi);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return map(rs);
                }
            }
        }

        return null;
    }

    /**
     * Menghitung jumlah aktivitas seorang user.
     *
     * Contoh:
     * hafizh = 17 aktivitas
     */
    public int countByUsername(String username) throws SQLException {

        String sql =
                "SELECT COUNT(*) AS total " +
                "FROM aktivitas_kerja " +
                "WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }

        return 0;
    }

    /**
     * Menghitung jumlah aktivitas berdasarkan jenis aktivitas.
     *
     * Contoh:
     * hafizh + OUTBOUND = 15
     */
    public int countByUsernameAndJenis(
            String username,
            String jenisAktivitas) throws SQLException {

        String sql =
                "SELECT COUNT(*) AS total " +
                "FROM aktivitas_kerja " +
                "WHERE username = ? " +
                "AND jenis_aktivitas = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, jenisAktivitas);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }

        return 0;
    }

    /**
     * Menghitung total quantity yang diproses user
     * berdasarkan jenis aktivitas.
     *
     * Contoh:
     * hafizh + OUTBOUND = 250 pcs
     */
    public int sumQtyByUsernameAndJenis(
            String username,
            String jenisAktivitas) throws SQLException {

        String sql =
                "SELECT COALESCE(SUM(total_qty), 0) AS total " +
                "FROM aktivitas_kerja " +
                "WHERE username = ? " +
                "AND jenis_aktivitas = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, jenisAktivitas);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }

        return 0;
    }

    /**
     * Mengambil aktivitas berdasarkan jenis.
     *
     * Contoh:
     * OUTBOUND
     */
    public List<AktivitasKerja> findByJenis(String jenisAktivitas)
            throws SQLException {

        String sql =
                "SELECT * FROM aktivitas_kerja " +
                "WHERE jenis_aktivitas = ? " +
                "ORDER BY waktu_selesai DESC, id DESC";

        List<AktivitasKerja> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, jenisAktivitas);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }

        return result;
    }

    /**
     * Mengambil aktivitas berdasarkan username
     * dan jenis aktivitas.
     */
    public List<AktivitasKerja> findByUsernameAndJenis(
            String username,
            String jenisAktivitas) throws SQLException {

        String sql =
                "SELECT * FROM aktivitas_kerja " +
                "WHERE username = ? " +
                "AND jenis_aktivitas = ? " +
                "ORDER BY waktu_selesai DESC, id DESC";

        List<AktivitasKerja> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, jenisAktivitas);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }

        return result;
    }

    /**
     * Mengambil laporan aktivitas dengan filter opsional: username,
     * jenis aktivitas, dan periode (rentang tanggal berdasarkan
     * waktu_selesai). Parameter yang null/kosong diabaikan (tidak
     * jadi filter). Dipakai layar "Laporan Aktivitas Kerja" (admin).
     */
    public List<AktivitasKerja> search(
            String username,
            String jenisAktivitas,
            LocalDate dari,
            LocalDate sampai) throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM aktivitas_kerja WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (username != null && !username.isBlank()) {
            sql.append("AND username = ? ");
            params.add(username);
        }
        if (jenisAktivitas != null && !jenisAktivitas.isBlank()) {
            sql.append("AND jenis_aktivitas = ? ");
            params.add(jenisAktivitas);
        }
        if (dari != null) {
            sql.append("AND waktu_selesai >= ? ");
            params.add(Timestamp.valueOf(dari.atStartOfDay()));
        }
        if (sampai != null) {
            sql.append("AND waktu_selesai < ? ");
            params.add(Timestamp.valueOf(sampai.plusDays(1).atStartOfDay()));
        }
        sql.append("ORDER BY waktu_selesai DESC, id DESC");

        List<AktivitasKerja> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }

        return result;
    }

    /**
     * Mapping ResultSet -> Model AktivitasKerja.
     */
    private AktivitasKerja map(ResultSet rs) throws SQLException {

        AktivitasKerja a = new AktivitasKerja();

        a.setId(rs.getInt("id"));
        a.setUsername(rs.getString("username"));
        a.setJenisAktivitas(rs.getString("jenis_aktivitas"));
        a.setReferenceId(rs.getInt("reference_id"));
        a.setNomorTransaksi(rs.getString("nomor_transaksi"));

        Timestamp waktuMulai = rs.getTimestamp("waktu_mulai");
        if (waktuMulai != null) {
            a.setWaktuMulai(waktuMulai.toLocalDateTime());
        }

        Timestamp waktuSelesai = rs.getTimestamp("waktu_selesai");
        if (waktuSelesai != null) {
            a.setWaktuSelesai(waktuSelesai.toLocalDateTime());
        }

        a.setJumlahItem(rs.getInt("jumlah_item"));
        a.setTotalQty(rs.getInt("total_qty"));
        a.setDetailSnapshot(rs.getString("detail_snapshot"));
        a.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            a.setCreatedAt(createdAt.toLocalDateTime());
        }

        return a;
    }
}