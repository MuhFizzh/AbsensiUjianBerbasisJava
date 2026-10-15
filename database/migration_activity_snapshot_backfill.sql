USE wmsdb;

-- ====================================================================
-- BACKFILL detail_snapshot untuk baris aktivitas_kerja yang SUDAH ADA.
--
-- Revisi: JSON_ARRAYAGG tidak dipakai lagi (tidak tersedia di semua
-- versi MySQL/MariaDB - baru ada di MySQL 5.7.22+ / MariaDB 10.5+).
-- Sebagai gantinya array JSON disusun manual pakai GROUP_CONCAT,
-- yang sudah ada sejak versi MySQL/MariaDB lama, dibungkus '[' ']'.
-- JSON_OBJECT tetap dipakai untuk 1 baris barang (ini biasanya sudah
-- tersedia sejak MySQL 5.7.8 / MariaDB 10.2.3).
--
-- group_concat_max_len dinaikkan dulu supaya barang yang jumlahnya
-- banyak dalam 1 transaksi tidak terpotong (default cuma 1024 byte).
-- ====================================================================

SET SESSION group_concat_max_len = 1000000;

-- 1) INBOUND
UPDATE aktivitas_kerja ak
SET ak.detail_snapshot = (
    SELECT CONCAT('[', GROUP_CONCAT(
        JSON_OBJECT(
            'Barang', b.nama,
            'Kode', b.kode,
            'Lokasi', l.kode,
            'Qty', CAST(d.qty AS CHAR)
        )
        ORDER BY d.id SEPARATOR ','
    ), ']')
    FROM inbound_detail d
    JOIN barang b ON d.barang_id = b.id
    JOIN lokasi_rak l ON d.lokasi_rak_id = l.id
    WHERE d.inbound_id = ak.reference_id
)
WHERE ak.jenis_aktivitas = 'INBOUND'
  AND ak.detail_snapshot IS NULL;

-- 2) OUTBOUND
UPDATE aktivitas_kerja ak
SET ak.detail_snapshot = (
    SELECT CONCAT('[', GROUP_CONCAT(
        JSON_OBJECT(
            'Barang', b.nama,
            'Kode', b.kode,
            'Lokasi', l.kode,
            'Qty', CAST(d.qty AS CHAR)
        )
        ORDER BY d.id SEPARATOR ','
    ), ']')
    FROM outbound_detail d
    JOIN barang b ON d.barang_id = b.id
    JOIN lokasi_rak l ON d.lokasi_rak_id = l.id
    WHERE d.outbound_id = ak.reference_id
)
WHERE ak.jenis_aktivitas = 'OUTBOUND'
  AND ak.detail_snapshot IS NULL;

-- 3) STOCK_OPNAME
-- Catatan: stock_opname_detail tidak menyimpan lokasi rak (sama
-- seperti tampilan live sebelumnya), jadi kolom "Lokasi" dikosongkan.
UPDATE aktivitas_kerja ak
SET ak.detail_snapshot = (
    SELECT CONCAT('[', GROUP_CONCAT(
        JSON_OBJECT(
            'Barang', b.nama,
            'Kode', b.kode,
            'Lokasi', '',
            'Sistem', CAST(d.system_qty AS CHAR),
            'Aktual', CAST(d.counted_qty AS CHAR),
            'Selisih', CAST(d.selisih AS CHAR)
        )
        ORDER BY d.id SEPARATOR ','
    ), ']')
    FROM stock_opname_detail d
    JOIN barang b ON b.id = d.barang_id
    WHERE d.opname_id = ak.reference_id
)
WHERE ak.jenis_aktivitas = 'STOCK_OPNAME'
  AND ak.detail_snapshot IS NULL;

-- 4) PERMINTAAN (selalu 1 baris per aktivitas)
UPDATE aktivitas_kerja ak
SET ak.detail_snapshot = (
    SELECT CONCAT('[', JSON_OBJECT(
        'Barang', COALESCE(b.nama, '-'),
        'Jumlah', COALESCE(CAST(p.jumlah AS CHAR), '-'),
        'Lokasi Asal', COALESCE(la.kode, '-'),
        'Lokasi Tujuan', COALESCE(lt.kode, '-')
    ), ']')
    FROM permintaan p
    LEFT JOIN barang b ON p.barang_id = b.id
    LEFT JOIN lokasi_rak la ON p.lokasi_asal_id = la.id
    LEFT JOIN lokasi_rak lt ON p.lokasi_tujuan_id = lt.id
    WHERE p.id = ak.reference_id
)
WHERE ak.jenis_aktivitas = 'PERMINTAAN'
  AND ak.detail_snapshot IS NULL;

-- ====================================================================
-- Ringkasan hasil, untuk dicek manual setelah dijalankan.
-- "masih_kosong" > 0 berarti transaksi aslinya sudah lebih dulu
-- terhapus sebelum backfill ini dijalankan.
-- ====================================================================
SELECT
    jenis_aktivitas,
    SUM(detail_snapshot IS NOT NULL) AS sudah_ada_snapshot,
    SUM(detail_snapshot IS NULL)     AS masih_kosong
FROM aktivitas_kerja
GROUP BY jenis_aktivitas;
