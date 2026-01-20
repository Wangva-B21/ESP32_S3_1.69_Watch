#include "WifiTimeManager.h"

void initWifiTime() {
    Serial.println(">> Init WiFi & Time Service...");
    
    // Ngắt kết nối cũ nếu có
    WiFi.disconnect(true);
    WiFi.mode(WIFI_STA);
    
    // Bắt đầu kết nối
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    
    // Cấu hình Time Server
    configTime(GMT_OFFSET_SEC, DAYLIGHT_OFFSET, NTP_SERVER);
    
    // Lưu ý: Chúng ta KHÔNG dùng while() chờ kết nối ở đây.
    // Vì nếu WiFi lỗi, nó sẽ treo vòng lặp -> WDT reset hệ thống -> Hỏng BLE.
    // Hãy để WiFi tự kết nối ngầm (background).
}

void checkWifiStatus() {
    // Cập nhật biến toàn cục để main.cpp và DisplayManager biết hiển thị Icon
    if (WiFi.status() == WL_CONNECTED) {
        wifiConnected = true;
    } else {
        wifiConnected = false;
    }
}

void get_time_string(char* buffer, size_t size) {
    struct tm timeinfo;
    if(!getLocalTime(&timeinfo, 10)) { // timeout rất ngắn (10ms) để ko chặn BLE
        snprintf(buffer, size, "--:--"); 
        return;
    }
    strftime(buffer, size, "%H:%M", &timeinfo);
}

void get_date_string(char* buffer, size_t size) {
    struct tm timeinfo;
    if(!getLocalTime(&timeinfo, 10)) {
        snprintf(buffer, size, "--/--/----"); 
        return;
    }
    // Định dạng ngày tháng năm: 07/12/2025
    strftime(buffer, size, "%d/%m/%Y", &timeinfo);
}