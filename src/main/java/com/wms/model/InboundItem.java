package com.wms.model;

public class InboundItem {
    private int id;
    private int inboundId;
    private int barangId;
    private String barangKode;
    private String barangNama;
    private int lokasiRakId;
    private String lokasiKode;
    private int qty;

    public InboundItem() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getInboundId() { return inboundId; }
    public void setInboundId(int inboundId) { this.inboundId = inboundId; }
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
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
}
