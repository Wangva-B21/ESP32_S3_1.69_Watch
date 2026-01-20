#ifndef WIFI_TIME_MANAGER_H
#define WIFI_TIME_MANAGER_H

#include <Arduino.h>
#include <WiFi.h>
#include "time.h"
#include "GlobalConfig.h"

// Hàm khởi tạo WiFi và cấu hình NTP
void initWifiTime();

// Hàm lấy chuỗi giờ (HH:MM)
void get_time_string(char* buffer, size_t size);

// Hàm lấy chuỗi ngày (DD/MM/YYYY)
void get_date_string(char* buffer, size_t size);

// Hàm duy trì kết nối (gọi trong loop hoặc task nếu cần, nhưng ESP32 tự quản lý khá tốt)
void checkWifiStatus();

#endif