# Warehouse Management

## System Scope
1. **Authentikasi & Authorisasi:** Proses verifikasi pengguna dimana semua proses harus dilakukan dalam kondisi user sudah login, dan sistem memastikan user yang login mendapatkan akses yang diberikan.
2. **Master Data:** Pengelolaan data yang digunakan sebagai data utama yang bersifat tetap dan sering digunakan sebagai referensi utama dalam proses transaksional. Sistem ini memiliki beberapa master data diantaranya:
    1. Produk: Produk fisik yang disimpan, contoh: laptop, tablet, meja kerja, dll
    2. Kategori Produk: kategori atas produk, contoh: elektronik, furnitur, spare part, dll
    3. Warehouse: gudang penyimpanan atas perusahaan tersebut, contoh: Gudang Malang Pakis (WH-Malang-01), Gudang Surabaya Rungkut (WH-Surabaya-01), dll
    4. Warehouse Location: Lokasi penyimpanan atas gudang tersebut, fisiknya bisa berupa rak.
3. **In-Bound:** Fitur ini digunakan untuk mengelola barang yang akan masuk kedalam warehouse. Dalam bisnis nya secara umum, proses ini melakukan penerimaan dan pengecekan item atas PO (Purchase Order)
4. **Inventory:** Proses ini digunakan untuk melakukan inventory / penyimpanan barang setelah barang diterima, proses penyimpanan dilakukan ke dalam warehouse dan warehouse location tujuan. Pada prosesnya sistem harus dapat mencover kebutuhan berikut:
    1. Stock Transfer: Melakukan transfer stock antar warehouse location (lokasi penyimpanan gudang)
    2. Stock Adjustment: Melakukan adjustment stock contoh jika ada barang rusak, hilang, dsb
5. **Out-Bound:** Proses yang dilakukan jika ada barang keluar gudang. Pada proses nya akan melakukan pengurangan barang dari suatu warehouse dan warehouse location
6. **Reporting:** Fitur dalam sistem yang dilakukan untuk memberikan laporan atas transaksional yang telah dilakukan. Kebutuhannya saat ini cukup sebagai berikut:
    1. Stock: Summary stock atas masing masing barang dan sistem dapat melakukan exporting data.
    2. Stock Movement: historical data yang mencatat pergerakan stock produk dari proses in-bound (stock ditambahkan), stock transfer inventory (stock tetap hanya pindah lokasi), stock adjustment inventory (stock berkurang atau bertambah), sampai out-bound. Dam, sistem dapat melakukan exporting data.
7. **Dashboard:** Fitur untuk menampilkan statistik data yang ada pada sistem. Data dashboard yang ditampilkan wajib difilter berdasarkan Warehouse, setidaknya dashboard mencakup informasi:
    1. Total Product
    2. Total Stock
    3. Low Stock Product
    4. Out of Stock Product
    5. Pending In-Bound
    6. Pending Stock Transfer
    7. Pending Stock Adjustment
    8. Pending Out-Bound
8. **Audit Trail:** Sistem harus menyediakan histori semua aktivitas atas user untuk mendukung penelusuran transaksi. Setidaknya sistem mencatat informasi berikut:
    1. User: user yang melakukan aksi tersebut
    2. Waktu: kapan aksi tersebut dilakukan
    3. Aksi: aksi apa yang dilakukan (menambahkan, mengubah, menghapus, request approval, approve, reject, dll)
    4. Tipe Data / Transaksi: indentifier data dari proses apa yang dilakukan aksi (master data warehouse, master data product, master data warehouse location, stock movement, stock adjustment, etc)
    5. Referensi Data: ID dari data yang dilakukan aksi
    6. Deskripsi: tambahan informasi, contoh diisi jika melakukan rejection, maka rejection note yang diisi

* * *
## Actor / Role
1. Superadmin: User full access
2. Supervisor: Akses fitur In-Bound, Out-Bound, Inventory, dan Reporting
3. Staff: Melakukan proses In Bound, Out Bound, dan Inventory

**Key Note:**
*   1 User dapat memiliki beberapa Role, 1 Role dapat memiliki beberapa Permission.
*   1 User memiliki > 1 Warehouse. Dan data user yang ditampilkan harus berdasarkan Warehouse yang dimiliki.

