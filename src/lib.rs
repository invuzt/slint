#![cfg(target_os = "android")]

slint::include_modules!();

#[no_mangle]
fn android_main(app: slint::android::AndroidApp) {
    slint::android::init(app).unwrap();
    
    let main_window = App::new().unwrap();
    let main_window_weak = main_window.as_weak();
    
    main_window.on_say_hello(move || {
        let main_window = main_window_weak.unwrap();
        let name = main_window.get_name_input();
        
        if name.is_empty() {
            main_window.set_message("Nama tidak boleh kosong!".into());
        } else {
            main_window.set_message(format!("Halo {}, Selamat datang di Slint!", name).into());
        }
    });
    
    main_window.run().unwrap();
}
