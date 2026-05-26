package com.cak.cakru

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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

        // Konfigurasi Tema MD3 Global
        val primaryButtonColor = Color.parseColor("#6750A4")
        val buttonTextColor = Color.parseColor("#FFFFFF")
        val inputBackgroundColor = Color.parseColor("#F3EDF7")
        val inputIndicatorColor = Color.parseColor("#49454F")
        
        // Warna latar belakang Top App Bar MD3 (Surface Container)
        val appBarBackgroundColor = Color.parseColor("#F3EDF7")
        val appBarTextColor = Color.parseColor("#1D1B20")

        val buttonHeightDp = 56
        val buttonPaddingHorizontalDp = 24
        val inputHeightDp = 56
        val inputPaddingHorizontalDp = 16
        val appBarHeightDp = 64

        // Root layout diubah padding atasnya menjadi 0 agar App Bar menempel sempurna di atap layar
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(0, 0, 0, 50)
            gravity = Gravity.CENTER_HORIZONTAL
            tag = "App"
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

        fun dpToPx(dp: Int): Int {
            return (dp * resources.displayMetrics.density).toInt()
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (trimmed.endsWith("{")) {
                currentWidget = trimmed.split(" ")[0]
                idVal = ""; textVal = ""; hintVal = ""; actionVal = ""; sizeVal = ""; colorVal = ""

                if (currentWidget == "Row") {
                    val rowLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        weightSum = 2f
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { 
                            // Beri margin samping agar tidak menempel ke dinding layar
                            setMargins(dpToPx(16), dpToPx(15), dpToPx(16), dpToPx(15)) 
                        }
                        tag = "Row"
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
                val containerType = activeContainer.tag as? String

                if (containerType == "Row" && currentWidget == "") {
                    if (containerStack.size > 1) containerStack.pop()
                    currentWidget = ""
                    continue
                } else if (containerType == "App" && currentWidget == "") {
                    currentWidget = ""
                    continue
                }

                when (currentWidget) {
                    "Text" -> {
                        val tv = TextView(this).apply {
                            text = textVal
                            
                            // Jika ID adalah 'judul', otomatis bertransformasi menjadi MD3 Small Top App Bar
                            if (idVal == "judul") {
                                textSize = 22f // Title Large MD3 Standar
                                setTextColor(appBarTextColor)
                                setBackgroundColor(appBarBackgroundColor)
                                // Jarak teks dari kiri (akomodasi ketiadaan ikon kiri = 16dp)
                                setPadding(dpToPx(16), 0, dpToPx(16), 0)
                                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                                
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, 
                                    dpToPx(appBarHeightDp)
                                ).apply {
                                    setMargins(0, 0, 0, dpToPx(15)) // Beri jeda ke bawah
                                }
                            } else {
                                // Gaya komponen Text biasa di bawah App Bar
                                textSize = if (sizeVal.isNotEmpty()) sizeVal.toFloat() else 18f
                                setTextColor(if (colorVal.isNotEmpty()) Color.parseColor(colorVal) else Color.parseColor("#4A4A4A"))
                                setPadding(0, 15, 0, 15)
                                
                                layoutParams = if (activeContainer.orientation == LinearLayout.HORIZONTAL) {
                                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                                } else {
                                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                        setMargins(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
                                    }
                                }
                            }
                        }
                        activeContainer.addView(tv)
                        if (idVal.isNotEmpty()) viewMap[idVal] = tv
                    }
                    "Input" -> {
                        val et = EditText(this).apply {
                            hint = hintVal
                            textSize = 16f
                            setHintTextColor(Color.parseColor("#79747E"))
                            setTextColor(Color.parseColor("#1C1B1F"))
                            
                            val padSide = dpToPx(inputPaddingHorizontalDp)
                            setPadding(padSide, 0, padSide, 0)
                            gravity = Gravity.CENTER_VERTICAL

                            val shape = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                cornerRadii = floatArrayOf(12f, 12f, 12f, 12f, 0f, 0f, 0f, 0f)
                                setColor(inputBackgroundColor)
                                setStroke(dpToPx(1), inputIndicatorColor)
                            }
                            background = shape

                            layoutParams = if (activeContainer.orientation == LinearLayout.HORIZONTAL) {
                                LinearLayout.LayoutParams(0, dpToPx(inputHeightDp), 1f).apply {
                                    setMargins(10, 0, 10, 0)
                                }
                            } else {
                                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(inputHeightDp)).apply {
                                    setMargins(dpToPx(16), dpToPx(15), dpToPx(16), dpToPx(15))
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
                            setTextColor(buttonTextColor)
                            isAllCaps = false
                            textSize = 16f
                            
                            val paddingHorizontal = dpToPx(buttonPaddingHorizontalDp)
                            setPadding(paddingHorizontal, 0, paddingHorizontal, 0)

                            val shape = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                cornerRadius = dpToPx(buttonHeightDp / 2).toFloat()
                                setColor(primaryButtonColor)
                            }
                            background = shape
                            
                            layoutParams = if (activeContainer.orientation == LinearLayout.HORIZONTAL) {
                                LinearLayout.LayoutParams(0, dpToPx(buttonHeightDp), 1f).apply {
                                    setMargins(10, 0, 10, 0)
                                }
                            } else {
                                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(buttonHeightDp)).apply {
                                    setMargins(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
                                }
                            }
                        }
                        activeContainer.addView(btn)
                        if (actionVal.isNotEmpty()) {
                            buttonActions.add(Triple(btn, actionVal, lastActiveInput))
                        }
                    }
                }
                currentWidget = ""
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
