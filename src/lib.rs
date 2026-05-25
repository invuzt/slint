mod parser;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

// Memuat file ui.cakru ke dalam binary saat compile-time
const UI_LAYOUT: &str = include_str!("../ui.cakru");

// JNI: Fungsi untuk mengirimkan isi ui.cakru yang sudah di-parse ke Kotlin
#[no_mangle]
pub unsafe extern "system" fn Java_com_cak_cakru_RustJni_getUiLayout(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let parsed_ui = parser::parse_ui(UI_LAYOUT);
    let output = env.new_string(parsed_ui).unwrap();
    output.into_raw()
}

// JNI: Fungsi untuk memproses aksi/logika tombol dari Kotlin
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

    // Jembatan Logika Berdasarkan properti 'action' di ui.cakru
    if action_str == "say_hello" {
        if input_str.trim().is_empty() {
            respon = "Nama tidak boleh kosong!".to_string();
        } else {
            respon = format!("Halo {}, Selamat datang di Cakru App!", input_str.trim());
        }
    }

    let output = env.new_string(respon).unwrap();
    output.into_raw()
}
