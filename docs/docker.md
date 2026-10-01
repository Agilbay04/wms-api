# Menjalankan WMS API dengan Docker

Docker Compose hanya menjalankan `wms-api`. PostgreSQL, Redis, dan Mailpit tetap menggunakan container yang sudah tersedia di HomeServer.

## Konfigurasi

Salin `.env.example` menjadi `.env`, lalu isi konfigurasi yang diperlukan.

```bash
cp .env.example .env
```

Pilih profile melalui `SPRING_PROFILES_ACTIVE`. Untuk development, gunakan `dev`.

Saat API berjalan di Docker, `localhost` berarti container `wms-api`, bukan laptop atau HomeServer. Karena itu, isi `WMS_DB_HOST`, `WMS_REDIS_HOST`, dan `WMS_MAIL_HOST` dengan alamat IP LAN atau hostname HomeServer yang dapat dijangkau oleh Docker engine.

Jangan commit `.env`. File tersebut berisi kredensial dan sudah dikecualikan dari Git serta Docker build context.

## Menjalankan aplikasi

Build image dan jalankan container di background. Makefile selalu memakai satu file `.env`.

```bash
make docker-build
make docker-up
```

Periksa log aplikasi.

```bash
make docker-logs
```

Swagger UI tersedia di:

```text
http://localhost:${WMS_SERVER_PORT}/swagger-ui/index.html
```

Nilai default `WMS_SERVER_PORT` adalah `8081`.

Untuk mencoba endpoint yang memerlukan autentikasi di Swagger UI:

1. Jalankan `POST /api/v1/auth/login` dengan akun yang tersedia.
2. Salin nilai `data.accessToken` dari respons.
3. Klik **Authorize** dan tempel token tersebut pada skema `bearerAuth` (tanpa awalan `Bearer `). Swagger UI menambahkan awalan itu ke header `Authorization` secara otomatis.
4. Jalankan endpoint master-data. Akses tetap mengikuti permission akun yang dipakai.

Hentikan container dengan:

```bash
make docker-down
```

Perintah ini hanya menghentikan `wms-api`; data PostgreSQL, Redis, dan Mailpit di HomeServer tidak dihapus.

## Data master contoh untuk development

Seeder master-data hanya berjalan dengan profile `dev` dan `SEEDER_ENABLE_DB_SETUP=true`. Nilai awal flag di `.env.example` adalah `false`. Pastikan `WMS_DB_HOST`, `WMS_DB_NAME`, dan kredensial menunjuk ke database development yang benar sebelum mengubah flag di `.env` dan menjalankan `make run` atau `make docker-up`.

Seeder menambahkan tiga kategori, dua belas produk, dua gudang, dan empat lokasi dari `src/main/resources/seed/*.csv`. Data masuk sesuai urutan relasi dan tidak menimpa baris yang sudah ada. Jika kode kategori atau gudang yang sama sudah ada tetapi tidak aktif atau telah dihapus secara soft-delete, startup akan gagal agar data lama tidak diubah diam-diam.

Setelah transaksi database berhasil, setiap percobaan seeding membuat `seed-results/seed-runs/<timestamp>/seed-result.json` pada proses lokal. Di Docker, hasilnya disimpan pada volume `wms_seed_results` agar tetap ada setelah container dibuat ulang dan tidak bergantung pada izin direktori `./exports` di host. Lihat lokasi file di container dengan `docker compose exec wms-api find /app/seed-results -name seed-result.json -print`. File hanya mencatat ID baris yang **baru** dibuat pada percobaan tersebut; jalankan ulang aplikasi untuk memeriksa bahwa percobaan kedua menghasilkan `resources: []`. Tidak ada cleanup otomatis. Jika ingin menghapus data contoh, gunakan ID dari file hasil dan hapus secara manual dalam urutan lokasi → produk → gudang → kategori, hanya pada database development dan hanya bila data belum dipakai transaksi.

Jika penulisan file hasil gagal **setelah** database commit, data yang sudah masuk tidak dibatalkan. Perbaiki direktori hasil, lalu cocokkan kode/SKU di file CSV dengan baris database untuk menemukan ID-nya; percobaan berikutnya akan melewati baris yang sudah ada dan tidak dapat membuat ulang laporan ID pertama.

Endpoint daftar kategori, produk, gudang, dan lokasi memakai `page=1&size=10` jika parameter dihilangkan. Contoh halaman berikutnya: `GET /api/v1/products?page=2&size=10`. Nomor halaman pada respons juga dimulai dari 1; `page=0` ditolak dengan HTTP 400.

## Export

Direktori `./exports` pada host dipasang ke `/app/exports` di container. File export yang dibuat aplikasi akan tersedia pada direktori lokal tersebut.

## Menjalankan tanpa Docker

Jalankan aplikasi lokal di foreground dengan:

```bash
make run
```

Target ini memuat `.env` hanya untuk proses Gradle. Gunakan perintah berikut untuk test dan menampilkan URL Swagger:

```bash
make test
make swagger
```
