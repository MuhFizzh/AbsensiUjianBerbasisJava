package com.wms.model;

/** Baris stok: jumlah sebuah barang pada sebuah lokasi rak tertentu. */
public class StockItem {
    private int barangId;
    private String barangKode;
    private String barangNama;
    private int lokasiRakId;
    private String lokasiKode;
    private String lokasiArea;
    private int qty;
    private int minStock;

    public StockItem() {
    }

    public int getBarangId() { return barangId; }
    public void setBarangId(int barangId) { this.barangId = barangId; }
    public String getBarangKode() { return barangKode; }
    public void setBarangKode(String barangKode) { this.barangKode = barangKode; }
    public String getBarangNama() { return barangNama; }
    public void setBarangNama(String barangNama) { this.barangNama = barangNama; }
    public int getLokasiRakId() { return lokasiRakId; }
    public void setLokasiRakId(int lokasiRakId) { this.lokasiRakId = lokasiRakId; }
    public String getLokasiKode() { return lokasiKode; }
    public void setLokasiKode(String lokasiKode) { this.lokasiKode = lokasiKode; }
    public String getLokasiArea() { return lokasiArea; }
    public void setLokasiArea(String lokasiArea) { this.lokasiArea = lokasiArea; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public int getMinStock() {return minStock;}
    public void setMinStock(int minStock) {this.minStock = minStock;}
}