* * *
## User Access & Data Visibility
### Role & Permission
1. User dapat memiliki lebih dari satu Role.
2. Setiap Role dapat memiliki beberapa Permission.
3. Permission menentukan fitur dan tindakan yang dapat dilakukan oleh user.
4. Akses terhadap suatu fitur tidak secara otomatis memberikan akses terhadap seluruh data Warehouse.
### Warehouse Access
1. User dapat memiliki akses ke satu atau lebih Warehouse.
2. Data transaksi, stock, laporan, dan dashboard harus mengikuti Warehouse yang dapat diakses oleh user.
3. User tidak dapat melihat atau memproses transaksi dari Warehouse yang tidak menjadi hak aksesnya.
4. Pemilihan Warehouse pada halaman transaksi dan laporan harus dibatasi berdasarkan hak akses user.

* * *
## Flow Process Utama
### In-Bound
1. **Staff** melakukan pencatatan In-Bound (barang masuk) kedalam Warehouse dan melakukan pencatatan barang apa saja yang masuk. Dalam satu proses In-Bound dapat memiliki multiple Product
2. **Supervisor** melakukan review approval atas data In-Bound yang dibuat oleh **Staff.** Proses nya memiliki 3 status:
    1. Pending: Belum di proses
    2. Approved: Sudah di approve, dan barang In-Bound dengan status ini dapat dimasukkan kedalam inventory
    3. Rejected: ditolak, dan akan dilakukan revise oleh **Staff**
3. **Staff** melakukan proses inventory kedalam Warehouse Location untuk data In-Bound yang sudah di-approve oleh **Supervisor**, proses inventory akan melakukan:
    1. penambahan penempatan barang ke Warehouse Location,
    2. penambahan stock, dan
    3. pencatatan Stock Movement dengan tipe **In-Bound**
4. jika **Supervisor** melakukan Reject, maka data In-Bound dikembalikan ke **Staff** terkait untuk dilakukan revisi.
5. **Staff** melakukan revisi atas data In-Bound, dan akan dilakukan request pengajuan review kembali kepada **Supervisor**
### Out-Bound
1. **Staff** melakukan pencatatan Out-Bound (barang keluar) dari Warehouse dan melakukan pencatatan barang apa saja yang keluar, dan dari Warehouse Location mana. Satu proses Out-Bound dapat terjadi untuk multiple Product
2. **Supervisor** melakukan review approval atas data Out-Bound yang dibuat oleh **Staff.** Prosesnya memiliki 3 status:
    1. Pending: Belum di proses
    2. Approved: Sudah disetujui
    3. Rejected: ditolak dan dapat dilakukan revise oleh **Staff**
3. jika **Supervisor** melakukan approve sistem akan melakukan:
    1. pengurangan stock atas barang dari Warehouse Location,
    2. pengurangan stock, dan
    3. pencatatan Stock Movement dengan tipe **Out-Bound**
4. jika **Supervisor** melakukan Reject, maka data Out-Bound dikembalikan ke **Staff** terkait untuk dilakukan revisi.
5. **Staff** melakukan revisi atas data Out-Bound, dan akan dilakukan request pengajuan review kembali kepada **Supervisor**
### Inventory
#### Stock Transfer
1. **Staff** membuat pengajuan data Stock Transfer dengan memilih barang dari warehouse location dan tujuan Warehouse Location
2. **Supervisor** melakukan review approval atas data Stock Transfer yang dibuat oleh **Staff.** Prosesnya memiliki 3 status:
    1. Pending: Belum diproses
    2. Approved: Sudah disetujui
    3. Rejected: Ditolak dan perlu dilakukan revise oleh **Staff**
3. Jika **Supervisor** melakukan aksi Approve, maka pada prosesnya sistem akan melakukan:
    1. Pengurangan stock barang atas Warehouse Location sumber, dan penambahan ke Warehouse Location tujuan
    2. Sistem juga menulis Stock Movement dengan tipe **Stock Transfer**
4. jika **Supervisor** melakukan Reject, maka data Stock Transfer dikembalikan ke **Staff** terkait untuk dilakukan revisi.
5. **Staff** melakukan revisi atas data Stock Transfer, dan akan dilakukan request pengajuan review kembali kepada **Supervisor**
#### Stock Adjustment
1. **Staff** membuat data pengajuan stock adjustment dengan memilih warehouse location, barang terkait, tipe adjustment (Stock Opname, Barang Rusak atau Cacat) dan detail alasan atas adjustment
2. **Supervisor** melakukan review approval atas data Stock Adjustment yang dibuat oleh **Staff.** Prosesnya memiliki 3 status:
    1. Pending: Belum disetujui
    2. Approved: Sudah disetujui
    3. Rejected: ditolak dan perlu dilakukan revise oleh **Staff**
