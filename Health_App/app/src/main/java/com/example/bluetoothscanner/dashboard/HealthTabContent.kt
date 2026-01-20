package com.example.bluetoothscanner.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bluetoothscanner.data.HealthDataState

@Composable
fun HealthTabContent(
    onConnectHealthConnect: () -> Unit
) {
    // Lấy dữ liệu
    val heartRate = HealthDataState.heartRate.value
    val spo2 = HealthDataState.bloodOxygen.value
    val steps = HealthDataState.steps.value

    // Logic cảnh báo
    val isHrWarning = heartRate > 0 && (heartRate > 100 || heartRate < 40)
    val isSpo2Warning = spo2 > 0 && spo2 < 95

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ==========================================
        // KHU VỰC 1: CÁC THẺ THÔNG SỐ (GRID)
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp) // Khoảng cách giữa 2 cột trái/phải
        ) {
            // --- CỘT TRÁI (WEIGHT 1) ---
            // Chứa: Nhịp tim + Bước chân
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Khoảng cách dọc giữa HR và Steps
            ) {
                // 1. Thẻ Nhịp tim (Góc trên trái)
                HealthCard(
                    icon = Icons.Default.Favorite,
                    label = "Nhịp tim",
                    value = heartRate,
                    unit = "BPM",
                    color = Color.Red,
                    isWarning = isHrWarning
                )

                // 2. Thẻ Bước chân (Nằm ngay dưới Nhịp tim)
                HealthCard(
                    icon = Icons.Default.Speed,
                    label = "Bước chân",
                    value = steps,
                    unit = "bước",
                    color = Color(0xFFFF9800)
                )
            }

            // --- CỘT PHẢI (WEIGHT 1) ---
            // Chứa: SpO2
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 3. Thẻ SpO2 (Góc trên phải)
                HealthCard(
                    icon = Icons.Default.WaterDrop,
                    label = "SpO2",
                    value = spo2,
                    unit = "%",
                    color = Color(0xFF00B0FF),
                    isWarning = isSpo2Warning
                )

                // (Bên dưới SpO2 để trống như layout bạn muốn)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ==========================================
        // KHU VỰC 2: CÁC BIỂU ĐỒ (NẰM DƯỚI CÙNG)
        // ==========================================

        // --- Biểu đồ Nhịp tim ---
        Text(
            text = "Biểu đồ Nhịp tim",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHrWarning) Color.Red else Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                LiveLineChart(
                    dataPoints = HealthDataState.heartRateHistory,
                    lineColor = Color.Red,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Biểu đồ SpO2 ---
        Text(
            text = "Biểu đồ SpO2",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSpo2Warning) Color.Red else Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                LiveLineChart(
                    dataPoints = HealthDataState.spO2History,
                    lineColor = if (isSpo2Warning) Color.Red else Color(0xFF00B0FF),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Thêm khoảng trống cuối cùng để cuộn không bị sát đáy
        Spacer(modifier = Modifier.height(30.dp))
    }
}