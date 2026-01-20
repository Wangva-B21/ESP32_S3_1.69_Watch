// File: app/src/main/java/com/example/bluetoothscanner/dashboard/ScanButton.kt
package com.example.bluetoothscanner.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.bluetoothscanner.bluetooh.BluetoothHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanButton(
    bluetoothHelper: BluetoothHelper,
    modifier: Modifier = Modifier
) {
    // ĐÚNG: Chỉ collectAsState() cho StateFlow (isScanning)
    val isScanning by bluetoothHelper.isScanning.collectAsState()

    // ĐÚNG: isBtEnabled là Boolean → đọc trực tiếp, KHÔNG dùng by
    val isBluetoothEnabled = bluetoothHelper.isBtEnabled

    Button(
        onClick = { bluetoothHelper.startScanOrRequest() },
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isScanning) Color(0xFFFF5722) else Color(0xFF6200EE)
        )
        // Nếu muốn nút luôn bấm được (để hiện popup bật BT) → KHÔNG dùng enabled
        // Nếu muốn nút mờ khi tắt BT → thêm: enabled = isBluetoothEnabled
    ) {
        Text(
            text = when {
                !isBluetoothEnabled -> "Bật Bluetooth để quét"
                isScanning -> "Đang quét... (Dừng)"
                else -> "Quét thiết bị BLE"
            },
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
    }
}