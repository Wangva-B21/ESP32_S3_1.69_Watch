package com.example.bluetoothscanner.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BleClientManager(private val context: Context) {

    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _receivedData = MutableStateFlow<ByteArray?>(null)
    val receivedData = _receivedData.asStateFlow()

    // UUID
    private val SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
    private val CHAR_UUID    = UUID.fromString("87654321-4321-4321-4321-ba0987654321")
    private val CCCD_UUID    = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    sealed class ConnectionState {
        object Connected : ConnectionState()
        object Connecting : ConnectionState()
        object Disconnected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        if (bluetoothGatt != null) {
            disconnect()
        }

        Log.d("BLE_CONNECT", "Đang kết nối NimBLE device: $address")
        val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val device = bluetoothAdapter.getRemoteDevice(address)

        // Android 13+ khuyến nghị dùng TRANSPORT_LE
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        _connectionState.value = ConnectionState.Connecting
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        Log.d("BLE_CONNECT", "Yêu cầu ngắt kết nối")
        bluetoothGatt?.disconnect()
    }

    private fun closeGatt() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.Disconnected
        _receivedData.value = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE_CONNECT", "✓ Đã kết nối! Đang khám phá dịch vụ...")
                _connectionState.value = ConnectionState.Connected
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLE_CONNECT", "⏹ Đã ngắt kết nối, status: $status")
                closeGatt()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    val characteristic = service.getCharacteristic(CHAR_UUID)
                    if (characteristic != null) {
                        enableNotification(gatt, characteristic)
                    } else {
                        Log.e("BLE_CONNECT", "Lỗi: Không tìm thấy Characteristic!")
                    }
                } else {
                    Log.e("BLE_CONNECT", "Lỗi: Không tìm thấy Service!")
                }
            } else {
                Log.w("BLE_CONNECT", "Khám phá dịch vụ thất bại: $status")
            }
        }

        // --- Kiểm tra đăng ký Notify thành công hay chưa ---
        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE_CONNECT", "✓ ĐĂNG KÝ NOTIFY THÀNH CÔNG! Đang chờ dữ liệu...")
            } else {
                Log.e("BLE_CONNECT", "❌ Đăng ký Notify thất bại: Status $status")
            }
        }

        // --- Xử lý dữ liệu nhận về ---

        // 1. Dành cho Android 12 trở xuống (API < 33)
        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            processData(characteristic.value)
        }

        // 2. Dành cho Android 13 trở lên (API >= 33)
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            processData(value)
        }

        private fun processData(data: ByteArray?) {
            _receivedData.value = data
            if (data != null && data.size >= 6) {
                val hr = data[0].toInt() and 0xFF
                val spo2 = data[1].toInt() and 0xFF
                val steps = (data[2].toInt() and 0xFF) or
                        ((data[3].toInt() and 0xFF) shl 8) or
                        ((data[4].toInt() and 0xFF) shl 16) or
                        ((data[5].toInt() and 0xFF) shl 24)

                Log.d("BLE_DATA", "📩 DATA -> HR: $hr, SpO2: $spo2, Steps: $steps")
            } else {
                Log.d("BLE_DATA", "📩 Nhận dữ liệu rỗng hoặc sai định dạng")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        // 1. Bật Notify cục bộ
        val setLocal = gatt.setCharacteristicNotification(characteristic, true)
        if (!setLocal) {
            Log.e("BLE_CONNECT", "Lỗi: setCharacteristicNotification trả về false")
            return
        }

        // 2. Ghi Descriptor gửi lên ESP32
        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
            Log.d("BLE_CONNECT", "⏳ Đang ghi Descriptor...")
        } else {
            Log.e("BLE_CONNECT", "Lỗi: Không tìm thấy CCCD Descriptor!")
        }
    }
}