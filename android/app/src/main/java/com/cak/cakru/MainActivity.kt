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

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(50, 50, 50, 50)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val containerStack = Stack<LinearLayout>()
        containerStack.push(rootLayout)

        val rawUiData = RustJni.getUiLayout()
        
        // Simpan semua referensi view berdasarkan ID-nya agar tidak tertukar
        val viewMap = HashMap<String, android.view.View>()
        // Daftarkan juga tombol yang butuh dipasangi aksi setelah loop selesai
        val buttonActions = ArrayList<Pair<Button, String>>()

        val lines = rawUiData.split("\n")
        var currentWidget = ""
        var idVal = ""
        var textVal = ""
        var hintVal = ""
        var actionVal = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.endsWith("{")) {
                currentWidget = trimmed.split(" ")[0]
                idVal = ""; textVal = ""; hintVal = ""; actionVal = ""
                
                if (currentWidget == "Row") {
                    val rowLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 20, 0, 20) }
                    }
                    containerStack.peek().addView(rowLayout)
                    containerStack.push(rowLayout)
                }
                continue
            } 
            
            if (trimmed.startsWith("id:")) {
                idVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed.startsWith("text:")) {
                textVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed.startsWith("placeholder:")) {
                hintVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed.startsWith("action:")) {
                actionVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed == "}") {
                val activeContainer = containerStack.peek()

                if (currentWidget == "Row" || currentWidget == "App") {
                    if (containerStack.size > 1) containerStack.pop()
                    currentWidget = ""
                    continue
                }

                when (currentWidget) {
                    "Text" -> {
                        val tv = TextView(this).apply {
                            text = textVal
                            textSize = 20f
                            setPadding(0, 20, 0, 20)
                        }
                        activeContainer.addView(tv)
                        if (idVal.isNotEmpty()) viewMap[idVal] = tv
                    }
                    "Input" -> {
                        val et = EditText(this).apply {
                            hint = hintVal
                            layoutParams = if (containerStack.size > 1 && activeContainer != rootLayout) {
                                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                            } else {
                                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                    setMargins(0, 20, 0, 20)
                                }
                            }
                        }
                        activeContainer.addView(et)
                        if (idVal.isNotEmpty()) viewMap[idVal] = et
                    }
                    "Button" -> {
                        val btn = Button(this).apply {
                            text = textVal
                            layoutParams = if (containerStack.size > 1 && activeContainer != rootLayout) {
                                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                            } else {
                                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            }
                        }
                        activeContainer.addView(btn)
                        // Simpan dulu button dan nama aksinya ke list
                        if (actionVal.isNotEmpty()) {
                            buttonActions.add(Pair(btn, actionVal))
                        }
                    }
                }
                currentWidget = if (containerStack.size > 1) "Row" else "App"
            }
        }

        // SETELAH LAYOUT SELESAI DIBUAT: Pasang fungsi klik dengan target ID yang presisi
        for (pair in buttonActions) {
            val btn = pair.first
            val action = pair.second
            
            btn.setOnClickListener {
                // Ambil data dari input_nama secara spesifik lewat Map ID
                val inputTarget = viewMap["input_nama"] as? EditText
                val inputData = inputTarget?.text?.toString() ?: ""
                
                // Kirim ke Rust Hub
                val resultFromRust = RustJni.Hub(action, inputData)
                
                // Tembakkan hasilnya khusus ke output_pesan lewat Map ID
                val outputTarget = viewMap["output_pesan"] as? TextView
                outputTarget?.text = resultFromRust
            }
        }

        setContentView(rootLayout)
    }
}
