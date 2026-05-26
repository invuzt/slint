/// Fungsi pusat untuk menangani segala aksi interaksi UI dari mesin Kotlin.
pub fn handle_action(action: &str, payload: &str) -> String {
    // SMART SEARCH STATE MANAGEMENT:
    // Anggap ini adalah representasi database dinamis yang datanya bisa berubah kapan saja.
    // Fungsi search di bawah akan selalu membaca kondisi data terbaru saat ini (Dinamis).
    let mut database_catatan: Vec<String> = vec![
        "Beli bahan makanan di pasar".to_string(),
        "Jadwal makan siang bareng tim Cakru".to_string(),
        "Belajar pemrograman Rust dan Kotlin".to_string(),
        "Push kode project vuzt ke GitHub".to_string(),
        "Jangan lupa minum air putih yang banyak".to_string(),
    ];

    // Contoh simulasi: Jika tombol update ditekan, kita pura-pura menambah data baru ke database 
    // untuk membuktikan kalau pencarian ini "Smart" dan otomatis mengenali perubahan data.
    if action == "btn_update" || action == "update_action" {
        database_catatan.push("Catatan Baru: Pembaruan Cakru Tiny Berhasil!".to_string());
        return "Memulai proses unduhan komponen Cakru Tiny...\n(Database Rust diperbarui, coba cari kata 'Pembaruan')".to_string();
    }

    match action {
        // Aksi Ikon Hamburger Menu
        "nav_menu" => {
            "OPEN_DRAWER".to_string()
        }

        // SMART SEARCH LOGIC (Case-Insensitive & Terisolasi dari Perubahan Data)
        "nav_search" => {
            if payload.is_empty() {
                return "Ketik di atas untuk mulai menyaring catatan...".to_string();
            }

            let keyword = payload.to_lowercase();
            
            // Rust melakukan iterasi langsung pada state database terbaru
            let hasil_filter: Vec<String> = database_catatan
                .iter()
                .filter(|catatan| catatan.to_lowercase().contains(&keyword))
                .cloned()
                .collect();

            if hasil_filter.is_empty() {
                format!("❌ Tidak ada catatan yang cocok dengan: '{}'", payload)
            } else {
                format!(
                    "🔍 Hasil pencarian untuk '{}' ({}/{} ditemukan):\n\n• {}",
                    payload,
                    hasil_filter.len(),
                    database_catatan.len(),
                    hasil_filter.join("\n• ")
                )
            }
        }

        // Aksi Tombol Cancel
        "btn_cancel" | "cancel_action" => {
            "Pembaruan sistem dibatalkan oleh pengguna.".to_string()
        }

        _ => format!("Aksi '{}' dijalankan dengan data: {}", action, payload),
    }
}
