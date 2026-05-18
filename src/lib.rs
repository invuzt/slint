#![cfg(target_os = "android")]
slint::include_modules!();

use android_activity::AndroidApp;

#[no_mangle]
fn android_main(app: AndroidApp) {
    android_logger::init_once(
        android_logger::Config::default().with_max_level(log::LevelFilter::Info)
    );
    
    // Initialize Slint platform for Android
    slint::platform::set_platform(Box::new(slint::platform::android::AndroidPlatform::new(app)))
        .expect("Failed to initialize Slint platform");
    
    let app = App::new().unwrap();
    let app_weak = app.as_weak();
    
    app.on_say_hello(move || {
        let app = app_weak.unwrap();
        let name = app.get_name_input();
        
        if name.is_empty() {
            app.set_message("Nama tidak boleh kosong!".into());
        } else {
            app.set_message(format!("Halo {}, Selamat datang di Slint!", name).into());
        }
    });
    
    app.run().unwrap();
}
