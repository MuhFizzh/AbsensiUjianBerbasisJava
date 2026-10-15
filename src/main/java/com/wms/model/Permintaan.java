package com.wms.model;

import java.time.LocalDate;

/**
 * Merepresentasikan satu permintaan (request) yang diajukan staff dan perlu
 * disetujui/ditolak lewat modul Approval. Bisa berupa permintaan pindah
 * lokasi stok, permintaan barang baru, atau jenis lain-lain (bebas teks).
 */
public class Permintaan {
    private int id;
    private String nomor;
    private LocalDate tanggal;
    private String jenis; // PINDAH_LOKASI / PERMINTAAN_BARANG / LAINNYA

    private Integer barangId;
    private String barangNama; // hasil join, untuk ditampilkan

    private Integer jumlah;

    private Integer lokasiAsalId;
    private String lokasiAsalKode; // hasil join

    private Integer lokasiTujuanId;
    private String lokasiTujuanKode; // hasil join

    private String keterangan;
    private String diajukanOleh;

    private String status; // MENUNGGU / DISETUJUI / DITOLAK
    private String disetujuiOleh;
    private String catatanApproval;

    public Permintaan() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomor() { return nomor; }
    public void setNomor(String nomor) { this.nomor = nomor; }

    public LocalDate getTanggal() { return tanggal; }
    public void setTanggal(LocalDate tanggal) { this.tanggal = tanggal; }

    public String getJenis() { return jenis; }
    public void setJenis(String jenis) { this.jenis = jenis; }

    public Integer getBarangId() { return barangId; }
    public void setBarangId(Integer barangId) { this.barangId = barangId; }

    public String getBarangNama() { return barangNama; }
    public void setBarangNama(String barangNama) { this.barangNama = barangNama; }

    public Integer getJumlah() { return jumlah; }
    public void setJumlah(Integer jumlah) { this.jumlah = jumlah; }

    public Integer getLokasiAsalId() { return lokasiAsalId; }
    public void setLokasiAsalId(Integer lokasiAsalId) { this.lokasiAsalId = lokasiAsalId; }

    public String getLokasiAsalKode() { return lokasiAsalKode; }
    public void setLokasiAsalKode(String lokasiAsalKode) { this.lokasiAsalKode = lokasiAsalKode; }

    public Integer getLokasiTujuanId() { return lokasiTujuanId; }
    public void setLokasiTujuanId(Integer lokasiTujuanId) { this.lokasiTujuanId = lokasiTujuanId; }

    public String getLokasiTujuanKode() { return lokasiTujuanKode; }
    public void setLokasiTujuanKode(String lokasiTujuanKode) { this.lokasiTujuanKode = lokasiTujuanKode; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    public String getDiajukanOleh() { return diajukanOleh; }
    public void setDiajukanOleh(String diajukanOleh) { this.diajukanOleh = diajukanOleh; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDisetujuiOleh() { return disetujuiOleh; }
    public void setDisetujuiOleh(String disetujuiOleh) { this.disetujuiOleh = disetujuiOleh; }

    public String getCatatanApproval() { return catatanApproval; }
    public void setCatatanApproval(String catatanApproval) { this.catatanApproval = catatanApproval; }

    /** Label jenis yang ramah ditampilkan di UI. */
    public String getJenisLabel() {
        if (jenis == null) return "-";
        return switch (jenis) {
            case "PINDAH_LOKASI" -> "Pindah Lokasi Stok";
            case "PERMINTAAN_BARANG" -> "Permintaan Barang Baru";
            default -> "Lainnya";
        };
    }
}
