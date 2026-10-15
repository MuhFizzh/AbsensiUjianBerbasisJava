USE wmsdb;

CREATE TABLE IF NOT EXISTS aktivitas_kerja (
    id INT AUTO_INCREMENT PRIMARY KEY,

    username VARCHAR(50) NOT NULL,

    jenis_aktivitas VARCHAR(30) NOT NULL,

    reference_id INT NOT NULL,

    nomor_transaksi VARCHAR(30) NOT NULL,

    waktu_mulai DATETIME NULL,

    waktu_selesai DATETIME NOT NULL,

    jumlah_item INT NOT NULL DEFAULT 0,

    total_qty INT NOT NULL DEFAULT 0,

    -- Salinan (snapshot) rincian barang dalam format JSON, dibuat saat
    -- transaksi selesai diproses. Lihat migration_activity_snapshot.sql
    -- untuk penjelasan lengkap kenapa kolom ini dibutuhkan.
    detail_snapshot LONGTEXT NULL,

    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_aktivitas_user (username),
    INDEX idx_aktivitas_jenis (jenis_aktivitas),
    INDEX idx_aktivitas_waktu (waktu_selesai),
    INDEX idx_aktivitas_reference (reference_id)
) ENGINE=InnoDB;

