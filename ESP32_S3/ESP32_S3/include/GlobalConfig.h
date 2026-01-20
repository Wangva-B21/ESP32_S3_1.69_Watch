#ifndef GLOBAL_CONFIG_H
#define GLOBAL_CONFIG_H

#include <Arduino.h>
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"
#include "pin_config.h"

// --- 1. CẤU HÌNH WIFI & TIME ---
#define WIFI_SSID "A35"
#define WIFI_PASSWORD "00000000"
#define NTP_SERVER "pool.ntp.org"
#define GMT_OFFSET_SEC 25200
#define DAYLIGHT_OFFSET 0

// --- 2. CẤU HÌNH CHÂN SENSOR (GIỮ NGUYÊN) ---
#define I2C_SDA_PIN 11
#define I2C_SCL_PIN 10
#define MAX30102_INT_PIN 16

// --- [MỚI] 3. CẤU HÌNH CHÂN POWER & BUTTON ---
// Chọn cấu hình chân: 1 = New, 0 = Old (Sửa ở đây)
#define USE_NEW_PIN_CONFIG 1

#if (USE_NEW_PIN_CONFIG)
#define PIN_BUTTON_IN 40 // Input
#define PIN_POWER_OUT 41 // Output (Giữ nguồn)
#define PIN_BUZZER 42    // Buzzer
#else
#define PIN_BUTTON_IN 36 // Input
#define PIN_POWER_OUT 35 // Output (Giữ nguồn)
#define PIN_BUZZER 33    // Buzzer
#endif

// --- 4. SHARED RESOURCES ---
extern SemaphoreHandle_t xDataMutex;
extern SemaphoreHandle_t xI2CMutex;
extern SemaphoreHandle_t xMaxIntSemaphore;

// --- 5. SHARED DATA ---
extern volatile int sharedStepCount;
extern volatile int sharedHeartRate;
extern volatile int sharedSpO2;
extern volatile bool wifiConnected;

#endif