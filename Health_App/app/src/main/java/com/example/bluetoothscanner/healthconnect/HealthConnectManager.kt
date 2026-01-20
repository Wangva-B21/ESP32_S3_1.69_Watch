package com.example.bluetoothscanner.healthconnect

import android.content.Context
import android.content.SharedPreferences // <== Import cái này
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Percentage
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.bluetoothscanner.data.HealthDataState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

class HealthConnectManager(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope
) {

    private val healthConnectClient = HealthConnectClient.getOrCreate(context)
    private var isAutoSyncing = false

    // Khởi tạo SharedPreferences để lưu trữ số bước "đã gửi"
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("HealthConnectPrefs", Context.MODE_PRIVATE)

    // Key để lưu dữ liệu
    private val PREF_LAST_STEPS = "last_synced_steps_key"

    private val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getWritePermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )

    private val permissionLauncherHealth = (context as ComponentActivity)
        .registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            if (granted.containsAll(PERMISSIONS)) {
                Toast.makeText(context, "Đã kết nối Google Fit!", Toast.LENGTH_SHORT).show()
                startAutoSyncLoop()
            } else {
                Toast.makeText(context, "Thiếu quyền! Vui lòng cấp đủ quyền.", Toast.LENGTH_SHORT).show()
            }
        }

    fun requestHealthConnectPermissionAndPush() {
        lifecycleScope.launch {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(PERMISSIONS)) {
                startAutoSyncLoop()
            } else {
                permissionLauncherHealth.launch(PERMISSIONS)
            }
        }
    }

    private fun startAutoSyncLoop() {
        if (isAutoSyncing) return
        isAutoSyncing = true

        // 1. Đọc số bước đã lưu từ lần chạy trước (Mặc định là 0 nếu chưa có gì)
        // Điều này giúp App nhớ được trạng thái kể cả khi bạn tắt đi bật lại
        var lastSyncedSteps = sharedPreferences.getLong(PREF_LAST_STEPS, 0L)

        lifecycleScope.launch {
            println("🔄 Bắt đầu vòng lặp đồng bộ. Mốc cũ đã lưu: $lastSyncedSteps")
            while (isActive) {
                val currentHeartRate = HealthDataState.heartRate.value.toLong()
                val currentSpO2 = HealthDataState.bloodOxygen.value.toDouble()
                val currentSteps = HealthDataState.steps.value.toLong()

                // --- XỬ LÝ HEART RATE & SPO2 (Giữ nguyên) ---
                if (currentHeartRate > 0) insertHeartRate(currentHeartRate)
                if (currentSpO2 > 0) insertOxygenSaturation(currentSpO2)

                // --- XỬ LÝ BƯỚC CHÂN (LOGIC MỚI - CHUẨN) ---
                if (currentSteps > 0) {

                    // Trường hợp 1: ESP32 bị reset (Ví dụ: Từ 1000 bước về 0 bước)
                    // Hoặc qua ngày mới mà ESP tự reset
                    if (currentSteps < lastSyncedSteps) {
                        println("⚠️ Phát hiện ESP reset số bước. Đặt lại mốc về 0.")
                        lastSyncedSteps = 0
                        // Cập nhật lại vào bộ nhớ luôn để tránh lỗi
                        sharedPreferences.edit().putLong(PREF_LAST_STEPS, 0).apply()
                    }

                    // Trường hợp 2: Có bước đi mới
                    val deltaSteps = currentSteps - lastSyncedSteps

                    if (deltaSteps > 0) {
                        // Gửi phần chênh lệch lên Health Connect
                        insertSteps(deltaSteps)

                        // Cập nhật mốc mới bằng số hiện tại
                        lastSyncedSteps = currentSteps

                        // QUAN TRỌNG: Lưu ngay vào bộ nhớ máy
                        // Để lần sau mở App lên nó biết là đã gửi đến số này rồi
                        sharedPreferences.edit().putLong(PREF_LAST_STEPS, lastSyncedSteps).apply()
                    }
                }

                delay(10000) // 10 giây đẩy 1 lần
            }
        }
    }

    private suspend fun insertSteps(count: Long) {
        val now = Instant.now()
        val startTime = now.minusSeconds(10) // Giả định quãng đi trong 10s vừa qua

        try {
            val stepsRecord = StepsRecord(
                startTime = startTime,
                endTime = now,
                count = count,
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata = Metadata(dataOrigin = DataOrigin(context.packageName))
            )
            healthConnectClient.insertRecords(listOf(stepsRecord))
            println("✅ [HC] Đã cộng thêm: +$count bước. (Tổng trên ESP: ${HealthDataState.steps.value})")
        } catch (e: Exception) {
            println("❌ Lỗi Steps: ${e.message}")
        }
    }

    // ... (Các hàm insertHeartRate và insertOxygenSaturation giữ nguyên)
    private suspend fun insertHeartRate(bpm: Long) { /* Giữ nguyên code cũ */
        val now = Instant.now()
        val startTime = now.minusSeconds(1)
        try {
            val hrRecord = HeartRateRecord(
                samples = listOf(HeartRateRecord.Sample(time = now, beatsPerMinute = bpm)),
                startTime = startTime,
                endTime = now,
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata = Metadata(dataOrigin = DataOrigin(context.packageName))
            )
            healthConnectClient.insertRecords(listOf(hrRecord))
        } catch (e: Exception) { }
    }

    private suspend fun insertOxygenSaturation(spO2: Double) { /* Giữ nguyên code cũ */
        val now = Instant.now()
        try {
            val spO2Record = OxygenSaturationRecord(
                time = now,
                percentage = Percentage(spO2),
                zoneOffset = ZoneOffset.UTC,
                metadata = Metadata(dataOrigin = DataOrigin(context.packageName))
            )
            healthConnectClient.insertRecords(listOf(spO2Record))
        } catch (e: Exception) { }
    }
}