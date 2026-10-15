package com.wms.model;

public class StockOpnameDetail {

    private int id;
    private int stockOpnameId;
    private int barangId;
    private int lokasiRakId;
    private int systemQty;
    private int actualQty;
    private int adjustmentQty;

    private String barangKode;
    private String barangNama;
    private String lokasiKode;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStockOpnameId() {
        return stockOpnameId;
    }

    public void setStockOpnameId(int stockOpnameId) {
        this.stockOpnameId = stockOpnameId;
    }

    public int getBarangId() {
        return barangId;
    }

    public void setBarangId(int barangId) {
        this.barangId = barangId;
    }

    public int getLokasiRakId() {
        return lokasiRakId;
    }

    public void setLokasiRakId(int lokasiRakId) {
        this.lokasiRakId = lokasiRakId;
    }

    public int getSystemQty() {
        return systemQty;
    }

    public void setSystemQty(int systemQty) {
        this.systemQty = systemQty;
    }

    public int getActualQty() {
        return actualQty;
    }

    public void setActualQty(int actualQty) {
        this.actualQty = actualQty;
    }

    public int getAdjustmentQty() {
        return adjustmentQty;
    }

    public void setAdjustmentQty(int adjustmentQty) {
        this.adjustmentQty = adjustmentQty;
    }

    public String getBarangKode() {
        return barangKode;
    }

    public void setBarangKode(String barangKode) {
        this.barangKode = barangKode;
    }

    public String getBarangNama() {
        return barangNama;
    }

    public void setBarangNama(String barangNama) {
        this.barangNama = barangNama;
    }

    public String getLokasiKode() {
        return lokasiKode;
    }

    public void setLokasiKode(String lokasiKode) {
        this.lokasiKode = lokasiKode;
    }
}