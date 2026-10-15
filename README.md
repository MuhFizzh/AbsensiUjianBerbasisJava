# WMS - Modul Dashboard, Master Data, Inbound & Outbound

Aplikasi desktop Java (Swing) untuk sistem Warehouse Management System (WMS),
mencakup:

1. **Dashboard** — layar pertama saat aplikasi dibuka. Menampilkan ringkasan
   angka (barang aktif, transaksi Inbound/Outbound bulan ini, jumlah barang
   stok minim), riwayat 5 transaksi Inbound & Outbound terakhir, dan daftar
   barang yang stok totalnya (dijumlah dari semua lokasi rak) sudah di bawah
   nilai **minimum stock** yang diatur di Master Data Barang.
2. **Master Data** — Barang, Kategori, Supplier, Customer, Lokasi Rak, User
   (CRUD lengkap dengan pencarian).
3. **Inbound** — pencatatan penerimaan barang dari supplier. Transaksi dibuat
   sebagai **Draft** (header + daftar item), lalu difinalisasi lewat tombol
   **Terima** yang menambah stok sesuai item yang tercatat.
4. **Outbound** — pencatatan pengiriman barang ke customer. Sama seperti
   Inbound, transaksi dibuat sebagai **Draft**, lalu difinalisasi lewat
   tombol **Kirim**. Saat "Kirim", sistem **memvalidasi stok tersedia**
   terlebih dahulu — jika ada barang yang kurang di lokasi rak yang dipilih,
   transaksi dibatalkan seluruhnya (rollback) dan pesan error menunjukkan
   rincian kekurangannya.

