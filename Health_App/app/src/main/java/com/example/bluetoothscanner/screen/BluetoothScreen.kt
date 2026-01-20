package com.example.bluetoothscanner.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bluetoothscanner.bluetooh.BleDevice
import com.example.bluetoothscanner.bluetooh.BluetoothHelper
import com.example.bluetoothscanner.dashboard.ScanButton
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    bluetoothHelper: BluetoothHelper,
    onRequestEnableBt: () -> Unit
) {
    val devices by bluetoothHelper.devices.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bluetooth Scanner") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            ScanButton(
                bluetoothHelper = bluetoothHelper,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Thiết bị tìm thấy:", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(devices) { device ->
                    Card(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .fillMaxWidth()
                            .clickable {
                                bluetoothHelper.connectToDevice(device.address)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = device.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }

                if (devices.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (bluetoothHelper.isScanning.value)
                                    "Đang quét thiết bị..."
                                else "Nhấn nút trên để quét",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}