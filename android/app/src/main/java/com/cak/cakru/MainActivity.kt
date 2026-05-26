package com.cak.cakru

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Stack

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Konfigurasi Warna Tema MD3
        val filledButtonColor = Color.parseColor("#0067FF")
        val outlinedButtonColor = Color.parseColor("#0067FF")
        val buttonTextFilledColor = Color.parseColor("#FFFFFF")
        val inputBackgroundColor = Color.parseColor("#F3EDF7")
        val inputIndicatorColor = Color.parseColor("#49454F")
        val appBarBackgroundColor = Color.parseColor("#F3EDF7")
        val appBarTextColor = Color.parseColor("#1C1B1F")

        val buttonHeightDp = 56
        val buttonPaddingHorizontalDp = 24
        val inputHeightDp = 56
        val inputPaddingHorizontalDp = 16
        val appBarHeightDp = 64

        fun dpToPx(dp: Int): Int {
            return (dp * resources.displayMetrics.density).toInt()
        }

        // CONTAINER UTAMA LAYAR
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        // Tempat menampung Navbar jika nanti ditemukan di ui.cakru
        var navbarContainer: LinearLayout? = null

        // SCROLLVIEW GLOBAL UNTUK KONTEN UTAMA
        val globalScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isVerticalScrollBarEnabled = true
        }

        // KONTEN UTAMA LAYOUT (Di dalam ScrollView)
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(40))
            gravity = Gravity.START
            tag = "App"
        }
        globalScrollView.addView(contentLayout)

        val containerStack = Stack<LinearLayout>()
        containerStack.push(contentLayout)

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
        var isInsideNavbar = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (trimmed.endsWith("{")) {
                currentWidget = trimmed.split(" ")[0]
                idVal = ""; textVal = ""; hintVal = ""; actionVal = ""; sizeVal = ""; colorVal = ""

                when (currentWidget) {
                    "Navbar" -> {
                        isInsideNavbar = true
                        navbarContainer = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setBackgroundColor(appBarBackgroundColor)
                            setPadding(dpToPx(8), 0, dpToPx(8), 0)
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                dpToPx(appBarHeightDp)
                            )
                            tag = "Navbar"
                        }
                        // Langsung masukkan ke posisi paling atas layar (di luar scroll)
                        mainContainer.addView(navbarContainer)
                        containerStack.push(navbarContainer)
                    }
                    "Row" -> {
                        val parentContainer = containerStack.peek()
                        val rowLayout = LinearLayout(this).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                if (isInsideNavbar) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                if (!isInsideNavbar) setMargins(0, dpToPx(20), 0, dpToPx(10))
                            }
                            tag = "Row"
                        }
                        parentContainer.addView(rowLayout)
                        containerStack.push(rowLayout)
                    }
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

                if ((containerType == "Row" || containerType == "Navbar") && currentWidget == "") {
                    if (containerType == "Navbar") isInsideNavbar = false
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
                            
                            if (isInsideNavbar) {
                                // Jika teks berada di dalam navbar, otomatis jadi icon bergaya tombol murni
                                textSize = 22f
                                setTextColor(appBarTextColor)
                                gravity = Gravity.CENTER
                                setPadding(dpToPx(12), 0, dpToPx(12), 0)
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            } else {
                                // Teks biasa di area konten (subjudul dll)
                                textSize = if (sizeVal.isNotEmpty()) sizeVal.toFloat() else 18f
                                setTextColor(if (colorVal.isNotEmpty()) Color.parseColor(colorVal) else Color.parseColor("#1C1B1F"))
                                if (idVal == "sub_judul") {
                                    textSize = 20f
                                    setPadding(0, dpToPx(8), 0, dpToPx(16))
                                } else {
                                    setPadding(0, dpToPx(8), 0, dpToPx(8))
                                }
                                layoutParams = LinearLayout.LayoutParams(
                                    if (activeContainer.orientation == LinearLayout.HORIZONTAL) 0 else ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    if (activeContainer.orientation == LinearLayout.HORIZONTAL) 1f else 0f
                                )
                            }
                        }
                        activeContainer.addView(tv)
                        if (idVal.isNotEmpty()) viewMap[idVal] = tv
                    }
                    "Input" -> {
                        val et = EditText(this).apply {
                            hint = hintVal.ifEmpty { trimmed.substringAfter("placeholder: \"", "").substringBefore("\"", "") }
                            textSize = 16f
                            setTextColor(appBarTextColor)
                            setHintTextColor(Color.parseColor("#49454F"))
                            
                            if (isInsideNavbar) {
                                // Input khusus di dalam navbar (Search Box Tanpa Border Bawaan)
                                setBackgroundColor(Color.TRANSPARENT)
                                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                                setPadding(dpToPx(8), 0, dpToPx(8), 0)
                                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                            } else {
                                // Input biasa MD3 di dalam area Form konten bawah
                                val padSide = dpToPx(inputPaddingHorizontalDp)
                                setPadding(padSide, 0, padSide, 0)
                                gravity = Gravity.CENTER_VERTICAL
                                background = GradientDrawable().apply {
                                    shape = GradientDrawable.RECTANGLE
                                    cornerRadii = floatArrayOf(12f, 12f, 12f, 12f, 0f, 0f, 0f, 0f)
                                    setColor(inputBackgroundColor)
                                    setStroke(dpToPx(1), inputIndicatorColor)
                                }
                                layoutParams = LinearLayout.LayoutParams(
                                    if (activeContainer.orientation == LinearLayout.HORIZONTAL) 0 else ViewGroup.LayoutParams.MATCH_PARENT,
                                    dpToPx(inputHeightDp),
                                    if (activeContainer.orientation == LinearLayout.HORIZONTAL) 1f else 0f
                                ).apply {
                                    if (!isInsideNavbar) setMargins(0, dpToPx(5), 0, dpToPx(5))
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
                            isAllCaps = false
                            textSize = 16f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            maxLines = 1
                            ellipsize = TextUtils.TruncateAt.END

                            val paddingHorizontal = dpToPx(buttonPaddingHorizontalDp)
                            setPadding(paddingHorizontal, 0, paddingHorizontal, 0)

                            if (textVal.equals("Cancel", ignoreCase = true) || idVal == "btn_cancel") {
                                setTextColor(outlinedButtonColor)
                                background = GradientDrawable().apply {
                                    shape = GradientDrawable.RECTANGLE
                                    cornerRadius = dpToPx(buttonHeightDp / 2).toFloat()
                                    setColor(Color.TRANSPARENT)
                                    setStroke(dpToPx(2), outlinedButtonColor)
                                }
                            } else {
                                setTextColor(buttonTextFilledColor)
                                background = GradientDrawable().apply {
                                    shape = GradientDrawable.RECTANGLE
                                    cornerRadius = dpToPx(buttonHeightDp / 2).toFloat()
                                    setColor(filledButtonColor)
                                }
                            }
                            
                            layoutParams = LinearLayout.LayoutParams(
                                if (activeContainer.orientation == LinearLayout.HORIZONTAL) 0 else ViewGroup.LayoutParams.MATCH_PARENT,
                                dpToPx(buttonHeightDp),
                                if (activeContainer.orientation == LinearLayout.HORIZONTAL) 1f else 0f
                            ).apply {
                                if (activeContainer.orientation == LinearLayout.HORIZONTAL) {
                                    setMargins(dpToPx(8), 0, dpToPx(8), 0)
                                } else {
                                    setMargins(0, dpToPx(10), 0, dpToPx(10))
                                }
                            }
                        }
                        activeContainer.addView(btn)
                        if (actionVal.isNotEmpty() || idVal.isNotEmpty()) {
                            val act = if (actionVal.isNotEmpty()) actionVal else idVal
                            buttonActions.add(Triple(btn, act, lastActiveInput))
                        }
                    }
                }
                currentWidget = ""
            }
        }

        // Tempelkan ScrollView Konten di bawah tempat Navbar berada
        mainContainer.addView(globalScrollView)
        setContentView(mainContainer)

        // Aksi Tombol
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
    }
}
