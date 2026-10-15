package com.wms.model;

import java.time.LocalDate;

public class OutboundHeader {
    private int id;
    private String nomor;
    private LocalDate tanggal;
    private Integer customerId;
    private String customerNama;
    private String catatan;
    private String status;
    private int totalItem;

    public OutboundHeader() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNomor() { return nomor; }
    public void setNomor(String nomor) { this.nomor = nomor; }
    public LocalDate getTanggal() { return tanggal; }
    public void setTanggal(LocalDate tanggal) { this.tanggal = tanggal; }
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public String getCustomerNama() { return customerNama; }
    public void setCustomerNama(String customerNama) { this.customerNama = customerNama; }
    public String getCatatan() { return catatan; }
    public void setCatatan(String catatan) { this.catatan = catatan; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalItem() { return totalItem; }
    public void setTotalItem(int totalItem) { this.totalItem = totalItem; }
}
