use rusqlite::{params, Connection, Result};
use std::sync::Mutex;

// State untuk menyimpan koneksi database dan master pilihan gudang di memori
static DB_CONN: Mutex<Option<Connection>> = Mutex::new(None);
static MASTER_GUDANG: Mutex<Vec<String>> = Mutex::new(Vec::new());
static INIT: std::sync::Once = std::sync::Once::new();

// Fungsi untuk inisialisasi Database SQLite lokal di dalam HP
fn init_database() {
    INIT.call_once(|| {
        // Membuka atau membuat file database SQLite bernama arzip_lokal.db
        let conn = Connection::open("arzip_lokal.db").expect("Gagal membuat/membuka file SQLite .db");
        
        // 1. Buat tabel utama untuk menyimpan data warkah jika belum ada
        conn.execute(
            "CREATE TABLE IF NOT EXISTS kardus_arsip (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                gudang TEXT NOT NULL,
                lokasi TEXT NOT NULL,
                start_di INTEGER NOT NULL,
                end_di INTEGER NOT NULL,
                tahun TEXT NOT NULL
            )",
            [],
        ).expect("Gagal membuat tabel kardus_arsip");

        // 2. Buat tabel master gudang untuk fitur dinamis tombol setting (⚙️)
        conn.execute(
            "CREATE TABLE IF NOT EXISTS master_gudang (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_gudang TEXT NOT NULL UNIQUE
            )",
            [],
        ).expect("Gagal membuat tabel master_gudang");

        // 3. Masukkan data master bawaan jika tabel master masih kosong (agar user langsung punya opsi awal)
        let mut stmt = conn.prepare("SELECT COUNT(*) FROM master_gudang").unwrap();
        let count: i64 = stmt.query_row([], |row| row.get(0)).unwrap_or(0);
        
        if count == 0 {
            let default_gudang = vec!["Gudang Lama", "Gudang Baru Lantai 1", "Gudang Baru Lantai 2"];
            for g in default_gudang {
                let _ = conn.execute("INSERT INTO master_gudang (nama_gudang) VALUES (?1)", [g]);
            }
        }

        // Sinkronkan data master gudang dari SQLite ke dalam cache RAM Rust (MASTER_GUDANG)
        let mut stmt_gudang = conn.prepare("SELECT nama_gudang FROM master_gudang").unwrap();
        let gudang_iter = stmt_gudang.query_map([], |row| row.get::<_, String>(0)).unwrap();
        let mut mg = MASTER_GUDANG.lock().unwrap();
        for g in gudang_iter {
            if let Ok(nama) = g {
                mg.push(nama);
            }
        }

        // Simpan koneksi secara permanen di dalam Mutex global
        let mut db_slot = DB_CONN.lock().unwrap();
        *db_slot = Some(conn);
    });
}

// Fungsi performa tinggi: Merender list data langsung dari baris query SQLite (Maksimal dikunci demi kecepatan)
fn render_table_from_db(query_condition: &str, query_params: &[&dyn rusqlite::ToSql], limit: usize) -> String {
    let db_slot = DB_CONN.lock().unwrap();
    if let Some(conn) = db_slot.as_ref() {
        let sql = format!(
            "SELECT gudang, lokasi, start_di, end_di, tahun FROM kardus_arsip {} ORDER BY id DESC LIMIT {}",
            query_condition, limit
        );
        
        let mut stmt = match conn.prepare(&sql) {
            Ok(s) => s,
            Err(_) => return "Gagal memuat struktur tabel arsip.".to_string(),
        };

        let rows_iter = stmt.query_map(query_params, |row| {
            Ok((
                row.get::<_, String>(0)?,
                row.get::<_, String>(1)?,
                row.get::<_, i32>(2)?,
                row.get::<_, i32>(3)?,
                row.get::<_, String>(4)?,
            ))
        }).unwrap();

        let mut susunan_tabel = Vec::new();
        for (index, item) in rows_iter.enumerate() {
            if let Ok((gudang, lokasi, start_di, end_di, tahun)) = item {
                susunan_tabel.push(format!(
                    "{}. 📦 DI.208: {} - {}\n   🏢 {} | 📍 {}\n   📅 Tahun: {}\n   ──────────────────────",
                    index + 1, start_di, end_di, gudang, lokasi, tahun
                ));
            }
        }

        if susunan_tabel.is_empty() {
            if query_condition.is_empty() {
                "Belum ada data warkah lokal yang disimpan. Silakan entri di atas!".to_string()
            } else {
                "❌ Data tidak ditemukan di database lokal.".to_string()
            }
        } else {
            susunan_tabel.join("\n")
        }
    } else {
        "Database belum siap.".to_string()
    }
}

