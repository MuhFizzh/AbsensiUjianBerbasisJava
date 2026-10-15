package com.wms.model;

public class Barang {
    private int id;
    private String kode;
    private String nama;
    private Integer kategoriId;
    private String kategoriNama;
    private String satuan;
    private int minStock;
    private String deskripsi;
    private String status;

    public Barang() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getKode() { return kode; }
    public void setKode(String kode) { this.kode = kode; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public Integer getKategoriId() { return kategoriId; }
    public void setKategoriId(Integer kategoriId) { this.kategoriId = kategoriId; }
    public String getKategoriNama() { return kategoriNama; }
    public void setKategoriNama(String kategoriNama) { this.kategoriNama = kategoriNama; }
    public String getSatuan() { return satuan; }
    public void setSatuan(String satuan) { this.satuan = satuan; }
    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return (kode != null ? kode + " - " : "") + nama;
    }
}
