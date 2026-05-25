mod parser;

// Trik ajaib mengarahkan modul ke file app_logic.rs yang ada di luar folder src
#[path = "../app_logic.rs"]
mod app_logic;

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
pub unsafe extern "system" fn Java_com_cak_cakru_RustJni_Hub(
    mut env: JNIEnv,
    _class: JClass,
    action: JString,
    payload: JString,
) -> jstring {
    let action_str: String = env.get_string(&action).unwrap().into();
    let payload_str: String = env.get_string(&payload).unwrap().into();

    // Lempar ke app_logic yang lokasinya sudah kita mapping di atas
    let hasil_respon = app_logic::handle_action(&action_str, &payload_str);

    let output = env.new_string(hasil_respon).unwrap();
    output.into_raw()
}
