package com.wms.util;

import com.wms.dao.AktivitasKerjaDAO;
import com.wms.model.AktivitasKerja;

import java.time.LocalDateTime;

/**
 * Mencatat aktivitas kerja user ke tabel aktivitas_kerja.
 *
 * Aturan: 1 transaksi selesai = 1 aktivitas (lihat javadoc
 * AktivitasKerjaDAO#insert).
 *
 * Mengikuti pola AuditLogger: best-effort, jika gagal mencatat
 * aktivitas maka proses utama (Inbound/Outbound/Stock Opname)
 * TIDAK boleh ikut gagal, cukup dicatat ke error stream.
 */
public final class ActivityLogger {

    private static final AktivitasKerjaDAO DAO = new AktivitasKerjaDAO();

    private ActivityLogger() {
    }

    public static void log(
            String jenisAktivitas,
            int referenceId,
            String nomorTransaksi,
            int jumlahItem,
            int totalQty,
            String status,
            String detailSnapshotJson) {

        log(jenisAktivitas, referenceId, nomorTransaksi, jumlahItem, totalQty,
                status, detailSnapshotJson, null);
    }

    /**
     * Sama seperti {@link #log(String, int, String, int, int, String, String)},
     * tapi username yang dicatat bisa ditentukan eksplisit lewat
     * attributedUsername - dipakai supaya aktivitas_kerja diatribusikan
     * ke pembuat/pengaju transaksi (yang benar-benar mengerjakan di
     * lapangan), bukan ke user yang kebetulan login saat transaksi
     * diselesaikan/divalidasi (mis. Terima Barang, Setujui Permintaan).
     *
     * Kalau attributedUsername null (mis. transaksi lama yang pembuat
     * aslinya tidak diketahui), fallback ke username sesi saat ini -
     * sama seperti perilaku sebelumnya.
     */
    public static void log(
            String jenisAktivitas,
            int referenceId,
            String nomorTransaksi,
            int jumlahItem,
            int totalQty,
            String status,
            String detailSnapshotJson,
            String attributedUsername) {

        String username = attributedUsername != null ? attributedUsername : Session.getUsername();

        if (username == null) {
            return;
        }

        AktivitasKerja aktivitas = new AktivitasKerja();
        aktivitas.setUsername(username);
        aktivitas.setJenisAktivitas(jenisAktivitas);
        aktivitas.setReferenceId(referenceId);
        aktivitas.setNomorTransaksi(nomorTransaksi);
        aktivitas.setWaktuSelesai(LocalDateTime.now());
        aktivitas.setJumlahItem(jumlahItem);
        aktivitas.setTotalQty(totalQty);
        aktivitas.setDetailSnapshot(detailSnapshotJson);
        aktivitas.setStatus(status);

        try {
            DAO.insert(aktivitas);
        } catch (Exception ex) {
            System.err.println(
                    "Activity log gagal: " + ex.getMessage());
        }
    }
}
