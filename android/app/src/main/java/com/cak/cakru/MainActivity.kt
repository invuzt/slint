package com.cak.cakru

import android.graphics.Color
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
        val viewMap = HashMap<String, android.view.View>()
        val buttonActions = ArrayList<Triple<Button, String, EditText?>>()

        val lines = rawUiData.split("\n")
        var currentWidget = ""
        var idVal = ""
        var textVal = ""
        var hintVal = ""
        var actionVal = ""
        var sizeVal = ""
        var colorVal = ""
        
        var lastActiveInput: EditText? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.endsWith("{")) {
                currentWidget = trimmed.split(" ")[0]
                idVal = ""; textVal = ""; hintVal = ""; actionVal = ""; sizeVal = ""; colorVal = ""

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
            } else if (trimmed.startsWith("size:")) {
                sizeVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            } else if (trimmed.startsWith("color:")) {
                colorVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
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
                            textSize = if (sizeVal.isNotEmpty()) sizeVal.toFloat() else 18f
                            // PERBAIKAN DI SINI: Menggunakan setTextColor() yang valid
                            if (colorVal.isNotEmpty()) {
                                try { setTextColor(Color.parseColor(colorVal)) } catch (e: Exception) {}
                            }
                            setPadding(0, 15, 0, 15)
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
                        lastActiveInput = et
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
                        if (actionVal.isNotEmpty()) {
                            buttonActions.add(Triple(btn, actionVal, lastActiveInput))
                        }
                    }
                }
                currentWidget = if (containerStack.size > 1) "Row" else "App"
            }
        }

        for (triple in buttonActions) {
            val btn = triple.first
            val action = triple.second
            val linkedInput = triple.third

            btn.setOnClickListener {
                val inputData = linkedInput?.text?.toString() ?: ""
                val resultFromRust = RustJni.Hub(action, inputData)
                val outputTarget = viewMap["output_pesan"] as? TextView
                outputTarget?.text = resultFromRust
            }
        }

        setContentView(rootLayout)
    }
}
