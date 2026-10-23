# Pengujian CampGear Android

Tanggal: 22 Oktober 2026. **Lolos build** dibedakan dari **lolos runtime/UI/API**. Tidak ada screenshot atau transaksi yang dinyatakan berhasil hanya karena layout/DTO dapat dikompilasi.

## Lingkungan audit

- Android: emulator `birthday_test`, Android16/API36, x86_64, 1080×2400; tersedia landscape2400×1080 dan mode gelap.
- Build: Android SDK37, AGP9.4.1, wrapper9.8.2, JBR Android Studio.
- Backend sumber read-only dibangun dengan output dan intermediate ke `/tmp/opencode/campgear-server*`, tanpa perubahan source. MariaDB aktif.
- Uji UI awal menggunakan backend sumber localhost:5162 dan database yang sudah ada; data master berlabel UKK yang dibuat untuk pemeriksaan telah dihapus kembali melalui API/UI. Loan/return pengujian berstatus final dan activity log merupakan jejak transaksi uji.
- Uji API dan Android regression berikutnya memakai skema terpisah `campgear_android_audit_...` yang dimuat dari seed `query.sql` (tanpa contoh transaksi akhir). Backend tersebut berjalan localhost:5163; **database lama tidak di-reset**.
- URL deployment pengguna `192.168.69.58:3001` timeout dari mesin audit; keberhasilan localhost tidak membuktikan deployment LAN tersebut bekerja.

## Ringkasan hasil terverifikasi

| Pemeriksaan | Hasil |
|---|---|
| Fase1 `assembleDebug` | BUILD SUCCESSFUL; versi pertama masih memiliki bug yang tidak diketahui compile |
| Fase1 `lint` | BUILD SUCCESSFUL, `No issues found.`; tidak membuktikan runtime |
| Fase2 `assembleDebug lint testDebugUnitTest` | BUILD SUCCESSFUL setelah error lint lifecycle diperbaiki |
| Fase3 build/lint | BUILD SUCCESSFUL setelah koreksi resource/style API24 dan resource yang belum terdaftar |
| Unit test revisi | 23 test, 0 failure: date/expiry/response contract/list-null/paging-overlap/pricing/imageURL |
| `connectedDebugAndroidTest` | 9 test Android, 0 failure dalam XML hasil |
| Runner Android langsung | `OK (9 tests)`; dijalankan untuk mengonfirmasi setelah host listener AGP sempat memunculkan error pelaporan |
| API nyata pada schema sementara | 20 pemeriksaan PASS (login3role/401/403/multi-item324000/approve/handover/returns295000&0/reject/edit230000/reportsCSV/paging) |
| UI manual via emulator | Login3role, dashboard, navigasi, catalog/detail/form, transaksi, CRUDmaster, edititems/reject/delete, reports/share, log/filter, dark/rotation berhasil pada skenario yang dijelaskan di bawah |

Toolchain memiliki pemberitahuan deprecated dari Gradle/plugin/test-runner (misalnya Unsafe protobuf/AGP listener), bukan error Kotlin aplikasi. Pernah muncul crash navigasi pada runtime, diperbaiki dan ditest ulang; tidak disembunyikan sebagai warning kosmetik.

## Tabel skenario (hasil nyata)

