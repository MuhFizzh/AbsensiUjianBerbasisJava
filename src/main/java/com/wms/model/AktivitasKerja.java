package com.wms.model;

import java.time.LocalDateTime;

public class AktivitasKerja {

    private int id;
    private String username;
    private String jenisAktivitas;
    private int referenceId;
    private String nomorTransaksi;
    private LocalDateTime waktuMulai;
    private LocalDateTime waktuSelesai;
    private int jumlahItem;
    private int totalQty;
    private String status;
    private LocalDateTime createdAt;

    /**
     * Salinan (snapshot) rincian barang dalam format JSON, dibuat pada
     * saat transaksi selesai diproses (lihat SnapshotUtil). Bisa null
     * untuk baris lama yang dicatat sebelum fitur snapshot ada -
     * dalam kasus itu UI fallback ke rekonstruksi live dari tabel asal.
     */
    private String detailSnapshot;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getJenisAktivitas() {
        return jenisAktivitas;
    }

    public void setJenisAktivitas(String jenisAktivitas) {
        this.jenisAktivitas = jenisAktivitas;
    }

    public int getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(int referenceId) {
        this.referenceId = referenceId;
    }

    public String getNomorTransaksi() {
        return nomorTransaksi;
    }

    public void setNomorTransaksi(String nomorTransaksi) {
        this.nomorTransaksi = nomorTransaksi;
    }

    public LocalDateTime getWaktuMulai() {
        return waktuMulai;
    }

    public void setWaktuMulai(LocalDateTime waktuMulai) {
        this.waktuMulai = waktuMulai;
    }

    public LocalDateTime getWaktuSelesai() {
        return waktuSelesai;
    }

    public void setWaktuSelesai(LocalDateTime waktuSelesai) {
        this.waktuSelesai = waktuSelesai;
    }

    public int getJumlahItem() {
        return jumlahItem;
    }

    public void setJumlahItem(int jumlahItem) {
        this.jumlahItem = jumlahItem;
    }

    public int getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(int totalQty) {
        this.totalQty = totalQty;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDetailSnapshot() {
        return detailSnapshot;
    }

    public void setDetailSnapshot(String detailSnapshot) {
        this.detailSnapshot = detailSnapshot;
    }
}