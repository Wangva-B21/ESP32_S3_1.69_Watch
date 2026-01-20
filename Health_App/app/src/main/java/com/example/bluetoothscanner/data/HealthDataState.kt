//bluetoothscanner/data/HealthDataState.kt
package com.example.bluetoothscanner.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

object HealthDataState {
    val heartRate = mutableStateOf(0)
    val bloodOxygen = mutableStateOf(0)
    val steps = mutableStateOf(0)
    val calories = mutableStateOf(0)
    val distance = mutableStateOf(0f)

    val heartRateHistory = mutableStateListOf<Int>()
    val spO2History = mutableStateListOf<Int>()
}