| ID | Skenario | Input / ekspektasi | Hasil aktual | Bukti / batas |
|---|---|---|---|---|
| TC01 | Login Admin | Seed081200000001/admin123 → Beranda + drawerAdmin | **Lolos UI dan Androidtest** | API200; headerAdmin; menus hanyaAdmin/shared |
| TC02 | Login Petugas | Seed081200000002/petugas123 → Beranda + drawerPetugas | **Lolos UI dan Androidtest** | API200, count pending/borrowed/monitor, route setelah rotasi |
| TC03 | Login Peminjam | Seed081200000003/peminjam123 → Beranda + bottomnav | **Lolos UI dan Androidtest** | API200; home/catalog/myLoans/profile; bug routeInt diperbaiki sebelum pass |
| TC04 | Login kosong/salah | Inline field kosong; salah kata sandi401 | **Lolos inline Androidtest + API401** | Pesan Indonesia; login salah UI tambahan perlu diulang pada perangkat fisik |
| TC05 | Katalog/detail/keranjang | Search/kategori, detail stok/price, CTA | **Lolos UI** | Detailalat tampil; missing file404 memakai placeholder; foto uploaded baru belum diuji UI penuh |
| TC06 | Pengajuan dengan estimasi | Alat aktualRp20.000×1×2hari=Rp40.000; dateend>=start | **Lolos UI/transaksi** | PreviewRp40.000; loan#10 tersimpanRp40.000, masuk mine |
| TC07 | Estimasi multi alat seed | 2×tenda50.000 +1×headlamp8.000;3hari | **Lolos API + unit test** | TotalserverRp324.000; UI multiitem seed belum diulang penuh |
| TC08 | Tanggal invalid | Kembali21Okt < pinjam22Okt | **Lolos UI** | Submit tidak terkirim; TextInputLayout inline error |
| TC09 | Rotasi borrow | Portrait→landscape→portrait dengan cart/date/total | **Lolos UI** | Rp40.000/cart/date tetap; sticky submit dapat diakses landscape |
| TC10 | Approve | Petugas loan diajukan, konfirmasi | **Lolos UI/API** | PATCH200, statusDisetujui/idPetugas, actionhandover muncul |
| TC11 | Reject | Diajakukan + catatan opsional | **Lolos UI/API** | StatusDitolak; catatan muncul di log; alasan bukan field detailloan |
| TC12 | Handover | Disetujui, stokcukup | **Lolos UI/API** | PATCH200, statusDipinjam, unitcodes diserahkan; validasi stokserver |
| TC13 | Return dengan fine | UI:2late +unitrusakringan25.000 | **Lolos UI/API** | Receiptserver20.000+25.000=45.000; condition/catatan disimpan |
| TC14 | Return fines seed | 2late +baik/ringan/hilang | **Lolos API** | Total295.000, unitstate sesuai; semua unit dikirim |
| TC15 | Return tanpa fine | On-time/allbaik | **Lolos API + receiptUI** | Denda0; detail menampilkan late0/condition0/total0 |
| TC16 | Rotasi return | KondisiRusakRingan, catatanRotasi | **Lolos UI** | Data tetap setelah landscape/portrait; cursor bug rebind diperbaiki |
| TC17 | Role restriction | Peminjam GETusers; jangan hapustoken403 | **Lolos API + Androidtest** | Failure403 pesanIndonesia, session dipertahankan |
| TC18 | Expired/invalid token401 | Tokeninvalid dengan expirylokalfuture; atau timer500ms | **Lolos Androidtest** | Tokenclear, destinationLogin; timerlogout tanpa request; stale401 tidak menghapus sessionbaru |
| TC19 | No connection/timeout | API IO gagal; retryread-only | **Lolos Android mapping + kondisiUI nyata** | Snackbar/errorIndonesia pernah muncul saat request terganggu; skenarioairplanemode end-to-end perlu ujiulang |
| TC20 | Response malformed/null | GET200null, user/itemsnull, dateinvalid | **Lolos unit + Androidtest** | Failure/JsonParseException tertangkap, bukan ClassCastException; unexpecteddata tidak dipakai UI |
| TC21 | CRUD kategori | TambahUKKAudit, ubahBaru, rotasi, delete | **Lolos UI** | Listrefresh setelahform; draftnama tetap; recordsuji dihapus |
| TC22 | CRUD pengguna | Tambahnomorunik/katasandi, updateblankpassword, delete | **Lolos UI** | Emptyfield validation; requestsuccess/listrefresh/delete; akunseed tidak diubah |
| TC23 | CRUD alat/unit | AlatUKKprice12500/categoryTenda, unitkodeunique, edit/delete | **Lolos UI (tanpa uploadfoto)** | Stock/conditionlist; unituji dihapus lalu alatdelete; multipart tanpa gambar verified |
| TC24 | Edit items Admin | HapusTenda dariloan#4, tambahHeadlamp via picker | **Lolos UI/API** | PUTitembaru; Kompor15.000+Headlamp8.000×2=Rp46.000; detailrefresh |
| TC25 | Admin delete rejected | Loan#4Ditolak → deleteconfirm | **Lolos UI/API** | DELETE200; kembali list, logdelete |
| TC26 | Reports JSON/CSV | RangeOktober/types, sharefile | **Lolos UI/API** | Summarybiaya/denda benar, androidSharesheetCSV muncul; belum membukafile diExcel/Drive |
| TC27 | Log/filter | Relativetime/userpickerTerapkan | **Lolos UI/API** | FilterPetugas hanyaactorPetugas; absolute timeaccessibility; pagina2nooverlapAPI |
| TC28 | Darkmode | Receipt dan CRUD/profile/nav | **Lolos UI padaAPI36** | Paletteforestsurface, contrasttext terlihat; belum auditcontrastotomatis seluruh state |
| TC29 | Largefont/smallphone/API24 | Touch>=48/grid/scroll/resourcecompat | **Lolos build/lint, perlu uji manual lebih luas** | Ada tokens/tablet/landscape; belum diuji semua ukurandensity/API24 |
| TC30 | Processdeathrestore | Systemkill aplikasi di tengahdraft | **Belum diverifikasi menyeluruh** | MasterSavedStateHandledraft non-password; cart/image/formlainvolatile; jangan dianggap offlinepersistensi |

