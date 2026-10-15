package com.wms.model;

import java.time.LocalDate;

public class StockOpname {

    private int id;
    private String nomor;
    private LocalDate tanggal;
    private String status;
    private String catatan;
    private Integer createdBy;
    private int lokasiRakId;
    private String lokasiKode;
    private String dibuatOleh;
    private int totalItem;
    private int totalSelisih;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNomor() {
        return nomor;
    }

    public void setNomor(String nomor) {
        this.nomor = nomor;
    }

    public LocalDate getTanggal() {
        return tanggal;
    }

    public void setTanggal(LocalDate tanggal) {
        this.tanggal = tanggal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCatatan() {
        return catatan;
    }

    public void setCatatan(String catatan) {
        this.catatan = catatan;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }
    
    public int getLokasiRakId() {
    return lokasiRakId;
    }

    public void setLokasiRakId(int lokasiRakId) {
        this.lokasiRakId = lokasiRakId;
    }

    public String getLokasiKode() {
        return lokasiKode;
    }

    public void setLokasiKode(String lokasiKode) {
        this.lokasiKode = lokasiKode;
    }

    public String getDibuatOleh() {
        return dibuatOleh;
    }

    public void setDibuatOleh(String dibuatOleh) {
        this.dibuatOleh = dibuatOleh;
    }

    public int getTotalItem() {
        return totalItem;
    }

    public void setTotalItem(int totalItem) {
        this.totalItem = totalItem;
    }

    public int getTotalSelisih() {
        return totalSelisih;
    }

    public void setTotalSelisih(int totalSelisih) {
        this.totalSelisih = totalSelisih;
    }
}