package com.example.ipcleaner

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("ipcleaner", MODE_PRIVATE) }
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var ipText: TextView
    private lateinit var statusText: TextView
    private var combinedPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        refreshIp()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 24)
            setBackgroundColor(0xFFF3F6FB.toInt())
        }
        panel.addView(TextView(this).apply {
            text = "IP CLEANER"; textSize = 27f; gravity = Gravity.CENTER
            setTextColor(0xFF14243A.toInt()); setPadding(0, 16, 0, 4)
        })
        panel.addView(TextView(this).apply {
            text = "Xiaomi Redmi K60 Pro • MIUI 14"; gravity = Gravity.CENTER
            textSize = 13f; setTextColor(0xFF64748B.toInt()); setPadding(0, 0, 0, 22)
        })
        ipText = TextView(this).apply {
            text = "IP công khai: đang kiểm tra..."; textSize = 18f
            setTextColor(0xFF14243A.toInt()); setPadding(18, 22, 18, 22)
            setBackgroundColor(-1)
        }
        statusText = TextView(this).apply {
            text = "Sẵn sàng"; textSize = 13f
            setTextColor(0xFF475569.toInt()); setPadding(2, 12, 2, 12)
        }
        panel.addView(ipText)
        panel.addView(statusText)
        panel.addView(actionButton("1. RESET IP DI ĐỘNG", 0xFF2563EB.toInt()) { openMobileSettings() })
        panel.addView(actionButton("2. XÓA SẠCH DỮ LIỆU CHROME", 0xFF15803D.toInt()) { openChromeSettings() })
        panel.addView(actionButton("3. RESET IP + XÓA CHROME", 0xFFB45309.toInt()) { combinedAction() })
        panel.addView(actionButton("4. CẬP NHẬT IP HIỆN TẠI", 0xFF475569.toInt()) { refreshIp() })
        panel.addView(actionButton("XEM LỊCH SỬ IP / THAO TÁC", 0xFF64748B.toInt()) { showHistory() })
        panel.addView(TextView(this).apply {
            text = "Lịch sử được lưu cục bộ trên điện thoại."; textSize = 12f
            setTextColor(0xFF334155.toInt()); setPadding(12, 12, 12, 12)
        })
        panel.addView(TextView(this).apply {
            text = "Giới hạn Android: ứng dụng không thể tự bật/tắt dữ liệu di động hoặc âm thầm xóa dữ liệu riêng của Chrome khi không có quyền hệ thống/Root. Bạn cần xác nhận trong Cài đặt. Nhà mạng cũng có thể cấp lại cùng địa chỉ IP."
            textSize = 12f; setTextColor(0xFF92400E.toInt()); setPadding(2, 18, 2, 8)
        })
        scroll.addView(panel)
        setContentView(scroll)
    }

    private fun actionButton(label: String, color: Int, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 13f; setTextColor(-1)
        setBackgroundColor(color); setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 7, 0, 7) }
    }

    private fun openMobileSettings() {
        log("Mở cài đặt mạng di động")
        statusText.text = "Tắt dữ liệu di động, chờ 10–15 giây rồi bật lại."
        try { startActivity(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)) }
        catch (_: Exception) { startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
    }

    private fun openChromeSettings() {
        log("Mở cài đặt ứng dụng Chrome")
        statusText.text = "Chọn Lưu trữ → Xóa dữ liệu → Xóa tất cả dữ liệu."
        try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.android.chrome"))) }
        catch (_: Exception) { statusText.text = "Không mở được trang cài đặt Chrome." }
    }

    private fun combinedAction() {
        if (!combinedPending) {
            combinedPending = true
            AlertDialog.Builder(this).setTitle("Bước 1/2 – Reset IP")
                .setMessage("Mở cài đặt mạng di động. Tắt dữ liệu di động, chờ 10–15 giây rồi bật lại. Quay lại ứng dụng và nhấn nút kết hợp lần nữa để sang bước xóa Chrome.")
                .setPositiveButton("Mở cài đặt mạng") { _, _ -> openMobileSettings() }
                .setNegativeButton("Hủy") { _, _ -> combinedPending = false }.show()
        } else {
            combinedPending = false
            log("Tiếp tục quy trình kết hợp: mở cài đặt xóa Chrome")
            openChromeSettings()
        }
    }

    private fun refreshIp() {
        statusText.text = "Đang kiểm tra IP công khai..."
        executor.execute {
            val ip = try { URL("https://api.ipify.org").readText().trim() } catch (_: Exception) { null }
            runOnUiThread {
                if (ip != null) {
                    ipText.text = "IP công khai: $ip"; statusText.text = "Đã cập nhật IP."
                    log("Ghi nhận IP: $ip")
                } else {
                    statusText.text = "Không lấy được IP. Kiểm tra Internet."; log("Kiểm tra IP thất bại")
                }
            }
        }
    }

    private fun log(message: String) {
        val time = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val old = prefs.getString("history", "") ?: ""
        prefs.edit().putString("history", "$time • $message\n$old".take(15000)).apply()
    }

    private fun showHistory() {
        val history = prefs.getString("history", "") ?: ""
        AlertDialog.Builder(this).setTitle("Lịch sử IP và thao tác")
            .setMessage(if (history.isBlank()) "Chưa có lịch sử." else history)
            .setPositiveButton("Đóng", null)
            .setNeutralButton("Xóa lịch sử") { _, _ ->
                prefs.edit().remove("history").apply()
                Toast.makeText(this, "Đã xóa lịch sử trong IP Cleaner", Toast.LENGTH_SHORT).show()
            }.show()
    }

    override fun onDestroy() { super.onDestroy(); executor.shutdown() }
}