Tampilan dibuat mengikuti desain mockup (warna navy `#0f2440`, aksen biru
`#2f6fed`, kartu putih dengan sudut membulat, badge status hijau/merah, dsb)
menggunakan komponen Swing kustom + [FlatLaf](https://www.formdev.com/flatlaf/)
sebagai Look and Feel modern.

---

## 1. Struktur Project

```
wms-masterdata/
├── pom.xml                          # konfigurasi Maven
├── database/
│   ├── schema.sql                   # skema LENGKAP (Master Data + Operations) + data contoh
│   └── migration_operations.sql     # migrasi TAMBAHAN saja (Stock, Inbound, Outbound) —
│                                     # pakai ini jika database sudah pernah dibuat sebelumnya
├── src/main/resources/
│   └── db.properties                # konfigurasi koneksi database
└── src/main/java/com/wms/
    ├── Main.java                    # entry point aplikasi
    ├── db/DatabaseConnection.java   # helper koneksi JDBC
    ├── model/                       # POJO: Barang, Kategori, ..., StockItem, InboundHeader/Item, OutboundHeader/Item
    ├── dao/                         # akses data (CRUD) + StockDAO, InboundDAO, OutboundDAO (transaksional)
    ├── util/                        # PasswordUtil (hash SHA-256), CodeGenerator (nomor/kode otomatis)
    └── ui/
        ├── UITheme.java             # palet warna & font sesuai mockup
        ├── MainFrame.java           # jendela utama: sidebar + topbar + 3 layar (Master Data/Inbound/Outbound)
        ├── component/               # komponen kustom (RoundedButton, BadgeLabel, SidebarPanel, dst)
        ├── dialog/                  # InboundFormDialog & OutboundFormDialog (form transaksi + line items)
        └── panel/                   # 6 panel Master Data + InboundPanel + OutboundPanel
```

## 2. Kebutuhan (Prerequisites)

- **JDK 17 atau lebih baru** (disarankan JDK 21)
- **NetBeans IDE** versi terbaru dengan plugin Maven (biasanya sudah bawaan)
- **MySQL Server 8.x** yang sedang berjalan
- Koneksi internet saat pertama kali membuka project (Maven akan mengunduh
  dependency MySQL Connector/J dan FlatLaf secara otomatis)

## 3. Setup Database

### Jika ini instalasi baru (belum pernah setup database sama sekali)
Jalankan seluruh isi `database/schema.sql` — sudah mencakup Master Data
**dan** Operations (Stock, Inbound, Outbound) sekaligus data contoh.
```bash
mysql -u root -p < database/schema.sql
```

### Jika database `wms_db` sudah pernah dibuat sebelumnya
Cukup jalankan file migrasi tambahan ini saja (aman dijalankan berkali-kali):
```bash
mysql -u root -p < database/migration_operations.sql
```
File ini hanya membuat tabel `stock`, `inbound`, `inbound_detail`, `outbound`,
`outbound_detail` yang belum ada, tanpa mengulang data Master Data yang sudah ada.

Setelah itu, sesuaikan kredensial koneksi di
`src/main/resources/db.properties` bila perlu:
```properties
db.url=jdbc:mysql://localhost:3306/wms_db?useSSL=false&serverTimezone=Asia/Jakarta&allowPublicKeyRetrieval=true
db.user=root
db.password=
```

## 4. Membuka & Menjalankan Project di NetBeans

1. Buka **NetBeans** → menu **File > Open Project…**
2. Arahkan ke folder `wms-masterdata` (folder yang berisi `pom.xml`) lalu klik **Open**.
   NetBeans akan otomatis mengenali ini sebagai **Maven Project**.
3. Klik kanan pada project → **Clean and Build** (tunggu Maven mengunduh dependency).
4. Klik kanan pada project → **Run**, atau tekan **F6**.

### Alternatif: menjalankan lewat terminal (tanpa NetBeans)

```bash
mvn clean package
java -jar target/wms-masterdata.jar
```

## 5. Menggunakan Aplikasi

### Master Data
Sama seperti sebelumnya — sidebar kiri → **Master Data** → 6 tab di atas
konten (Barang, Kategori, Supplier, Customer, Lokasi Rak, User).

### Inbound (Barang Masuk)
1. Sidebar kiri → **Inbound**
2. Klik **+ Buat Inbound** → isi tanggal, pilih supplier, catatan (opsional)
3. Di bagian **Tambah Item**: pilih barang, lokasi rak, qty → klik
   **+ Tambah ke Daftar** (ulangi untuk beberapa barang)
4. Klik **Simpan Transaksi** → transaksi tersimpan dengan status **Draft**
   dan nomor otomatis (`IN-0001`, dst)
5. Di daftar transaksi, klik **Terima** untuk memfinalisasi — stok akan
   otomatis bertambah sesuai item yang tercatat, status berubah **COMPLETED**
6. Transaksi berstatus Draft juga bisa **Dihapus**; transaksi yang sudah
   Completed tidak bisa diedit/dihapus (untuk menjaga integritas riwayat stok)

### Outbound (Barang Keluar)
Alurnya sama seperti Inbound, tapi memilih **Customer** dan tombol
finalisasinya **Kirim**. Perbedaan penting: saat **Kirim** diklik, sistem
mengecek stok yang tersedia di lokasi rak masing-masing item. Jika ada
barang yang stoknya tidak cukup, seluruh transaksi **dibatalkan** (tidak ada
stok yang berubah sama sekali) dan muncul pesan rinci barang mana yang kurang.

## 6. Detail Teknis Penting

- **Kode/nomor otomatis**: `BRG-001`, `KTG-01`, `SUP-01`, `CUS-01`, `USR-01`,
  `LOK-A01` (Master Data) dan `IN-0001`, `OUT-0001` (transaksi), semua
  dihasilkan otomatis oleh `CodeGenerator`.
- **Stok** disimpan per kombinasi (barang, lokasi rak) di tabel `stock`.
  Inbound menambah qty, Outbound mengurangi qty — keduanya lewat method
  `StockDAO.adjustQty()` yang dijalankan **dalam satu transaksi database**
  bersama proses finalisasi, sehingga tidak akan ada data stok yang
  "nyangkut" setengah jalan bila terjadi error.
- **Refresh otomatis**: semua panel (Master Data, Inbound, Outbound)
  mengambil ulang data terbaru dari database setiap kali dibuka —
  lihat `RefreshablePanel` — supaya perubahan dari layar lain (mis. kategori
  baru, atau stok yang berubah) langsung terlihat tanpa perlu restart aplikasi.
- **Password user** disimpan dalam bentuk **hash SHA-256** (lihat
  `PasswordUtil`), tidak pernah disimpan sebagai teks biasa.
- Semua akses database menggunakan **JDBC murni** (PreparedStatement,
  try-with-resources) melalui pola **DAO**, tanpa framework ORM.

## 7. Ringkasan Cakupan Modul

Seluruh menu di sidebar **sudah dibangun dan fungsional**:
- **Dashboard** — ringkasan jumlah barang, stok menipis, dan aktivitas
  transaksi terbaru (Inbound/Outbound)
- **Master Data** — Barang, Kategori, Supplier, Customer, Lokasi Rak, User
- **Inbound** — penerimaan barang dari supplier, update stok otomatis
- **Outbound** — pengiriman barang ke customer, dengan validasi stok
- **Create Request** — pengajuan permintaan (pindah lokasi stok, permintaan
  barang baru, atau lainnya)
- **Approval** — menyetujui/menolak permintaan yang diajukan lewat Create
  Request

### Create Request & Approval — alur singkat
1. Sidebar → **Create Request** → isi jenis permintaan (pindah lokasi /
   permintaan barang / lainnya), barang, jumlah, keterangan, nama pemohon →
   **Ajukan**. Status awal: **Menunggu**.
2. Sidebar → **Approval** → daftar permintaan berstatus Menunggu tampil
   sebagai antrian (FIFO, yang paling lama diajukan di atas) → klik
   **Setujui** atau **Tolak**, opsional isi catatan approval.
3. Permintaan yang sudah diproses (Disetujui/Ditolak) tercatat sebagai
   riwayat dan tidak bisa diajukan ulang approval-nya.

Catatan: proses **Setujui** pada Create Request bertipe "Pindah Lokasi"
saat ini mengubah status permintaan saja dan **belum** otomatis memindahkan
baris stok antar lokasi rak di tabel `stock` — ini area yang masih bisa
dikembangkan lebih lanjut bila dibutuhkan (mis. memanggil `StockDAO.adjustQty`
dua kali: kurangi di lokasi asal, tambah di lokasi tujuan, dalam satu
transaksi, mirip pola yang dipakai Inbound/Outbound).

Struktur project ini (model → dao → panel/dialog) konsisten dipakai di semua
modul, sehingga pengembangan fitur baru berikutnya bisa mengikuti pola yang
sama.

