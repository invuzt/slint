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

        // Kontainer Utama (Vertikal - dari atas ke bawah)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(50, 50, 50, 50)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Penampung layout aktif (bisa rootLayout, bisa juga Row horizontal)
        var currentContainer: LinearLayout = rootLayout

        val rawUiData = RustJni.getUiLayout()
        var currentInput: EditText? = null
        var outputTextView: TextView? = null

        val lines = rawUiData.split("\n")
        var currentWidget = ""
        var textVal = ""
        var hintVal = ""
        var actionVal = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.endsWith("{")) {
                currentWidget = trimmed.split(" ")[0]
                textVal = ""; hintVal = ""; actionVal = ""
                
                if (currentWidget == "Row") {
                    val rowLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { 
                            setMargins(0, 20, 0, 20)
                        }
                    }
                    rootLayout.addView(rowLayout)
                    currentContainer = rowLayout // Pindahkan fokus ke dalam Row
                }
            } else if (trimmed.startsWith("text:")) {
                textVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed.startsWith("placeholder:")) {
                hintVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed.startsWith("action:")) {
                actionVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed == "}") {
                when (currentWidget) {
                    "Text" -> {
                        val tv = TextView(this).apply {
                            text = textVal
                            textSize = 20f
                            setPadding(0, 20, 0, 20)
                        }
                        currentContainer.addView(tv)
                        outputTextView = tv
                    }
                    "Input" -> {
                        val et = EditText(this).apply {
                            hint = hintVal
                            // PERBAIKAN: Jika di dalam Row pakai weight, jika di luar pakai MATCH_PARENT
                            layoutParams = if (currentContainer != rootLayout) {
                                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                            } else {
                                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                    setMargins(0, 20, 0, 20)
                                }
                            }
                        }
                        currentContainer.addView(et)
                        currentInput = et // Ambil referensi input aktif
                    }
                    "Button" -> {
                        val btn = Button(this).apply {
                            text = textVal
                            // PERBAIKAN: Jika di dalam Row bagi rata (weight 1f), jika di luar pakai MATCH_PARENT
                            layoutParams = if (currentContainer != rootLayout) {
                                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                            } else {
                                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            }
                            val capturedAction = actionVal
                            setOnClickListener {
                                // Ambil teks aktual saat tombol diklik
                                val inputData = currentInput?.text?.toString() ?: ""
                                val resultFromRust = RustJni.onButtonClick(capturedAction, inputData)
                                outputTextView?.text = resultFromRust
                            }
                        }
                        currentContainer.addView(btn)
                    }
                    "Row" -> {
                        // Kembali ke kontainer utama (Vertikal) setelah Row selesai
                        currentContainer = rootLayout
                    }
                }
                currentWidget = ""
            }
        }

        setContentView(rootLayout)
    }
}
