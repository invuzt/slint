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
static INIT: std::sync::Once = std::sync::Once::new();

fn init_database() {
    INIT.call_once(|| {
        let mut db = DATABASE.lock().unwrap();
        db.push(KardusArsip {
            gudang: "Gudang Baru Lantai 1".to_string(),
            lokasi: "Rak Biru-01".to_string(),
            start_di: 3526,
            end_di: 3550,
            tahun: "2025".to_string(),
        });
    });
}

pub fn handle_action(action: &str, payload: &str) -> String {
    init_database();
    let mut db = DATABASE.lock().unwrap();

    match action {
        "nav_menu" => "OPEN_DRAWER".to_string(),

        "nav_search" => {
            if payload.is_empty() {
                return "Ketik nomor DI.208, tahun, atau nama gudang...".to_string();
            }

            let query = payload.trim().to_lowercase();
            let mut hasil_pencarian = Vec::new();
            let query_angka = query.parse::<i32>().ok();

            for kardus in db.iter() {
                let mut cocok = false;

                // Pencarian via teks (Tahun, Nama Gudang, atau Detail Rak)
                if kardus.tahun.contains(&query) || 
                   kardus.gudang.to_lowercase().contains(&query) || 
                   kardus.lokasi.to_lowercase().contains(&query) {
                    cocok = true;
                }
                
                // Pencarian via nomor DI.208 (Smart Range)
                if let Some(nomor) = query_angka {
                    if nomor >= kardus.start_di && nomor <= kardus.end_di {
                        cocok = true;
                    }
                }

                if cocok {
                    hasil_pencarian.push(format!(
                        "🏢 [{}] -> {}\n   • Rentang DI.208: {} s/d {}\n   • Tahun Arsip: {}\n",
                        kardus.gudang, kardus.lokasi, kardus.start_di, kardus.end_di, kardus.tahun
                    ));
                }
            }

            if hasil_pencarian.is_empty() {
                format!("❌ Data '{}' tidak ditemukan di gudang manapun.", query)
            } else {
                format!(
                    "🔍 Hasil Analisis ArZip untuk '{}':\n\n{}",
                    payload,
                    hasil_pencarian.join("\n")
                )
            }
        }

        "btn_simpan" => {
            // Format payload baru: "gudang|lokasi|rentang|tahun"
            let parts: Vec<&str> = payload.split('|').collect();
            if parts.len() < 4 || parts[0].is_empty() || parts[1].is_empty() || parts[2].is_empty() || parts[3].is_empty() {
                return "⚠️ Gagal! Pastikan pilihan Gudang, Rak, Rentang, dan Tahun terisi.".to_string();
            }

            let gudang = parts[0].to_string();
            let lokasi = parts[1].to_string();
            let range_str = parts[2].replace(" ", "");
            let tahun = parts[3].to_string();

            let range_parts: Vec<&str> = range_str.split('-').collect();
            let start_di = range_parts.get(0).and_then(|s| s.parse::<i32>().ok()).unwrap_or(0);
            let end_di = range_parts.get(1).and_then(|s| s.parse::<i32>().ok()).unwrap_or(start_di);

            db.push(KardusArsip {
                gudang,
                lokasi,
                start_di,
                end_di,
                tahun,
            });

            "✅ BERHASIL DICATAT!\nData kardus baru sudah aman disimpan di memori Rust.".to_string()
        }

        _ => format!("Aksi '{}' diterima.", action),
    }
}
