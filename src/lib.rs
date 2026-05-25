mod parser;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

const UI_LAYOUT: &str = include_str!("../ui.cakru");

#[no_mangle]
pub unsafe extern "system" fn Java_com_cak_cakru_RustJni_getUiLayout(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let parsed_ui = parser::parse_ui(UI_LAYOUT);
    let output = env.new_string(parsed_ui).unwrap();
    output.into_raw()
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_cak_cakru_RustJni_onButtonClick(
    mut env: JNIEnv,
    _class: JClass,
    action: JString,
    input_text: JString,
) -> jstring {
    let action_str: String = env.get_string(&action).unwrap().into();
    let input_str: String = env.get_string(&input_text).unwrap().into();

    let mut respon = String::new();

    // Logika pencocokan 3 tombol sekaligus
    if action_str == "sapa" {
        if input_str.trim().is_empty() {
            respon = "Halo Orang Asing! Ketik namamu dong di atas.".to_string();
        } else {
            respon = format!("Halo {}! Selamat datang di ekosistem .cakru 🚀", input_str);
        }
    } else if action_str == "klik_kiri" {
        respon = format!("Kamu menekan Tombol KIRI! Input: {}", input_str);
    } else if action_str == "klik_kanan" {
        respon = format!("Kamu menekan Tombol KANAN! Input: {}", input_str);
    } else {
        respon = "Aksi tidak dikenali".to_string();
    }

    let output = env.new_string(respon).unwrap();
    output.into_raw()
}
