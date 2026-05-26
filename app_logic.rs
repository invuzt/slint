use std::sync::Mutex;

#[derive(Clone)]
struct KardusArsip {
    gudang: String,
    lokasi: String,
    start_di: i32,
    end_di: i32,
    tahun: String,
}

static DATABASE: Mutex<Vec<KardusArsip>> = Mutex::new(Vec::new());
static MASTER_GUDANG: Mutex<Vec<String>> = Mutex::new(Vec::new());
static INIT: std::sync::Once = std::sync::Once::new();

fn init_database() {
    INIT.call_once(|| {
        let mut db = DATABASE.lock().unwrap();
        // Mockup data awal untuk contoh tabel live
        db.push(KardusArsip {
            gudang: "Gudang Baru Lantai 1".to_string(),
            lokasi: "Rak A-01".to_string(),
            start_di: 3526,
            end_di: 3550,
            tahun: "2025".to_string(),
        });
        db.push(KardusArsip {
            gudang: "Gudang Lama".to_string(),
            lokasi: "Sektor B".to_string(),
            start_di: 1000,
            end_di: 1250,
            tahun: "1988".to_string(),
        });

        let mut mg = MASTER_GUDANG.lock().unwrap();
        mg.push("Gudang Lama".to_string());
        mg.push("Gudang Baru Lantai 1".to_string());
        mg.push("Gudang Baru Lantai 2".to_string());
    });
}

// Fungsi helper untuk merender list data (maksimal 5 item terbaru jika tanpa query)
fn render_table_view(data_list: &[KardusArsip], limit: usize) -> String {
    if data_list.is_empty() {
        return "Belum ada data kardus yang terekam.".to_string();
    }

    let mut susunan_tabel = Vec::new();
    // Ambil data dari belakang (terbaru) dan batasi jumlahnya demi performa RAM
    for (index, kardus) in data_list.iter().rev().take(limit).enumerate() {
        susunan_tabel.push(format!(
            "{}. 📦 DI.208: {} - {}\n   🏢 {} | 📍 {}\n   📅 Tahun: {}\n   ──────────────────────",
            index + 1, kardus.start_di, kardus.end_di, kardus.gudang, kardus.lokasi, kardus.tahun
        ));
    }
    susunan_tabel.join("\n")
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
            if payload.trim().is_empty() { return "ERROR_EMPTY".to_string(); }
            let mut mg = MASTER_GUDANG.lock().unwrap();
            mg.push(payload.trim().to_string());
            mg.join("|")
        }

        "hapus_master_gudang" => {
            let mut mg = MASTER_GUDANG.lock().unwrap();
            mg.retain(|x| x != payload);
            if mg.is_empty() { "EMPTY".to_string() } else { mg.join("|") }
        }

        // Trigger dipanggil saat pertama kali aplikasi Android terbuka untuk memuat tabel awal
        "ambil_tabel_awal" => {
            let db = DATABASE.lock().unwrap();
            render_table_view(&db, 5)
        }

        "nav_search" => {
            let db = DATABASE.lock().unwrap();
            if payload.is_empty() {
                // Logika Cerdas: Jika search bar kosong, kembalikan ke 5 data entri terakhir
                return render_table_view(&db, 5);
            }

            let query = payload.trim().to_lowercase();
            let mut hasil_filtered = Vec::new();
            let query_angka = query.parse::<i32>().ok();

            for kardus in db.iter() {
                let mut cocok = false;
                if kardus.tahun.contains(&query) || 
                   kardus.gudang.to_lowercase().contains(&query) || 
                   kardus.lokasi.to_lowercase().contains(&query) {
                    cocok = true;
                }
                if let Some(nomor) = query_angka {
                    if nomor >= kardus.start_di && nomor <= kardus.end_di {
                        cocok = true;
                    }
                }
                if cocok {
                    hasil_filtered.push(kardus.clone());
                }
            }

            if hasil_filtered.is_empty() {
                format!("❌ Data '{}' tidak ditemukan.", query)
            } else {
                // Tampilkan semua data yang cocok dengan hasil pencarian
                render_table_view(&hasil_filtered, 50)
            }
        }

        "btn_simpan" => {
            let mut db = DATABASE.lock().unwrap();
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

            db.push(KardusArsip { gudang, lokasi, start_di, end_di, tahun });
            
            // Logika Cerdas: Kirimkan kode sukses disusul dengan visual data tabel terupdate langsung
            format!("✅ SUKSES\n{}", render_table_view(&db, 5))
        }

        _ => format!("Aksi '{}' diterima.", action),
    }
}