3. Jika **Supervisor** melakukan aksi Approve, maka pada prosesnya sistem akan melakukan:
    1. Pengurangan stock barang
    2. Pengurangan stock barang atas Warehouse Location
    3. Sistem juga menulis Stock Movement dengan tipe **Stock Adjustment**
4. jika **Supervisor** melakukan Reject, maka data Stock Adjustment dikembalikan ke **Staff** terkait untuk dilakukan revisi.
5. **Staff** melakukan revisi atas data Stock Adjustment, dan akan dilakukan request pengajuan review kembali kepada **Supervisor**

* * *
## Project Objective
Sistem Warehouse Management dikembangkan untuk membantu perusahaan mengelola operasional pergudangan secara terstruktur, mulai dari penerimaan barang, penyimpanan, perpindahan stock, penyesuaian stock, hingga pengeluaran barang.

1. Memusatkan pencatatan transaksi pergudangan dalam satu sistem.
2. Memastikan informasi stock sesuai dengan pencatatan transaksi yang dilakukan.
3. Menyediakan informasi stock berdasarkan Warehouse, Warehouse Location, dan Product.
4. Menyediakan mekanisme approval untuk transaksi yang membutuhkan persetujuan Supervisor.
5. Menyediakan histori pergerakan stock yang dapat ditelusuri.
6. Menyediakan laporan dan dashboard untuk membantu monitoring operasional warehouse.

* * *
## Functional Business Requirements

| ID | Requirement |
| ---| --- |
| BR-001 | Setiap transaksi harus dilakukan oleh user yang telah login. |
| BR-002 | User hanya dapat mengakses data Warehouse yang menjadi hak aksesnya. |
| BR-003 | Setiap transaksi harus memiliki nomor referensi yang unik. |
| BR-004 | Transaksi yang membutuhkan approval harus disetujui oleh Supervisor sebelum perubahan stock dilakukan. |
| BR-005 | Stock tidak boleh menjadi negatif akibat transaksi. |
| BR-006 | Setiap perubahan stock harus menghasilkan pencatatan Stock Movement. |
| BR-007 | Stock Transfer tidak mengubah total stock Product, tetapi mengubah distribusi stock antar Warehouse Location. |
| BR-008 | Stock Adjustment harus memiliki tipe adjustment dan alasan yang menjelaskan perubahan stock. |
| BR-009 | Transaksi yang telah selesai diproses tidak boleh diubah secara langsung tanpa mekanisme koreksi yang sesuai. |
| BR-010 | Setiap transaksi harus mencatat user dan waktu terjadinya aktivitas. |
| BR-011 | Proses export data dilakukan secara asynchronous dan hasil export akan dikirimkan melalui email user terkait |

* * *
## Non-Functional Requirements

| ID | Category | Requirement |
| ---| ---| --- |
| NFR-001 | Security | Sistem mewajibkan autentikasi sebelum user mengakses fitur. |
| NFR-002 | Authorization | Sistem menerapkan Role, Permission, dan pembatasan akses Warehouse. |
| NFR-003 | Data Integrity | Sistem menjaga konsistensi Stock Balance dan Stock Movement. |
| NFR-004 | Concurrency | Sistem mencegah perubahan stock yang tidak konsisten akibat transaksi bersamaan. |
| NFR-005 | Auditability | Sistem menyediakan histori transaksi dan aktivitas penting. |
| NFR-006 | Performance | Sistem harus memiliki waktu respons yang memadai untuk operasional warehouse. |
| NFR-007 | Reliability | Kegagalan transaksi tidak boleh menyebabkan perubahan stock sebagian. |
| NFR-008 | Maintainability | Sistem dikembangkan dengan struktur yang memudahkan pemeliharaan dan pengembangan fitur berikutnya. |

* * *

## Database Schema / ERD Design

Berikut adalah rekomendasi desain database atas project usecase berikut. Gunakan DBML design berikut sebagai referensi, dan feel free jika dimodifikasi sesuai dengan kebutuhan.

[

dbdiagram.io

https://dbdiagram.io/d/Training-Warehouse-Management-6aba10585869425612b08329

](https://dbdiagram.io/d/Training-Warehouse-Management-6aba10585869425612b08329)