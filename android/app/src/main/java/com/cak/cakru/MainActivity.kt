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

        // Penampung layout aktif (bisa rootLayout, bisa juga Row horizontal nanti)
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
                
                // JIKA MEMULAI SEBUAH ROW (Bagi layar kanan-kiri)
                if (currentWidget == "Row") {
                    val rowLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { weightSum = 2f } // Dibagi rata menjadi 2 bagian
                    }
                    rootLayout.addView(rowLayout)
                    currentContainer = rowLayout // Pindahkan fokus inject ke dalam Row
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
                            layoutParams = LinearLayout.LayoutParams(
                                0, // diatur 0 karena memakai weight
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                        }
                        currentContainer.addView(et)
                        currentInput = et
                    }
                    "Button" -> {
                        val btn = Button(this).apply {
                            text = textVal
                            layoutParams = LinearLayout.LayoutParams(
                                0, // diatur 0 karena memakai weight
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                1f // Mengambil porsi 1 dari total 2 bagian di Row
                            )
                            val capturedAction = actionVal
                            setOnClickListener {
                                val inputData = currentInput?.text?.toString() ?: ""
                                val resultFromRust = RustJni.onButtonClick(capturedAction, inputData)
                                outputTextView?.text = resultFromRust
                            }
                        }
                        currentContainer.addView(btn)
                    }
                    "Row" -> {
                        // Jika blok Row tutup, kembalikan fokus inject ke kontainer utama (Vertikal)
                        currentContainer = rootLayout
                    }
                }
                currentWidget = ""
            }
        }

        setContentView(rootLayout)
    }
}
