-- =========================================================
-- WMS - Master Data Module
-- Database schema untuk MySQL 8.x
-- =========================================================

CREATE DATABASE IF NOT EXISTS wmsdb
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE wmsdb;

CREATE TABLE IF NOT EXISTS kategori (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  kode         VARCHAR(15)  NOT NULL UNIQUE,
  nama         VARCHAR(100) NOT NULL,
  deskripsi    TEXT,
  status       ENUM('AKTIF','NONAKTIF') NOT NULL DEFAULT 'AKTIF',
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS barang (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  kode         VARCHAR(15)  NOT NULL UNIQUE,
  nama         VARCHAR(150) NOT NULL,
  kategori_id  INT,
  satuan       VARCHAR(20)  NOT NULL DEFAULT 'Pcs',
  min_stock    INT          NOT NULL DEFAULT 0,
  deskripsi    TEXT,
  status       ENUM('AKTIF','NONAKTIF') NOT NULL DEFAULT 'AKTIF',
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_barang_kategori FOREIGN KEY (kategori_id) REFERENCES kategori(id)
      ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS supplier (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  kode         VARCHAR(15)  NOT NULL UNIQUE,
  nama         VARCHAR(150) NOT NULL,
  telepon      VARCHAR(30),
  email        VARCHAR(100),
  alamat       TEXT,
  status       ENUM('AKTIF','NONAKTIF') NOT NULL DEFAULT 'AKTIF',
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customer (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  kode         VARCHAR(15)  NOT NULL UNIQUE,
  nama         VARCHAR(150) NOT NULL,
  telepon      VARCHAR(30),
  email        VARCHAR(100),
  alamat       TEXT,
  status       ENUM('AKTIF','NONAKTIF') NOT NULL DEFAULT 'AKTIF',
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lokasi_rak (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  kode         VARCHAR(15)  NOT NULL UNIQUE,
  area         VARCHAR(50)  NOT NULL,
  rak          VARCHAR(10)  NOT NULL,
  kolom        VARCHAR(10)  NOT NULL,
  posisi       VARCHAR(10)  NOT NULL,
  kapasitas    INT          NOT NULL DEFAULT 0,
  status       ENUM('AKTIF','NONAKTIF') NOT NULL DEFAULT 'AKTIF',
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  kode         VARCHAR(15)  NOT NULL UNIQUE,
  nama         VARCHAR(150) NOT NULL,
  username     VARCHAR(50)  NOT NULL UNIQUE,
  password     VARCHAR(255) NOT NULL,
  role         ENUM('Administrator','Warehouse Operator','Staff Gudang') NOT NULL DEFAULT 'Staff Gudang',
  status       ENUM('AKTIF','NONAKTIF') NOT NULL DEFAULT 'AKTIF',
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Password default 'admin123' (disimpan dalam bentuk hash SHA-256 oleh aplikasi)
INSERT INTO users (kode, nama, username, password, role, status) VALUES
('USR-01','Ahmad Fauzi','afauzi','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a','Administrator','AKTIF');

CREATE TABLE IF NOT EXISTS stock (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  barang_id      INT NOT NULL,
  lokasi_rak_id  INT NOT NULL,
  qty            INT NOT NULL DEFAULT 0,
  updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_stock_barang_lokasi (barang_id, lokasi_rak_id),
  CONSTRAINT fk_stock_barang FOREIGN KEY (barang_id) REFERENCES barang(id) ON DELETE CASCADE,
  CONSTRAINT fk_stock_lokasi FOREIGN KEY (lokasi_rak_id) REFERENCES lokasi_rak(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- INBOUND (barang masuk dari supplier)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS inbound (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  nomor        VARCHAR(20) NOT NULL UNIQUE,
  tanggal      DATE NOT NULL,
  supplier_id  INT,
  catatan      TEXT,
  status       ENUM('DRAFT','COMPLETED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_inbound_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inbound_detail (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  inbound_id     INT NOT NULL,
  barang_id      INT NOT NULL,
  lokasi_rak_id  INT NOT NULL,
  qty            INT NOT NULL,
  CONSTRAINT fk_inbounddetail_inbound FOREIGN KEY (inbound_id) REFERENCES inbound(id) ON DELETE CASCADE,
  CONSTRAINT fk_inbounddetail_barang FOREIGN KEY (barang_id) REFERENCES barang(id),
  CONSTRAINT fk_inbounddetail_lokasi FOREIGN KEY (lokasi_rak_id) REFERENCES lokasi_rak(id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- OUTBOUND (barang keluar ke customer)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS outbound (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  nomor        VARCHAR(20) NOT NULL UNIQUE,
  tanggal      DATE NOT NULL,
  customer_id  INT,
  catatan      TEXT,
  status       ENUM('DRAFT','COMPLETED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_outbound_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS outbound_detail (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  outbound_id    INT NOT NULL,
  barang_id      INT NOT NULL,
  lokasi_rak_id  INT NOT NULL,
  qty            INT NOT NULL,
  CONSTRAINT fk_outbounddetail_outbound FOREIGN KEY (outbound_id) REFERENCES outbound(id) ON DELETE CASCADE,
  CONSTRAINT fk_outbounddetail_barang FOREIGN KEY (barang_id) REFERENCES barang(id),
  CONSTRAINT fk_outbounddetail_lokasi FOREIGN KEY (lokasi_rak_id) REFERENCES lokasi_rak(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS permintaan (
  id                INT AUTO_INCREMENT PRIMARY KEY,
  nomor             VARCHAR(20)  NOT NULL UNIQUE,
  tanggal           DATE         NOT NULL,
  jenis             ENUM('PINDAH_LOKASI','PERMINTAAN_BARANG','LAINNYA') NOT NULL DEFAULT 'LAINNYA',
  barang_id         INT,
  jumlah            INT,
  lokasi_asal_id    INT,
  lokasi_tujuan_id  INT,
  keterangan        TEXT,
  diajukan_oleh     VARCHAR(150) NOT NULL,
  status            ENUM('MENUNGGU','DISETUJUI','DITOLAK') NOT NULL DEFAULT 'MENUNGGU',
  disetujui_oleh    VARCHAR(150),
  catatan_approval  TEXT,
  created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_permintaan_barang FOREIGN KEY (barang_id) REFERENCES barang(id) ON DELETE SET NULL,
  CONSTRAINT fk_permintaan_lokasi_asal FOREIGN KEY (lokasi_asal_id) REFERENCES lokasi_rak(id) ON DELETE SET NULL,
  CONSTRAINT fk_permintaan_lokasi_tujuan FOREIGN KEY (lokasi_tujuan_id) REFERENCES lokasi_rak(id) ON DELETE SET NULL
) ENGINE=InnoDB;


