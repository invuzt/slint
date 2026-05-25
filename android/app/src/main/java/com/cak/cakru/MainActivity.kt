package com.cak.cakru

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Buat Layout Utama (Vertical Layout) bawaan Android
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(50, 50, 50, 50)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Ambil data layout dari otak Rust
        val rawUiData = RustJni.getUiLayout()
        
        var currentInput: EditText? = null
        var outputTextView: TextView? = null

        // Parser Sederhana di Sisi Kotlin untuk merender Native Views
        val lines = rawUiData.split("\n")
        var currentWidget = ""
        
        // Nilai default/temporary untuk menampung data atribut .cakru
        var textVal = ""
        var hintVal = ""
        var actionVal = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.endsWith("{")) {
                currentWidget = trimmed.split(" ")[0]
                textVal = ""; hintVal = ""; actionVal = ""
            } else if (trimmed.startsWith("text:")) {
                textVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed.startsWith("placeholder:")) {
                hintVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed.startsWith("action:")) {
                actionVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed == "}") {
                // Ketika blok widget tutup, langsung inject ke layar Android
                when (currentWidget) {
                    "Text" -> {
                        val tv = TextView(this).apply {
                            text = textVal
                            textSize = 20f
                            setPadding(0, 20, 0, 20)
                        }
                        rootLayout.addView(tv)
                        // Simpan reference textview terakhir untuk wadah output pesan
                        outputTextView = tv
                    }
                    "Input" -> {
                        val et = EditText(this).apply {
                            hint = hintVal
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        }
                        rootLayout.addView(et)
                        currentInput = et
                    }
                    "Button" -> {
                        val btn = Button(this).apply {
                            text = textVal
                            val capturedAction = actionVal
                            setOnClickListener {
                                val inputData = currentInput?.text?.toString() ?: ""
                                // Panggil otak Rust saat di-klik!
                                val resultFromRust = RustJni.onButtonClick(capturedAction, inputData)
                                // Tampilkan hasilnya ke TextView output
                                outputTextView?.text = resultFromRust
                            }
                        }
                        rootLayout.addView(btn)
                    }
                }
                currentWidget = ""
            }
        }

        setContentView(rootLayout)
    }
}
