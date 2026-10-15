USE wmsdb;

-- ====================================================================
-- BACKFILL riwayat (audit_log) untuk transaksi INBOUND yang sudah ada
-- SEBELUM fitur "Riwayat" ini dipasang.
--
-- Beda dengan backfill snapshot sebelumnya: riwayat mencatat KEJADIAN
-- diskrit (siapa melakukan apa, kapan) - bukan "state" yang bisa
-- disusun ulang dari data yang masih ada. Untuk kejadian yang memang
-- tidak pernah dicatat sama sekali di kolom manapun (siapa yang void,
-- siapa yang hapus draft), datanya betul-betul sudah hilang dan TIDAK
-- BISA direkonstruksi - itu bukan keterbatasan script ini, tapi karena
-- infonya memang tidak pernah disimpan di sistem lama.
--
-- Yang BISA dibackfill di sini:
--   1) "Terima Barang" - AKURAT, diambil dari aktivitas_kerja
--      (sudah menyimpan username + waktu_selesai untuk transaksi
--      INBOUND yang sudah COMPLETED).
--   2) "Buat Draft" - PARSIAL, waktu diambil dari inbound.created_at,
--      tapi username ditandai "(tidak tercatat)" karena kolom siapa
--      pembuat draft memang tidak pernah ada di tabel inbound.
--
-- Aman dijalankan berkali-kali: pakai NOT EXISTS supaya tidak bikin
-- baris duplikat kalau dijalankan ulang atau kalau audit_log-nya
-- sebagian sudah terisi (misal dari transaksi yang dibuat setelah
-- fitur ini aktif).
-- ====================================================================

-- 1) Terima Barang (akurat, dari aktivitas_kerja)
INSERT INTO audit_log (username, module, action, reference_id, description, created_at)
SELECT
    ak.username,
    'INBOUND',
    'Terima Barang',
    ak.reference_id,
    CONCAT('Barang diterima, stok diperbarui (', ak.jumlah_item, ' item, total qty ',
           ak.total_qty, '). Status: DRAFT -> COMPLETED  [dicatat otomatis dari riwayat lama]'),
    ak.waktu_selesai
FROM aktivitas_kerja ak
WHERE ak.jenis_aktivitas = 'INBOUND'
  AND ak.status = 'COMPLETED'
  AND NOT EXISTS (
      SELECT 1 FROM audit_log al
      WHERE al.module = 'INBOUND'
        AND al.reference_id = ak.reference_id
        AND al.action = 'Terima Barang'
  );

-- 2) Buat Draft (parsial - waktu ada, pelaku tidak diketahui)
INSERT INTO audit_log (username, module, action, reference_id, description, created_at)
SELECT
    '(tidak tercatat)',
    'INBOUND',
    'Buat Draft',
    i.id,
    CONCAT('Draft inbound ', i.nomor, ' dibuat. Status: - -> DRAFT  ',
           '[dicatat otomatis dari riwayat lama - pembuat draft tidak tercatat di data lama]'),
    i.created_at
FROM inbound i
WHERE NOT EXISTS (
    SELECT 1 FROM audit_log al
    WHERE al.module = 'INBOUND'
      AND al.reference_id = i.id
      AND al.action = 'Buat Draft'
);

-- ====================================================================
-- Ringkasan: jumlah transaksi inbound yang sekarang punya riwayat
-- vs yang masih kosong (kosong = dibuat & selesai sebelum fitur ini,
-- atau memang sudah dihapus/di-void sebelum fitur ini ada).
-- ====================================================================
SELECT
    i.status,
    COUNT(*) AS jumlah_transaksi,
    SUM(EXISTS (
        SELECT 1 FROM audit_log al WHERE al.module = 'INBOUND' AND al.reference_id = i.id
    )) AS sudah_ada_riwayat
FROM inbound i
GROUP BY i.status;