pub fn handle_action(action: &str, payload: &str) -> String {
    init_database();
    
    match action {
        "nav_menu" => "OPEN_DRAWER".to_string(),

        // Ambil data opsi gudang dari cache RAM untuk dropdown Kotlin
        "nav_setting" => {
            let mg = MASTER_GUDANG.lock().unwrap();
            if mg.is_empty() { "EMPTY".to_string() } else { mg.join("|") }
        }

        "tambah_master_gudang" => {
            let nama_baru = payload.trim();
            if nama_baru.is_empty() { return "ERROR_EMPTY".to_string(); }
            
            let db_slot = DB_CONN.lock().unwrap();
            if let Some(conn) = db_slot.as_ref() {
                // Simpan permanen ke tabel master SQLite
                let _ = conn.execute("INSERT OR IGNORE INTO master_gudang (nama_gudang) VALUES (?1)", [nama_baru]);
            }
            
            // Update cache RAM
            let mut mg = MASTER_GUDANG.lock().unwrap();
            if !mg.contains(&nama_baru.to_string()) {
                mg.push(nama_baru.to_string());
            }
            mg.join("|")
        }

        "hapus_master_gudang" => {
            let db_slot = DB_CONN.lock().unwrap();
            if let Some(conn) = db_slot.as_ref() {
                let _ = conn.execute("DELETE FROM master_gudang WHERE nama_gudang = ?1", [payload]);
            }
            
            let mut mg = MASTER_GUDANG.lock().unwrap();
            mg.retain(|x| x != payload);
            if mg.is_empty() { "EMPTY".to_string() } else { mg.join("|") }
        }

        // DI-TRIGGER SAAT APLIKASI PERTAMA BUKA: Ambil 5 data TERBARU dari database (.db)
        "ambil_tabel_awal" => {
            render_table_from_db("", &[], 5)
        }

        // PENCARIAN LIVE DAN SUPER CEPAT BERBASIS INDEX DI SQLITE
        "nav_search" => {
            if payload.is_empty() {
                return render_table_from_db("", &[], 5); // Jika kosong balik ke 5 data terbaru
            }

            let query = format!("%{}%", payload.trim().to_lowercase());
            let query_angka = payload.trim().parse::<i32>().unwrap_or(-1);

            // Jika yang diketik angka, cari yang memotong rentang start_di s/d end_di
            if query_angka != -1 {
                render_table_from_db(
                    "WHERE ?1 >= start_di AND ?1 <= end_di OR tahun LIKE ?2",
                    params![query_angka, query],
                    50
                )
            } else {
                // Jika yang diketik teks, cari berdasarkan nama gudang atau lokasi rak
                render_table_from_db(
                    "WHERE LOWER(gudang) LIKE ?1 OR LOWER(lokasi) LIKE ?1 OR tahun LIKE ?1",
                    params![query],
                    50
                )
            }
        }

        // SIMPAN PERMANEN DATA BARU KE SQLITE DATABASE (.db)
        "btn_simpan" => {
            let parts: Vec<&str> = payload.split('|').collect();
            if parts.len() < 4 || parts[0].is_empty() || parts[1].is_empty() || parts[2].is_empty() || parts[3].is_empty() {
                return "⚠️ Gagal! Pastikan semua form terisi.".to_string();
            }

            let gudang = parts[0].to_string();
            let lokasi = parts[1].to_string();
            let range_str = parts[2].replace(" ", "");
            let tahun = parts[3].to_string();

            let range_parts: Vec<&str> = range_str.split('-').collect();
            let start_di = range_parts.get(0).and_then(|s| s.parse::<i32>().ok()).unwrap_or(0);
            let end_di = range_parts.get(1).and_then(|s| s.parse::<i32>().ok()).unwrap_or(start_di);

            let db_slot = DB_CONN.lock().unwrap();
            if let Some(conn) = db_slot.as_ref() {
                // Eksekusi insert baris aman kilat
                let res_insert = conn.execute(
                    "INSERT INTO kardus_arsip (gudang, lokasi, start_di, end_di, tahun) VALUES (?1, ?2, ?3, ?4, ?5)",
                    params![gudang, lokasi, start_di, end_di, tahun],
                );

                if res_insert.is_ok() {
                    // Logika Cerdas: Kembalikan kode sukses disusul render data terupdate langsung dari DB .db
                    drop(db_slot); // Lepas lock sebelum memanggil fungsi query agar tidak deadlock
                    return format!("✅ SUKSES\n{}", render_table_from_db("", &[], 5));
                }
            }
            "⚠️ Gagal menyimpan ke database lokal internal HP.".to_string()
        }

        _ => format!("Aksi '{}' diterima.", action),
    }
}
