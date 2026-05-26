package com.cak.cakru

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import java.util.Stack

class MainActivity : AppCompatActivity() {

    // Simpan daftar master gudang secara dinamis di level class
    private var dynamicGudangList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tarik data awal gudang dari Rust untuk pertama kali
        val initialGudangRaw = RustJni.Hub("nav_setting", "")
        updateGudangListFromPayload(initialGudangRaw)

        val filledButtonColor = Color.parseColor("#0067FF")
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

        val drawerLayout = DrawerLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        val sideMenuContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            setPadding(dpToPx(24), dpToPx(48), dpToPx(24), dpToPx(24))
            layoutParams = DrawerLayout.LayoutParams(dpToPx(280), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.START
            }
        }

        val menuTitle = TextView(this).apply {
            text = "ArZip Panel"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1C1B1F"))
            setPadding(0, 0, 0, dpToPx(24))
        }
        sideMenuContainer.addView(menuTitle)

        var navbarContainer: LinearLayout? = null
        val globalScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            isVerticalScrollBarEnabled = true
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(40))
            gravity = Gravity.START
            tag = "App"
        }
        globalScrollView.addView(contentLayout)

        val containerStack = Stack<LinearLayout>()
        containerStack.push(contentLayout)

        val rawUiData = RustJni.getUiLayout()
        val viewMap = HashMap<String, android.view.View>()
        val viewActions = ArrayList<Triple<android.view.View, String, Boolean>>()

        val lines = rawUiData.split("\n")
        var currentWidget = ""
        var idVal = ""
        var textVal = ""
        var hintVal = ""
        var actionVal = ""
        var sizeVal = ""
        var colorVal = ""

        var isInsideNavbar = false
        val outValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        val rippleResId = outValue.resourceId

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
                            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(appBarHeightDp))
                            tag = "Navbar"
                        }
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
                                if (!isInsideNavbar) setMargins(0, dpToPx(12), 0, dpToPx(12))
                            }
                            tag = "Row"
                        }
                        parentContainer.addView(rowLayout)
                        containerStack.push(rowLayout)
                    }
                }
                continue
            }

            if (trimmed.startsWith("id:")) idVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            else if (trimmed.startsWith("text:")) textVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            else if (trimmed.startsWith("placeholder:")) hintVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            else if (trimmed.startsWith("action:")) actionVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            else if (trimmed.startsWith("size:")) sizeVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            else if (trimmed.startsWith("color:")) colorVal = trimmed.substringAfter("\"").substringBeforeLast("\"")
            else if (trimmed == "}") {
                val activeContainer = containerStack.peek()
                val containerType = activeContainer.tag as? String

                if ((containerType == "Row" || containerType == "Navbar") && currentWidget == "") {
                    if (containerType == "Navbar") isInsideNavbar = false
                    if (containerStack.size > 1) containerStack.pop()
                    currentWidget = ""
                    continue
                }

                when (currentWidget) {
                    "Text" -> {
                        val tv = TextView(this).apply {
                            text = textVal
                            if (isInsideNavbar) {
                                textSize = 22f
                                setTextColor(appBarTextColor)
                                gravity = Gravity.CENTER
                                setPadding(dpToPx(12), 0, dpToPx(12), 0)
                                setBackgroundResource(rippleResId)
                                isClickable = true
                                isFocusable = true
                                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            } else {
                                textSize = if (sizeVal.isNotEmpty()) sizeVal.toFloat() else 16f
                                setTextColor(if (colorVal.isNotEmpty()) Color.parseColor(colorVal) else Color.parseColor("#1C1B1F"))
                                setPadding(0, dpToPx(6), 0, dpToPx(6))
                                layoutParams = LinearLayout.LayoutParams(
                                    if (activeContainer.orientation == LinearLayout.HORIZONTAL) 0 else ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    if (activeContainer.orientation == LinearLayout.HORIZONTAL) 1f else 0f
                                )
                            }
                        }
                        activeContainer.addView(tv)
                        if (idVal.isNotEmpty()) viewMap[idVal] = tv
                        if (isInsideNavbar && (actionVal.isNotEmpty() || idVal.isNotEmpty())) {
                            viewActions.add(Triple(tv, if (actionVal.isNotEmpty()) actionVal else idVal, false))
                        }
                    }
                    "Input" -> {
                        val et = EditText(this).apply {
                            hint = hintVal
                            textSize = 15f
                            setTextColor(appBarTextColor)
                            setHintTextColor(Color.parseColor("#79747E"))
                            if (isInsideNavbar) {
                                setBackgroundColor(Color.TRANSPARENT)
                                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                                setPadding(dpToPx(8), 0, dpToPx(8), 0)
                                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                                
                                val searchActionId = if (idVal.isNotEmpty()) idVal else "nav_search"
                                addTextChangedListener(object : TextWatcher {
                                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                        val res = RustJni.Hub(searchActionId, s?.toString() ?: "")
                                        (viewMap["output_pesan"] as? TextView)?.text = res
                                    }
                                    override fun afterTextChanged(s: Editable?) {}
                                })
                            } else {
                                val padSide = dpToPx(inputPaddingHorizontalDp)
                                setPadding(padSide, 0, padSide, 0)
                                gravity = Gravity.CENTER_VERTICAL
                                background = GradientDrawable().apply {
                                    shape = GradientDrawable.RECTANGLE
                                    cornerRadius = dpToPx(8).toFloat()
                                    setColor(inputBackgroundColor)
                                    setStroke(dpToPx(1), inputIndicatorColor)
                                }
                                layoutParams = LinearLayout.LayoutParams(
                                    if (activeContainer.orientation == LinearLayout.HORIZONTAL) 0 else ViewGroup.LayoutParams.MATCH_PARENT,
                                    dpToPx(inputHeightDp),
                                    if (activeContainer.orientation == LinearLayout.HORIZONTAL) 1f else 0f
                                ).apply { setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4)) }

                                if (idVal == "input_gudang") {
                                    isFocusable = false
                                    isClickable = true
                                    setOnClickListener {
                                        if (dynamicGudangList.isEmpty()) {
                                            setText("")
                                            val outTarget = viewMap["output_pesan"] as? TextView
                                            outTarget?.text = "⚠️ Daftar gudang kosong. Tambahkan dulu lewat tombol ⚙️ di atas."
                                            return@setOnClickListener
                                        }
                                        val arr = dynamicGudangList.toTypedArray()
                                        AlertDialog.Builder(this@MainActivity)
                                            .setTitle("Pilih Gudang")
                                            .setItems(arr) { _, index -> setText(arr[index]) }
                                            .show()
                                    }
                                } else if (idVal == "input_tahun") {
                                    isFocusable = false
                                    isClickable = true
                                    setOnClickListener {
                                        val listTahun = arrayOf("2026", "2025", "2024", "1990", "1989", "1988")
                                        AlertDialog.Builder(this@MainActivity)
                                            .setTitle("Pilih Tahun Warkah")
                                            .setItems(listTahun) { _, index -> setText(listTahun[index]) }
                                            .show()
                                    }
                                } else if (idVal == "input_range") {
                                    inputType = InputType.TYPE_CLASS_PHONE
                                }
                            }
                        }
                        activeContainer.addView(et)
                        if (idVal.isNotEmpty()) viewMap[idVal] = et
                    }
                    "Button" -> {
                        val btn = Button(this).apply {
                            text = textVal
                            isAllCaps = false
                            textSize = 16f
                            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                            val paddingHorizontal = dpToPx(buttonPaddingHorizontalDp)
                            setPadding(paddingHorizontal, 0, paddingHorizontal, 0)

                            setTextColor(buttonTextFilledColor)
                            background = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                cornerRadius = dpToPx(buttonHeightDp / 2).toFloat()
                                setColor(filledButtonColor)
                            }
                            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(buttonHeightDp)).apply {
                                setMargins(0, dpToPx(8), 0, dpToPx(8))
                            }
                        }
                        activeContainer.addView(btn)
                        if (actionVal.isNotEmpty() || idVal.isNotEmpty()) {
                            viewActions.add(Triple(btn, if (actionVal.isNotEmpty()) actionVal else idVal, true))
                        }
                    }
                }
                currentWidget = ""
            }
        }

        mainContainer.addView(globalScrollView)
        drawerLayout.addView(mainContainer)
        drawerLayout.addView(sideMenuContainer)
        setContentView(drawerLayout)

        for (triple in viewActions) {
            val view = triple.first
            val action = triple.second
            val isFormSubmit = triple.third

            view.setOnClickListener {
                if (action == "nav_setting") {
                    // DIALOG PENGATURAN GUDANG DINAMIS
                    openGudangManagementDialog()
                    return@setOnClickListener
                }

                val inputGudangEt = viewMap["input_gudang"] as? EditText
                val inputKardusEt = viewMap["input_kardus"] as? EditText
                val inputRangeEt = viewMap["input_range"] as? EditText
                val inputTahunEt = viewMap["input_tahun"] as? EditText

                var rangeRaw = inputRangeEt?.text?.toString() ?: ""
                if (rangeRaw.contains(" ")) {
                    rangeRaw = rangeRaw.trim().replace("\\s+".toRegex(), "-")
                }

                val payloadData = if (isFormSubmit) {
                    val gud = inputGudangEt?.text?.toString() ?: ""
                    val lok = inputKardusEt?.text?.toString() ?: ""
                    val thn = inputTahunEt?.text?.toString() ?: ""
                    "$gud|$lok|$rangeRaw|$thn"
                } else ""

                val resultFromRust = RustJni.Hub(action, payloadData)
                
                if (resultFromRust == "OPEN_DRAWER") {
                    drawerLayout.openDrawer(Gravity.START)
                } else {
                    (viewMap["output_pesan"] as? TextView)?.text = resultFromRust
                    if (isFormSubmit && resultFromRust.startsWith("✅")) {
                        inputRangeEt?.setText("")
                        inputRangeEt?.requestFocus()
                    }
                }
            }
        }
    }

    // Fungsi pemecah payload dari Rust ke Array List Kotlin
    private fun updateGudangListFromPayload(payload: String) {
        dynamicGudangList.clear()
        if (payload != "EMPTY" && payload.isNotEmpty()) {
            val items = payload.split("|")
            for (item in items) {
                if (item.isNotEmpty()) dynamicGudangList.add(item)
            }
        }
    }

    // Modal Manager Dialog khusus Master Gudang
    private fun openGudangManagementDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("⚙️ Kelola Master Gudang")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val description = TextView(this).apply {
            text = "Daftar Gudang Aktif saat ini (Tap untuk hapus):"
            setPadding(0, 0, 0, 10)
        }
        layout.addView(description)

        // Tampilkan daftar gudang yang bisa di-tap untuk hapus
        for (gudangName in dynamicGudangList) {
            val tv = TextView(this).apply {
                text = "❌ $gudangName"
                textSize = 16f
                setPadding(10, 15, 10, 15)
                isClickable = true
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("Hapus '$gudangName' dari pilihan?")
                        .setPositiveButton("Ya") { _, _ ->
                            val res = RustJni.Hub("hapus_master_gudang", gudangName)
                            updateGudangListFromPayload(res)
                            // Refresh dialog
                            it.visibility = ViewGroup.GONE
                        }
                        .setNegativeButton("Batal", null)
                        .show()
                }
            }
            layout.addView(tv)
        }

        val inputBaru = EditText(this).apply {
            hint = "Nama Gudang Baru..."
            setSingleLine()
        }
        layout.addView(inputBaru)

        builder.setView(layout)
        builder.setPositiveButton("Tambah") { _, _ ->
            val namaBaru = inputBaru.text.toString()
            if (namaBaru.isNotEmpty()) {
                val res = RustJni.Hub("tambah_master_gudang", namaBaru)
                updateGudangListFromPayload(res)
            }
        }
        builder.setNegativeButton("Tutup", null)
        builder.show()
    }
}
