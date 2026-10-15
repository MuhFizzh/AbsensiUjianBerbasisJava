USE wmsdb;

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
