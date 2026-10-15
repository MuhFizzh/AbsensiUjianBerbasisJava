package com.wms.model;

import java.time.LocalDate;

public class InboundHeader {
    private int id;
    private String nomor;
    private LocalDate tanggal;
    private Integer supplierId;
    private String supplierNama;
    private String catatan;
    private String status; // DRAFT, COMPLETED, CANCELLED
    private int totalItem;

    public InboundHeader() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNomor() { return nomor; }
    public void setNomor(String nomor) { this.nomor = nomor; }
    public LocalDate getTanggal() { return tanggal; }
    public void setTanggal(LocalDate tanggal) { this.tanggal = tanggal; }
    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }
    public String getSupplierNama() { return supplierNama; }
    public void setSupplierNama(String supplierNama) { this.supplierNama = supplierNama; }
    public String getCatatan() { return catatan; }
    public void setCatatan(String catatan) { this.catatan = catatan; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalItem() { return totalItem; }
    public void setTotalItem(int totalItem) { this.totalItem = totalItem; }
}
