package com.example.bluetoothscanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.bluetoothscanner.ble.BleClientManager
import com.example.bluetoothscanner.bluetooh.BluetoothHelper
import com.example.bluetoothscanner.dashboard.HealthDashboardScreen
import com.example.bluetoothscanner.data.HealthDataUpdater
import com.example.bluetoothscanner.healthconnect.HealthConnectManager

class MainActivity : ComponentActivity() {

    // <=== QUAN TRỌNG: Biến toàn cục để Service chạy ngầm có thể truy cập ===>
    companion object {
        lateinit var bluetoothHelper: BluetoothHelper
        fun bluetoothHelperIsInitialized(): Boolean {
            return ::bluetoothHelper.isInitialized
        }
    }

    private lateinit var bleClientManager: BleClientManager
    private val healthDataUpdater = HealthDataUpdater()

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // Launcher để bật Bluetooth nếu đang tắt
    private val btEnableLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    // ID kênh thông báo
    private val CHANNEL_ID = "BLE_ALERT_CHANNEL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Tạo kênh thông báo ngay khi mở App (để sẵn sàng bắn thông báo)
        createNotificationChannel()

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = btManager.adapter

        bleClientManager = BleClientManager(this)

        // 2. Khởi tạo BluetoothHelper và gán vào biến Companion Object
        bluetoothHelper = BluetoothHelper(
            bluetoothAdapter = adapter,
            bleClientManager = bleClientManager,
            healthDataUpdater = healthDataUpdater,
            onRequestEnableBt = {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                btEnableLauncher.launch(enableBtIntent)
            }
        )

        healthConnectManager = HealthConnectManager(this, lifecycleScope)

        // 3. Cấu hình xin quyền
        setupPermissionLauncher()

        // 4. Kiểm tra quyền và bắt đầu chạy
        if (!hasAllPermissions()) {
            requestAllNecessaryPermissions()
        } else {
            // Nếu đã có đủ quyền -> Chạy ngay
            proceedAfterPermissions(adapter)
            startHealthDataCollection()
            startBackgroundService() // <=== KÍCH HOẠT CHẠY NGẦM
        }

        // 5. Hiển thị giao diện
        setContent {
            HealthDashboardScreen(
                bluetoothHelper = bluetoothHelper,
                onEnableBluetooth = {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    btEnableLauncher.launch(enableBtIntent)
                },
                onConnectHealthConnect = {
                    healthConnectManager.requestHealthConnectPermissionAndPush()
                }
            )
        }
    }

    // <=== HÀM KÍCH HOẠT SERVICE CHẠY NGẦM ===>
    private fun startBackgroundService() {
        val intent = Intent(this, HealthService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun startHealthDataCollection() {
        bluetoothHelper.startDataCollection(lifecycleScope)
        Toast.makeText(this, "✓ Đã bắt đầu quét và xử lý dữ liệu!", Toast.LENGTH_SHORT).show()
    }

    // <=== TẠO KÊNH THÔNG BÁO (BẮT BUỘC CHO ANDROID 8+) ===>
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Cảnh báo mất kết nối"
            val descriptionText = "Thông báo khẩn cấp khi đồng hồ bị ngắt kết nối"
            val importance = NotificationManager.IMPORTANCE_HIGH // Quan trọng CAO để nhảy Pop-up

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000) // Rung mạnh

                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // <=== CÁC HÀM XỬ LÝ QUYỀN ===>
    private fun hasAllPermissions(): Boolean {
        val requiredPermissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
        )
        // Android 13+ cần quyền thông báo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grants ->
            val allGranted = grants.values.all { it }
            if (allGranted) {
                Toast.makeText(this, "✓ Đã cấp đủ quyền!", Toast.LENGTH_SHORT).show()
                val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

                proceedAfterPermissions(btManager.adapter)
                startHealthDataCollection()
                startBackgroundService() // <=== CẤP QUYỀN XONG LÀ CHẠY NGẦM LUÔN
            } else {
                Toast.makeText(this, "⚠️ Bạn cần cấp quyền để app hoạt động!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestAllNecessaryPermissions() {
        val permissionsToRequest = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private fun proceedAfterPermissions(adapter: BluetoothAdapter) {
        if (!adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            btEnableLauncher.launch(enableBtIntent)
        }
        ensureLocationEnabled()
    }

    private fun ensureLocationEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
        if (!isLocationEnabled) {
            Toast.makeText(this, "⚠️ Vui lòng bật GPS để quét Bluetooth!", Toast.LENGTH_LONG).show()
            startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }
}