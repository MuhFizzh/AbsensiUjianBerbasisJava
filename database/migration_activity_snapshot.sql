USE wmsdb;

-- =========================================================
-- Snapshot detail barang pada aktivitas_kerja
--
-- Sebelumnya, layar "Laporan Aktivitas Kerja" mengambil rincian
-- barang (nama barang, lokasi, qty, dst) dengan JOIN LIVE ke tabel
-- transaksi aslinya (inbound_detail/outbound_detail/stock_opname_detail/
-- permintaan) berdasarkan reference_id yang tersimpan di aktivitas_kerja.
--
-- Konsekuensinya: kalau baris transaksi aslinya berubah atau hilang
-- (mis. data barang direname/dihapus, atau transaksi terkait terhapus),
-- rincian yang tampil di Aktivitas Kerja ikut berubah/hilang, padahal
-- Aktivitas Kerja seharusnya menjadi catatan riwayat yang utuh.
--
-- Kolom detail_snapshot menyimpan SALINAN rincian barang (dalam
-- format JSON, dibuat oleh ActivityLogger/SnapshotUtil) persis pada
-- saat transaksi tersebut selesai diproses. Setelah tersimpan, isi
-- kolom ini tidak lagi bergantung pada data transaksi asli.
--
-- Dibuat sebagai ALTER TABLE terpisah (bukan langsung di
-- migration_activity.sql) supaya aman dijalankan berkali-kali dan
-- tidak mengganggu database yang tabel aktivitas_kerja-nya sudah ada
-- lebih dulu (baris lama akan punya detail_snapshot = NULL, dan
-- aplikasi otomatis fallback ke cara lama/live-query untuk baris itu).
-- =========================================================

SET @kolom_sudah_ada := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'wmsdb'
      AND TABLE_NAME = 'aktivitas_kerja'
      AND COLUMN_NAME = 'detail_snapshot'
);

SET @ddl := IF(
    @kolom_sudah_ada = 0,
    'ALTER TABLE aktivitas_kerja ADD COLUMN detail_snapshot LONGTEXT NULL AFTER total_qty',
    'SELECT ''Kolom detail_snapshot sudah ada, migration dilewati.'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
