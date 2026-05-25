// ====================================================================
// SUB-MODUL LOGIKA (Setiap aksi punya rumahnya sendiri)
// ====================================================================

fn aksi_sapa(payload: &str) -> String {
    if payload.trim().is_empty() {
        "Halo Orang Asing! Ketik namamu dulu.".to_string()
    } else {
        format!("Halo {}! Selamat datang di Permanent JNI 🚀", payload)
    }
}

fn aksi_klik_kiri(payload: &str) -> String {
    format!("Aksi Kiri Berhasil! Data: {}", payload)
}

fn aksi_klik_kanan(payload: &str) -> String {
    format!("Aksi Kanan Berhasil! Data: {}", payload)
}

// ====================================================================
// CORE ROUTER (Tugasnya hanya mengarahkan aksi)
// ====================================================================
pub fn handle_action(action: &str, payload: &str) -> String {
    match action {
        "sapa" => aksi_sapa(payload),
        "klik_kiri" => aksi_klik_kiri(payload),
        "klik_kanan" => aksi_klik_kanan(payload),
        
        // Menggunakan Into karena string literal &str bisa diubah ke String secara efisien
        "bersihkan_teks" => "Teks berhasil direset! Menunggu aksi berikutnya...".into(),
        
        // Peringatan error jika aksi dari UI tidak terdaftar
        _ => format!("Error: Aksi '{}' belum diimplementasikan di Rust!", action),
    }
}
