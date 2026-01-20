package com.example.bluetoothscanner.data

import android.util.Log

class HealthDataUpdater {

    fun parseAndApplyData(data: ByteArray) {
        try {
            // Cấu trúc gói tin: [HR, SpO2, Step1, Step2, Step3, Step4]
            if (data.size >= 6) {
                val heartRate = data[0].toInt() and 0xFF
                val spO2 = data[1].toInt() and 0xFF

                val step1 = (data[2].toInt() and 0xFF)
                val step2 = (data[3].toInt() and 0xFF) shl 8
                val step3 = (data[4].toInt() and 0xFF) shl 16
                val step4 = (data[5].toInt() and 0xFF) shl 24
                val steps = step1 or step2 or step3 or step4

                // --- 1. Cập nhật Text (Phần này bạn đã có, nên số mới nhảy) ---
                HealthDataState.heartRate.value = heartRate
                HealthDataState.bloodOxygen.value = spO2
                HealthDataState.steps.value = steps

                // --- 2. Cập nhật Biểu đồ (PHẦN BẠN ĐANG THIẾU) ---
                // <=== QUAN TRỌNG: Phải gọi hàm này thì biểu đồ mới có dữ liệu vẽ ===>
                updateHistory(HealthDataState.heartRateHistory, heartRate)
                updateHistory(HealthDataState.spO2History, spO2)

                Log.d("BLE_PARSE", "✅ HR=$heartRate, SpO2=$spO2, Steps=$steps")
            }
        } catch (e: Exception) {
            Log.e("BLE_PARSE", "Lỗi phân tích dữ liệu: ${e.message}")
        }
    }

    // Hàm phụ trợ: Thêm dữ liệu mới và xóa dữ liệu cũ (Giữ lại 50 điểm)
    private fun updateHistory(list: androidx.compose.runtime.snapshots.SnapshotStateList<Int>, newValue: Int) {
        // Thêm vào cuối danh sách
        list.add(newValue)

        // Nếu danh sách dài quá 50 điểm thì xóa bớt điểm đầu tiên đi
        // để tạo hiệu ứng biểu đồ trôi (Sliding Window)
        if (list.size > 50) {
            list.removeAt(0)
        }
    }
}