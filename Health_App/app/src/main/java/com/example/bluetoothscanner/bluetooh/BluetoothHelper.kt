package com.example.bluetoothscanner.bluetooh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import com.example.bluetoothscanner.ble.BleClientManager
import com.example.bluetoothscanner.data.HealthDataUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BleDevice(val name: String, val address: String)

class BluetoothHelper(
    private val bluetoothAdapter: BluetoothAdapter,
    private val bleClientManager: BleClientManager,
    private val healthDataUpdater: HealthDataUpdater,
    private val onRequestEnableBt: () -> Unit = {}
) {
    private val scanner by lazy { bluetoothAdapter.bluetoothLeScanner }

    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    val connectionState = bleClientManager.connectionState
    val receivedData = bleClientManager.receivedData // Kiểu dữ liệu giờ là ByteArray?
    val isBtEnabled: Boolean get() = bluetoothAdapter.isEnabled

    fun startDataCollection(scope: kotlinx.coroutines.CoroutineScope) {
        scope.launch {
            receivedData.collectLatest { data ->
                // SỬA: Kiểm tra mảng byte
                if (data != null && data.isNotEmpty()) {
                    healthDataUpdater.parseAndApplyData(data)
                }
            }
        }
    }

    // ... (Các phần ScanCallback và logic quét giữ nguyên) ...
    // Giữ nguyên phần scanCallback, startRealScan, stopRealScan, startScanOrRequest, connectToDevice, disconnect
    // ...
    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val name = device.name ?: "N/A"
                val bleDev = BleDevice(name, device.address)
                if (_devices.value.none { it.address == bleDev.address }) {
                    _devices.value = _devices.value + bleDev
                }
            }
        }
        override fun onScanFailed(errorCode: Int) { _isScanning.value = false }
    }

    @SuppressLint("MissingPermission")
    private fun startRealScan() {
        _devices.value = emptyList()
        _isScanning.value = true
        scanner.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun stopRealScan() {
        _isScanning.value = false
        scanner.stopScan(scanCallback)
    }

    fun startScanOrRequest() {
        if (!bluetoothAdapter.isEnabled) onRequestEnableBt()
        else if (_isScanning.value) stopRealScan() else startRealScan()
    }

    fun connectToDevice(address: String) {
        stopRealScan()
        bleClientManager.connectToDevice(address)
    }

    fun disconnect() = bleClientManager.disconnect()
}