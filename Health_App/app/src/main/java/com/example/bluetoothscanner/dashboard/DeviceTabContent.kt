package com.example.bluetoothscanner.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetoothscanner.bluetooh.BluetoothHelper

// <=== BIẾN TOÀN CỤC: Lưu địa chỉ thiết bị đang kết nối (Sống dai kể cả khi chuyển tab) ===>
// Mặc định là rỗng. Khi bấm kết nối sẽ lưu vào đây.
private var globalConnectedAddress by mutableStateOf<String?>(null)

@Composable
fun DeviceTabContent(
    bluetoothHelper: BluetoothHelper
) {
    val devices by bluetoothHelper.devices.collectAsState()
    val isScanning by bluetoothHelper.isScanning.collectAsState()
    val connectionState by bluetoothHelper.connectionState.collectAsState()
    val receivedData by bluetoothHelper.receivedData.collectAsState()

    val stateString = connectionState.toString()

    // Logic: Kết nối thành công khi trạng thái là Connected HOẶC đã có dữ liệu
    val isGlobalConnected = stateString.contains("Connected") || receivedData != null

    // Nếu bị mất kết nối (Disconnected), tự động reset biến nhớ để không hiện xanh nữa
    LaunchedEffect(stateString) {
        if (stateString == "Disconnected") {
            // Giữ nguyên hoặc reset tùy logic, ở đây mình giữ nguyên để người dùng biết vừa kết nối cái nào
            // Nhưng nếu muốn chuẩn chỉ:
            // globalConnectedAddress = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =================================================================
        // 1. NÚT QUÉT
        // =================================================================
        Button(
            onClick = { bluetoothHelper.startScanOrRequest() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isScanning) Color(0xFFFF5722) else Color(0xFF2196F3)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = if (isScanning) Icons.Filled.Stop else Icons.Filled.BluetoothSearching,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isScanning) "Dừng quét" else "Quét thiết bị BLE",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // =================================================================
        // 2. DANH SÁCH THIẾT BỊ
        // =================================================================
        if (devices.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.SearchOff, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (isScanning) "Đang tìm kiếm..." else "Chưa tìm thấy thiết bị nào.\nHãy bấm Quét.",
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(devices) { device ->

                    // Logic kiểm tra xem thiết bị này có phải là cái đang kết nối không
                    // Dựa vào biến toàn cục globalConnectedAddress
                    val isThisDeviceConnected = isGlobalConnected && (device.address == globalConnectedAddress)
                    val isConnecting = (stateString == "Connecting") && (device.address == globalConnectedAddress)

                    // Card hiển thị
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        // NẾU LÀ THIẾT BỊ ĐANG KẾT NỐI: VIỀN XANH ĐẬM
                        border = if (isThisDeviceConnected) BorderStroke(2.dp, Color(0xFF4CAF50)) else null,
                        colors = CardDefaults.cardColors(
                            // NẾU LÀ THIẾT BỊ ĐANG KẾT NỐI: NỀN XANH NHẠT, NỔI BẬT HẲN LÊN
                            containerColor = if (isThisDeviceConnected) Color(0xFFE8F5E9) else Color.White
                        ),
                        // Đổ bóng cao hơn nếu đang kết nối
                        elevation = CardDefaults.cardElevation(if (isThisDeviceConnected) 8.dp else 1.dp),
                        onClick = {
                            // Chỉ bấm được khi chưa kết nối cái này
                            if (!isThisDeviceConnected) {
                                globalConnectedAddress = device.address // <=== LƯU VÀO BIẾN TOÀN CỤC NGAY
                                bluetoothHelper.connectToDevice(device.address)
                            }
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // --- HÀNG TRÊN: TÊN & TRẠNG THÁI ---
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // ICON: Check Xanh to rõ
                                Icon(
                                    imageVector = if (isThisDeviceConnected) Icons.Filled.CheckCircle else Icons.Filled.Watch,
                                    contentDescription = null,
                                    tint = if (isThisDeviceConnected) Color(0xFF4CAF50) else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.name ?: "Unknown Device",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        // Tên thiết bị đổi màu xanh luôn cho dễ nhìn
                                        color = if (isThisDeviceConnected) Color(0xFF2E7D32) else Color.Black
                                    )
                                    Text(
                                        text = device.address,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }

                                // Trạng thái chữ bên phải
                                if (isConnecting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else if (isThisDeviceConnected) {
                                    // Hiện chữ Connected màu xanh
                                    Text(
                                        text = "Connected",
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Mũi tên chỉ thị
                                Icon(
                                    imageVector = if (isThisDeviceConnected) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Toggle",
                                    tint = if (isThisDeviceConnected) Color(0xFF4CAF50) else Color.Gray
                                )
                            }

                            // --- HÀNG DƯỚI: DATA & DISCONNECT (TỰ ĐỘNG MỞ KHI KẾT NỐI) ---
                            // Dùng AnimatedVisibility để tự động xổ xuống
                            AnimatedVisibility(visible = isThisDeviceConnected) {
                                Column {
                                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFC8E6C9))

                                    if (receivedData != null && receivedData!!.size >= 6) {
                                        val hr = receivedData!![0].toInt() and 0xFF
                                        val spo2 = receivedData!![1].toInt() and 0xFF
                                        val s1 = (receivedData!![2].toInt() and 0xFF)
                                        val s2 = (receivedData!![3].toInt() and 0xFF) shl 8
                                        val s3 = (receivedData!![4].toInt() and 0xFF) shl 16
                                        val s4 = (receivedData!![5].toInt() and 0xFF) shl 24
                                        val steps = s1 or s2 or s3 or s4

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            MiniDataItem(Icons.Filled.Favorite, "$hr", "bpm", Color.Red)
                                            MiniDataItem(Icons.Filled.WaterDrop, "$spo2", "%", Color.Blue)
                                            MiniDataItem(Icons.Filled.DirectionsWalk, "$steps", "bước", Color(0xFFFF9800))
                                        }
                                    } else {
                                        Text(
                                            "Đang chờ dữ liệu từ cảm biến...",
                                            fontSize = 13.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = Color.Gray,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // NÚT DISCONNECT
                                    Button(
                                        onClick = {
                                            bluetoothHelper.disconnect()
                                            // Không cần reset biến toàn cục ở đây cũng được,
                                            // để nó tự động xử lý khi trạng thái chuyển về Disconnected
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                                        modifier = Modifier.fillMaxWidth().height(45.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Disconnect / Ngắt kết nối", color = Color(0xFFD32F2F))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniDataItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = unit, fontSize = 10.sp, color = Color.Gray)
    }
}