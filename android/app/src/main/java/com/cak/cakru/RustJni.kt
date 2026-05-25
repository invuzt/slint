package com.cak.cakru

object RustJni {
    init {
        // Memuat file .so hasil compile Rust
        System.loadLibrary("cakru_app")
    }

    // Mengambil layout dari Rust
    external fun getUiLayout(): String

    // Mengirim action klik dan input teks ke Rust, menerima response string
    external fun onButtonClick(action: String, inputText: String): String
}
