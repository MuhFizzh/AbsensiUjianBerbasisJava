package com.wms.model;

public class Kategori {
    private int id;
    private String kode;
    private String nama;
    private String deskripsi;
    private String status;

    public Kategori() {
    }

    public Kategori(int id, String kode, String nama, String deskripsi, String status) {
        this.id = id;
        this.kode = kode;
        this.nama = nama;
        this.deskripsi = deskripsi;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getKode() { return kode; }
    public void setKode(String kode) { this.kode = kode; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() { return nama; }
}
