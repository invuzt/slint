pub fn handle_action(action: &str, payload: &str) -> String {
    match action {
        "sapa" => {
            if payload.trim().is_empty() {
                "Halo Orang Asing! Ketik namamu dulu.".to_string()
            } else {
                format!("Halo {}! Selamat datang di Permanent JNI 🚀", payload)
            }
        }
        "klik_kiri" => format!("Aksi Kiri Berhasil! Data: {}", payload),
        "klik_kanan" => format!("Aksi Kanan Berhasil! Data: {}", payload),
        "bersihkan_teks" => "Teks berhasil direset! Menunggu aksi berikutnya...".to_string(),
        _ => format!("Error: Aksi '{}' belum diimplementasikan di Rust!", action),
    }
}
