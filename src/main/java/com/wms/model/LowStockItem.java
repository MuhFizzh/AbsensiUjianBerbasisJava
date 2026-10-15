package com.wms.model;

/** Baris hasil agregasi: barang yang total stoknya (di semua lokasi rak) di bawah minimum. */
public class LowStockItem {
    private int barangId;
    private String kode;
    private String nama;
    private String satuan;
    private int minStock;
    private int totalStock;

    public int getBarangId() { return barangId; }
    public void setBarangId(int barangId) { this.barangId = barangId; }
    public String getKode() { return kode; }
    public void setKode(String kode) { this.kode = kode; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public String getSatuan() { return satuan; }
    public void setSatuan(String satuan) { this.satuan = satuan; }
    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
    public int getTotalStock() { return totalStock; }
    public void setTotalStock(int totalStock) { this.totalStock = totalStock; }
}
