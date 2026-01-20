package com.example.bluetoothscanner.dashboard

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource // <=== MỚI: Dùng để tải icon từ file xml
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetoothscanner.R // <=== Đảm bảo import đúng gói R của bạn
import com.example.bluetoothscanner.bluetooh.BluetoothHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDashboardScreen(
    bluetoothHelper: BluetoothHelper,
    onEnableBluetooth: () -> Unit,
    onConnectHealthConnect: () -> Unit,
    onNavigateToScanScreen: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (selectedTabIndex == 0) "Sức khỏe" else "Thiết bị",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                ),
                actions = {
                    if (selectedTabIndex == 0) {
                        // NÚT HEALTH CONNECT VỚI ICON CHUẨN
                        TextButton(
                            onClick = onConnectHealthConnect,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF00796B) // Màu xanh đậm
                            )
                        ) {
                            // Dùng painterResource để load file xml vừa tạo
                            Icon(
                                painter = painterResource(id = R.drawable.ic_health_connect_logo),
                                contentDescription = "Health Connect Logo",
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF00796B) // Tô màu xanh cho icon đen gốc
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Health Connect",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(
                                    width = 6.5.dp,
                                    color = if (selectedTabIndex == 0) Color(0xFFFFA500) else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                    },
                    label = {
                        Text("Sức khỏe", color = if (selectedTabIndex == 0) Color(0xFFFFA500) else Color.Gray)
                    },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFFFFF3E0))
                )

                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = {
                        Icon(imageVector = Icons.Filled.Watch, contentDescription = "Thiết bị")
                    },
                    label = { Text("Thiết bị") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2196F3),
                        selectedTextColor = Color(0xFF2196F3),
                        indicatorColor = Color(0xFFE3F2FD)
                    )
                )
            }
        }
    ) { paddingValues ->
        Crossfade(
            targetState = selectedTabIndex,
            animationSpec = tween(durationMillis = 300),
            modifier = Modifier.padding(paddingValues),
            label = "TabAnimation"
        ) { tabIndex ->
            when (tabIndex) {
                0 -> HealthTabContent(onConnectHealthConnect)
                1 -> DeviceTabContent(bluetoothHelper)
            }
        }
    }
}