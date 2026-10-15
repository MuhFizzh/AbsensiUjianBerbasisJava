package com.wms.model;

public class Supplier {
    private int id;
    private String kode;
    private String nama;
    private String telepon;
    private String email;
    private String alamat;
    private String status;

    public Supplier() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getKode() { return kode; }
    public void setKode(String kode) { this.kode = kode; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public String getTelepon() { return telepon; }
    public void setTelepon(String telepon) { this.telepon = telepon; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAlamat() { return alamat; }
    public void setAlamat(String alamat) { this.alamat = alamat; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    @Override
public String toString() {
    return nama;
}
}
