USE wmsdb;

CREATE TABLE IF NOT EXISTS audit_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    module VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    reference_id INT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_audit_created (created_at),
    INDEX idx_audit_user (username)
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS stock_opname (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nomor VARCHAR(20) NOT NULL UNIQUE,
    tanggal DATE NOT NULL,
    lokasi_rak_id INT NOT NULL,
    dibuat_oleh VARCHAR(50) NOT NULL,
    status ENUM('DRAFT','COMPLETED','CANCELLED')
        NOT NULL DEFAULT 'DRAFT',
    catatan TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_opname_lokasi
        FOREIGN KEY (lokasi_rak_id)
        REFERENCES lokasi_rak(id)
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS stock_opname_detail (
    id INT AUTO_INCREMENT PRIMARY KEY,
    opname_id INT NOT NULL,
    barang_id INT NOT NULL,
    system_qty INT NOT NULL DEFAULT 0,
    counted_qty INT NOT NULL DEFAULT 0,
    selisih INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_opname_detail_header
        FOREIGN KEY (opname_id)
        REFERENCES stock_opname(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_opname_detail_barang
        FOREIGN KEY (barang_id)
        REFERENCES barang(id)
) ENGINE=InnoDB;
