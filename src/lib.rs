#![cfg(target_os = "android")]

slint::include_modules!();

#[no_mangle]
fn android_main(app: slint::android::AndroidApp) {
    slint::android::init(app).unwrap();

    let main_window = CakruApp::new().unwrap();
    
    // DETEKSI TEMA NATIVE: Membaca konfigurasi sistem Android
    // 0x20 melambangkan UI_MODE_NIGHT_YES pada sistem Android SDK
    let config = app.config();
    let is_android_dark = (config.uiMode & 0x30) == 0x20;
    
    // Set tema aplikasi berdasarkan kondisi HP asli user saat dibuka
    main_window.set_is_dark(is_android_dark);

    let main_window_weak = main_window.as_weak();
    main_window.on_say_hello(move || {
        let main_window = main_window_weak.unwrap();
        let name = main_window.get_name_input();

        if name.is_empty() {
            main_window.set_message("Nama tidak boleh kosong!".into());
        } else {
            main_window.set_message(format!("Halo {}, Selamat datang di ekosistem CakRu!", name).into());
        }
    });

    main_window.run().unwrap();
}
