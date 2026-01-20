package com.example.bluetoothscanner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HealthService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var isConnected = false
    private var lastDataTime: Long = 0

    private var lastVitalSignAlertTime: Long = 0
    private var isDisconnectAlarmTriggered = false

    private val FOREGROUND_ID = 1
    private val CHANNEL_ID = "HEALTH_MEDICAL_ALERT_FAST_V8" // Đổi ID để reset cài đặt

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(FOREGROUND_ID, buildNotification("⏳ Đang theo dõi chỉ số sức khỏe..."))

        monitorBluetoothState()
        monitorDataTraffic()
        startWatchdogTimer()

        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        serviceJob.cancel()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        stopSelf()
    }

    private fun monitorBluetoothState() {
        serviceScope.launch {
            if (!MainActivity.bluetoothHelperIsInitialized()) return@launch

            MainActivity.bluetoothHelper.connectionState.collect { state ->
                val stateString = state.toString()

                if (stateString.contains("Connected")) {
                    isConnected = true
                    updateNotification("✅ Đang theo dõi sức khỏe")
                    isDisconnectAlarmTriggered = false
                }
                else if (stateString == "Disconnected") {
                    isConnected = false
                    if (!isDisconnectAlarmTriggered) {
                        sendAlertPopup("⚠️ Mất kết nối Bluetooth!", isCritical = false)
                        updateNotification("❌ Đã ngắt kết nối thiết bị")
                        isDisconnectAlarmTriggered = true
                    }
                }
            }
        }
    }

    private fun monitorDataTraffic() {
        serviceScope.launch {
            if (!MainActivity.bluetoothHelperIsInitialized()) return@launch

            MainActivity.bluetoothHelper.receivedData.collect { data ->
                if (data != null && data.size >= 2) {
                    lastDataTime = System.currentTimeMillis()

                    val hr = data[0].toInt() and 0xFF
                    val spo2 = data[1].toInt() and 0xFF

                    analyzeVitalSigns(hr, spo2)
                }
            }
        }
    }

    // <=== LOGIC PHÂN TÍCH Y TẾ (ĐÃ CHỈNH LẠI THỜI GIAN) ===>
    private fun analyzeVitalSigns(hr: Int, spo2: Int) {
        val currentTime = System.currentTimeMillis()
        var alertMessage = ""
        var isCritical = false

        // --- 1. ĐÁNH GIÁ MỨC ĐỘ ---

        // SpO2
        if (spo2 > 0) {
            when {
                spo2 >= 97 -> { }
                spo2 in 94..96 -> {
                    alertMessage = "⚠️ SpO2 hơi thấp ($spo2%). Hít thở sâu."
                }
                spo2 in 90..93 -> {
                    alertMessage = "⚠️ SpO2 THẤP ($spo2%)! Cần hỗ trợ oxy."
                    isCritical = true
                }
                spo2 < 90 -> {
                    alertMessage = "🚨 CẤP CỨU! SpO2 cực thấp ($spo2%). Nguy hiểm!"
                    isCritical = true
                }
            }
        }

        // Nhịp tim
        var hrMsg = ""
        if (hr > 0) {
            when {
                hr in 60..100 -> { }
                hr in 101..120 -> {
                    hrMsg = "⚠️ Nhịp tim nhanh ($hr). Hãy nghỉ ngơi."
                }
                hr > 120 -> {
                    hrMsg = "🚨 NGUY HIỂM! Tim quá nhanh ($hr). Đi khám ngay!"
                    isCritical = true
                }
                hr < 60 -> {
                    hrMsg = "⚠️ Nhịp tim chậm ($hr). Theo dõi thêm."
                }
            }
        }

        if (hrMsg.isNotEmpty()) {
            alertMessage = if (alertMessage.isNotEmpty()) "$alertMessage\n$hrMsg" else hrMsg
        }

        // --- 2. QUYẾT ĐỊNH THỜI GIAN CHỜ (DEBOUNCE) ---
        // Nếu là CẤP CỨU (Critical) -> 3 giây báo 1 lần (Rất nhanh)
        // Nếu là Cảnh báo thường -> 10 giây báo 1 lần
        val waitTime = if (isCritical) 3000 else 10000

        if (currentTime - lastVitalSignAlertTime < waitTime) return // Chưa đến lúc báo lại thì thoát

        // --- 3. GỬI CẢNH BÁO ---
        if (alertMessage.isNotEmpty()) {
            sendAlertPopup(alertMessage, isCritical)
            updateNotification(if (isCritical) "🚨 NGUY HIỂM TÍNH MẠNG!" else "⚠️ Cảnh báo sức khỏe")
            lastVitalSignAlertTime = currentTime
        } else {
            // Nếu bình thường thì reset dòng chữ
            // (Thêm điều kiện để không spam update notification liên tục)
            if (currentTime - lastVitalSignAlertTime > 5000) {
                updateNotification("✅ Chỉ số sức khỏe ổn định")
            }
        }
    }

    private fun startWatchdogTimer() {
        serviceScope.launch {
            while (isActive) {
                delay(2000)
                if (isConnected) {
                    val timeDiff = System.currentTimeMillis() - lastDataTime
                    if (timeDiff > 8000 && !isDisconnectAlarmTriggered && lastDataTime > 0) {
                        isDisconnectAlarmTriggered = true
                        sendAlertPopup("⚠️ Mất tín hiệu cảm biến!", isCritical = false)
                        updateNotification("📡 Mất tín hiệu - Đang chờ kết nối lại...")
                    }
                }
            }
        }
    }

    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(FOREGROUND_ID, buildNotification(contentText))
    }

    private fun buildNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Health Monitor")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun sendAlertPopup(msg: String, isCritical: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Rung mạnh hơn nếu Critical
        val vibrationPattern = if (isCritical)
            longArrayOf(0, 500, 100, 500, 100, 500) // Rung 3 hồi dồn dập (Cấp cứu)
        else
            longArrayOf(0, 500, 200, 500) // Rung 2 hồi (Cảnh báo thường)

        val alertNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isCritical) "🚨 BÁO ĐỘNG KHẨN CẤP!" else "⚠️ Cảnh báo")
            .setContentText(msg)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(vibrationPattern)
            .setSound(RingtoneManager.getDefaultUri(if (isCritical) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(999, alertNotification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Y Tế Khẩn Cấp Fast"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = "Cảnh báo sức khỏe nguy hiểm"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}