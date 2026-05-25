package com.cak.cakru

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Stack

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kontainer Utama Aplikasi (Vertikal)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(50, 50, 50, 50)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Gunakan Stack untuk melacak posisi kontainer aktif
        val containerStack = Stack<LinearLayout>()
        containerStack.push(rootLayout)

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
            
            if (trimmed.isEmpty()) continue

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
                    // Masukkan row baru ke dalam kontainer aktif saat ini
                    containerStack.peek().addView(rowLayout)
                    // Angkat rowLayout sebagai kontainer aktif yang baru
                    containerStack.push(rowLayout)
                }
                continue
            } 
            
            if (trimmed.startsWith("text:")) {
                textVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed.startsWith("placeholder:")) {
                hintVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed.startsWith("action:")) {
                actionVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed == "}") {
                // Jika yang ditutup adalah blok elemen layout seperti Row atau App
                if (currentWidget == "Row" || currentWidget == "App") {
                    if (containerStack.size > 1) {
                        containerStack.pop() // Turun satu tingkat ke kontainer induknya
                    }
                    currentWidget = ""
                    continue
                }

                // Ambil kontainer yang sedang aktif di puncak stack
                val activeContainer = containerStack.peek()

                when (currentWidget) {
                    "Text" -> {
                        val tv = TextView(this).apply {
                            text = textVal
                            textSize = 20f
                            setPadding(0, 20, 0, 20)
                        }
                        activeContainer.addView(tv)
                        outputTextView = tv
                    }
                    "Input" -> {
                        val et = EditText(this).apply {
                            hint = hintVal
                            // Jika aktif di dalam Row, bagi rata. Jika di luar, penuhi layar.
                            layoutParams = if (containerStack.size > 1 && activeContainer != rootLayout) {
                                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                            } else {
                                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                    setMargins(0, 20, 0, 20)
                                }
                            }
                        }
                        activeContainer.addView(et)
                        currentInput = et
                    }
                    "Button" -> {
                        val btn = Button(this).apply {
                            text = textVal
                            layoutParams = if (containerStack.size > 1 && activeContainer != rootLayout) {
                                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                            } else {
                                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            }
                            val capturedAction = actionVal
                            setOnClickListener {
                                val inputData = currentInput?.text?.toString() ?: ""
                                val resultFromRust = RustJni.onButtonClick(capturedAction, inputData)
                                outputTextView?.text = resultFromRust
                            }
                        }
                        activeContainer.addView(btn)
                    }
                }
                // Setelah memproses widget non-layout, tandai bahwa kita sedang berada di scope induknya
                currentWidget = if (containerStack.size > 1) "Row" else "App"
            }
        }

        setContentView(rootLayout)
    }
}
