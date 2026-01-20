package com.example.bluetoothscanner.bluetooh

import android.Manifest
import android.os.Build
import androidx.activity.result.ActivityResultLauncher

fun requestPermissions(launcher: ActivityResultLauncher<Array<String>>) {
    val permissions = mutableListOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    launcher.launch(permissions.toTypedArray())
}
