package com.cak.cakru

object RustJni {
    init {
        System.loadLibrary("cakru_app")
    }
    external fun getUiLayout(): String
    external fun Hub(action: String, payload: String): String
}
