package com.wms.model;

public class LokasiRak {
    private int id;
    private String kode;
    private String area;
    private String rak;
    private String kolom;
    private String posisi;
    private int kapasitas;
    private String status;

    public LokasiRak() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getKode() { return kode; }
    public void setKode(String kode) { this.kode = kode; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getRak() { return rak; }
    public void setRak(String rak) { this.rak = rak; }
    public String getKolom() { return kolom; }
    public void setKolom(String kolom) { this.kolom = kolom; }
    public String getPosisi() { return posisi; }
    public void setPosisi(String posisi) { this.posisi = posisi; }
    public int getKapasitas() { return kapasitas; }
    public void setKapasitas(int kapasitas) { this.kapasitas = kapasitas; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return (kode != null ? kode + " - " : "") + area;
    }
}
