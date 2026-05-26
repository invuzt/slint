use rusqlite::{params, Connection};
use std::sync::Mutex;

static DB_CONN: Mutex<Option<Connection>> = Mutex::new(None);
static MASTER_GUDANG: Mutex<Vec<String>> = Mutex::new(Vec::new());
static INIT: std::sync::Once = std::sync::Once::new();

fn init_database() {
    INIT.call_once(|| {
        let db_path = "/storage/emulated/0/Download/arzip_lokal.db";
        let conn = Connection::open(db_path).expect("Gagal membuat file SQLite di folder Download");
        
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

        conn.execute(
            "CREATE TABLE IF NOT EXISTS master_gudang (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_gudang TEXT NOT NULL UNIQUE
            )",
            [],
        ).expect("Gagal membuat tabel master_gudang");

        // BLOCK SCOPE 1: Mengisolasi pengisian data default awal
        {
            let mut stmt = conn.prepare("SELECT COUNT(*) FROM master_gudang").unwrap();
            let count: i64 = stmt.query_row([], |row| row.get(0)).unwrap_or(0);
            
            if count == 0 {
                let default_gudang = vec!["Gudang Lama", "Gudang Baru Lantai 1", "Gudang Baru Lantai 2"];
                for g in default_gudang {
                    let _ = conn.execute("INSERT INTO master_gudang (nama_gudang) VALUES (?1)", [g]);
                }
            }
        } 

        // BLOCK SCOPE 2: Mengisolasi pembacaan master gudang ke cache RAM
        {
            let mut stmt_gudang = conn.prepare("SELECT nama_gudang FROM master_gudang").unwrap();
            let gudang_iter = stmt_gudang.query_map([], |row| row.get::<_, String>(0)).unwrap();
            let mut mg = MASTER_GUDANG.lock().unwrap();
            for g in gudang_iter {
                if let Ok(nama) = g {
                    mg.push(nama);
                }
            }
        } // <--- stmt_gudang hancur di sini, koneksi BEBAS dari pinjaman!

        // Simpan koneksi utuh ke slot global
        let mut db_slot = DB_CONN.lock().unwrap();
        *db_slot = Some(conn);
    });
}

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

        "nav_setting" => {
            let mg = MASTER_GUDANG.lock().unwrap();
            if mg.is_empty() { "EMPTY".to_string() } else { mg.join("|") }
        }

        "tambah_master_gudang" => {
            let nama_baru = payload.trim();
            if nama_baru.is_empty() { return "ERROR_EMPTY".to_string(); }
            
            let db_slot = DB_CONN.lock().unwrap();
            if let Some(conn) = db_slot.as_ref() {
                let _ = conn.execute("INSERT OR IGNORE INTO master_gudang (nama_gudang) VALUES (?1)", [nama_baru]);
            }
            
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

        "ambil_tabel_awal" => {
            render_table_from_db("", &[], 5)
        }

        "nav_search" => {
            if payload.is_empty() {
                return render_table_from_db("", &[], 5);
            }

            let query = format!("%{}%", payload.trim().to_lowercase());
            let query_angka = payload.trim().parse::<i32>().unwrap_or(-1);

            if query_angka != -1 {
                render_table_from_db(
                    "WHERE ?1 >= start_di AND ?1 <= end_di OR tahun LIKE ?2",
                    params![query_angka, query],
                    50
                )
            } else {
                render_table_from_db(
                    "WHERE LOWER(gudang) LIKE ?1 OR LOWER(lokasi) LIKE ?1 OR tahun LIKE ?1",
                    params![query],
                    50
                )
            }
        }

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
                let res_insert = conn.execute(
                    "INSERT INTO kardus_arsip (gudang, lokasi, start_di, end_di, tahun) VALUES (?1, ?2, ?3, ?4, ?5)",
                    params![gudang, lokasi, start_di, end_di, tahun],
                );

                if res_insert.is_ok() {
                    drop(db_slot); 
                    return format!("✅ SUKSES\n{}", render_table_from_db("", &[], 5));
                }
            }
            "⚠️ Gagal menyimpan ke database lokal internal HP.".to_string()
        }

        _ => format!("Aksi '{}' diterima.", action),
    }
}
