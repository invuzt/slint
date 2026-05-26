use std::sync::Mutex;

// Struktur data untuk merepresentasikan satu buah kardus arsip pertanahan
#[derive(Clone)]
struct KardusArsip {
    lokasi: String,
    start_di: i32,
    end_di: i32,
    tahun: String,
}

// Menggunakan static Mutex agar database di memori Rust bersifat persisten dan bisa ditambah terus selama aplikasi hidup
static DATABASE: Mutex<Vec<KardusArsip>> = Mutex::new(Vec::new());
static INIT: std::sync::Once = std::sync::Once::new();

fn init_database() {
    INIT.call_once(|| {
        let mut db = DATABASE.lock().unwrap();
        // Memasukkan data bawaan berdasarkan foto kardus asli milikmu sebagai data awal
        db.push(KardusArsip {
            lokasi: "Rak Biru-01".to_string(),
            start_di: 3526,
            end_di: 3550,
            tahun: "2025".to_string(),
        });
        db.push(KardusArsip {
            lokasi: "Kardus Cokelat Lama".to_string(),
            start_di: 100,
            end_di: 250,
            tahun: "1988".to_string(),
        });
    });
}

pub fn handle_action(action: &str, payload: &str) -> String {
    init_database();
    let mut db = DATABASE.lock().unwrap();

    match action {
        "nav_menu" => "OPEN_DRAWER".to_string(),

        // SMART SEARCH: Bisa mengenali Pencarian Tahun ATAU Pencarian Nomor DI.208 di dalam rentang
        "nav_search" => {
            if payload.is_empty() {
                return "Ketik nomor DI.208 atau tahun warkah untuk mencari lokasi kardus...".to_string();
            }

            let query = payload.trim();
            let mut hasil_pencarian = Vec::new();

            // Coba cek apakah user mengetik angka nomor DI.208 spesifik
            let query_angka = query.parse::<i32>().ok();

            for kardus in db.iter() {
                let mut cocok = false;

                // 1. Cek kecocokan berdasarkan input tahun (string match)
                if kardus.tahun.contains(query) {
                    cocok = true;
                }
                
                // 2. SMART CHECK: Cek apakah nomor yang dicari masuk ke dalam rentang DI.208 kardus ini
                if let Some(nomor) = query_angka {
                    if nomor >= kardus.start_di && nomor <= kardus.end_di {
                        cocok = true;
                    }
                }

                if cocok {
                    hasil_pencarian.push(format!(
                        "📍 LOKASI: {}\n   • Rentang DI.208: {} s/d {}\n   • Tahun Arsip: {}\n",
                        kardus.lokasi, kardus.start_di, kardus.end_di, kardus.tahun
                    ));
                }
            }

            if hasil_pencarian.is_empty() {
                format!("❌ Warkah atau tahun '{}' belum tercatat di kardus manapun.", query)
            } else {
                format!(
                    "🔍 Hasil Analisis Pencarian '{}' (Found {}):\n\n{}",
                    query,
                    hasil_pencarian.len(),
                    hasil_pencarian.join("\n")
                )
            }
        }

        // Aksi Menyimpan Data Kardus Baru dari Form Android
        "btn_simpan" => {
            // Data dikirim dari Kotlin digabung menggunakan delimiter "|"
            // Format payload: "lokasi|rentang|tahun"
            let parts: Vec<&str> = payload.split('|').collect();
            if parts.len() < 3 || parts[0].is_empty() || parts[1].is_empty() || parts[2].is_empty() {
                return "⚠️ Gagal menyimpan! Pastikan semua kolom Form terisi.".to_string();
            }

            let lokasi = parts[0].to_string();
            let range_str = parts[1].replace(" ", "");
            let tahun = parts[2].to_string();

            // Memecah rentang seperti "3526-3550" menjadi start dan end
            let range_parts: Vec<&str> = range_str.split('-').collect();
            let start_di = range_parts.get(0).and_then(|s| s.parse::<i32>().ok()).unwrap_or(0);
            let end_di = range_parts.get(1).and_then(|s| s.parse::<i32>().ok()).unwrap_or(start_di);

            db.push(KardusArsip {
                lokasi: lokasi.clone(),
                start_di,
                end_di,
                tahun: tahun.clone(),
            });

            format!(
                "✅ BERHASIL DICATAT!\nKardus di [{}] untuk DI.208 nomor {} - {} ({}) sudah masuk sistem database Rust.",
                lokasi, start_di, end_di, tahun
            )
        }

        _ => format!("Aksi '{}' diterima.", action),
    }
}