## Cara menjalankan tes

```bash
# Pemeriksaan defaultLAN tanpa membutuhkan backendlive
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug lint testDebugUnitTest

# Androidtest pada emulator, API backend uji harus tersedia di host port5163
JAVA_HOME=/opt/android-studio/jbr ./gradlew connectedDebugAndroidTest -PapiBaseUrl=http://10.0.2.2:5163/api/v1/
```

TestAndroid memakai akunseed dan login/logout; pilih databasesandbox untuk tidak mencampur log dataujian. Endpointuji tetap backend nyata, bukan mock server. `ClientRegressionTest` tidak membuat fakeendpoint diapp. Hasilunit: `app/build/test-results/testDebugUnitTest/`; hasilAndroid: `app/build/outputs/androidTest-results/connected/debug/`; laporanlint: `app/build/reports/lint-results-debug.html`.

## Screenshot yang perlu Anda capture untuk laporan ujian

1. Splash/iconCampGear dan login branded (terang dan gelap).
2. Login errorinline/credentialssalah.
3. BerandaPeminjam + bottomnavigation dengan countAPI.
4. BerandaPetugas + drawername/role/countpending/borrowed/monitor.
5. BerandaAdmin + semua menu berikon.
6. Katalog/search/categorychips, imageatauplaceholder, stokHabis.
7. Detailalat besar/price/description/CTA.
8. Formloan cartstepper/date/duration/stickytotal (contoh324.000) dan errorend<start.
9. Daftarloan/statuschips dan timeline detailDiajukan/Disetujui/Dipinjam/Selesai/Ditolak.
10. Dialogapprove/reject/handover; detailassignedunitcodes.
11. Returnform perunitconditions/notes/estimasi breakdown.
12. Receiptwithfine (late+condition+totalserver) dan receiptzero.
13. AdminlistFAB/overflow dan form inlineerrors untukpengguna/kategori/alat/unit.
14. Fotoalatpreview/uploadsuccess (perlu capture setelahuji manual upload).
15. Editloan tambah/hapusitem + estimasitotalterbaru.
16. Monitorfilter jatuh tempo/terlambat dan hari/phone.
17. Riwayatreturn/datefilter/detail.
18. Logactivity/filteruser/relative time.
19. Reportdate-range/summary/list dan AndroidCSV Sharesheet.
20. 403message/sesipertahankan; 401loginlagi; noconnection+retry.
21. Portrait/landscape dan fontbesar minimal satuform/list; darktheme.

Screenshot emulator audit disimpan sementara di `/tmp/opencode/campgear-*.png` untuk pemeriksaan; itu bukan pengganti screenshotlaporan yang Anda ambil dari datasetfinal ujian.

## Hal yang harus diuji lagi di lingkungan pengguna

- ServerLAN3001 dan firewall/interface/HTTPredirect; physicalphone tidak dapat diuji dari buildlokal saja.
- Foto valid JPG/PNG/WebP dan >2MB, mime/URI provider yang berbeda; unggah + render-filedi server.
- Race stok409 saat dua client handover, pagination ketika recordsditambah/dihapus bersamaan, keyboard/D-pad/TalkBack.
- Retry refreshoffline dengan datalama, airplane/noDNS/timeout, rotatingMaterialpicker terbuka, semuaAPI24–37.
- CSV dengannama berisi `;`, quote/newline: backendbug tetapdicatat, tidak dianggap parserclientrusak.
- Mutasitimeout setelahcommit: refreshstatus terlebihdahulu, tidakautoretry mutasi.
