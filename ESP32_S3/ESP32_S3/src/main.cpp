#include <Arduino.h>
#include <Wire.h>
#include <NimBLEDevice.h>
#include <esp_task_wdt.h>
#include "GlobalConfig.h"
#include "WifiTimeManager.h"

// =================================================================================
// 1. KHAI BÁO BIẾN CHO NÚT BẤM (MỚI)
// =================================================================================
bool buttonState = false;
bool lastButtonState = false;
unsigned long lastDebounceTime = 0;
const unsigned long debounceDelay = 50;

unsigned long lastClickTime = 0;
const unsigned long clickInterval = 500;      // Thời gian chờ click đúp
const unsigned long longPressDuration = 1000; // Thời gian nhấn giữ

bool longPressDetected = false;
bool doubleClickDetected = false;
bool clickDetected = false;

// =================================================================================
// CẤU TRÚC DỮ LIỆU CŨ & BIẾN TOÀN CỤC (GIỮ NGUYÊN)
// =================================================================================
struct HealthData
{
    int steps;
    int heartRate;
    int spo2;
};

SemaphoreHandle_t xDataMutex;
SemaphoreHandle_t xI2CMutex;
SemaphoreHandle_t xMaxIntSemaphore;

volatile int sharedStepCount = 0;
volatile int sharedHeartRate = 0;
volatile int sharedSpO2 = 0;
volatile bool wifiConnected = false;
volatile int batteryLevel = 85;

#define SERVICE_UUID "12345678-1234-1234-1234-1234567890ab"
#define CHARACTERISTIC_UUID "87654321-4321-4321-4321-ba0987654321"
#define DEVICE_NAME "ESP32S3_SmartWatch"

extern void initDisplay();
extern void loopDisplay();
extern void initSensors();
extern void updateUI(int steps, int hr, int spo2, bool isWifi, bool isBle, int batLevel);

NimBLEServer *pServer = nullptr;
NimBLECharacteristic *pCharacteristic = nullptr;
volatile bool deviceConnected = false;
volatile bool oldDeviceConnected = false;
volatile bool clientSubscribed = false;

// =================================================================================
// [MỚI] HÀM XỬ LÝ NÚT BẤM KHÔNG GÂY BLOCK (NON-BLOCKING)
// =================================================================================
void handlePowerButton()
{
    int reading = digitalRead(PIN_BUTTON_IN);

    // 1. Xử lý chống rung (Debounce)
    if (reading != lastButtonState)
    {
        lastDebounceTime = millis();
    }

    if ((millis() - lastDebounceTime) > debounceDelay)
    {
        // Nếu trạng thái nút thực sự thay đổi
        if (reading != buttonState)
        {
            buttonState = reading;

            // --- NÚT ĐƯỢC NHẤN (LOW) ---
            if (buttonState == LOW)
            {
                unsigned long now = millis();
                // Check Double Click
                if (now - lastClickTime < clickInterval && !longPressDetected)
                {
                    Serial.println(">> BUTTON: Double Click");
                    doubleClickDetected = true;
                    // Code xử lý Double Click ở đây (nếu cần)
                }
                else
                {
                    // Chuẩn bị check Single Click (chưa confirm ngay)
                    if (!longPressDetected && !doubleClickDetected)
                    {
                        // Chỉ đánh dấu, xử lý ở lúc thả nút
                    }
                }
                lastClickTime = now;
            }
            // --- NÚT ĐƯỢC THẢ (HIGH) ---
            else
            {
                if (longPressDetected)
                {
                    Serial.println(">> BUTTON: Long Press Released -> POWER OFF");

                    // Tắt Loa
                    noTone(PIN_BUZZER);

                    // QUAN TRỌNG: Ngắt nguồn giữ (Power Off)
                    digitalWrite(PIN_POWER_OUT, LOW);

                    longPressDetected = false;
                }
                else
                {
                    // Nếu không phải Long Press và không phải Double Click -> Single Click
                    if (!doubleClickDetected)
                    {
                        Serial.println(">> BUTTON: Single Click");
                        clickDetected = true;
                        // Code xử lý Single Click ở đây (VD: Đổi màn hình)
                    }
                }
                // Reset cờ
                clickDetected = false;
                doubleClickDetected = false;
            }
        }
    }

    // 2. Kiểm tra Nhấn Giữ (Long Press) liên tục
    if (buttonState == LOW && (millis() - lastDebounceTime >= longPressDuration) && !longPressDetected)
    {
        Serial.println(">> BUTTON: Long Press Detected...");

        // Kêu bíp báo hiệu sắp tắt nguồn
        tone(PIN_BUZZER, 2000);

        longPressDetected = true;
        clickDetected = false;
        doubleClickDetected = false;
    }

    lastButtonState = reading;
}

// =================================================================================
// CÁC HÀM HỖ TRỢ CŨ (GIỮ NGUYÊN)
// =================================================================================

void sendBleData(const HealthData &data)
{
    if (!deviceConnected || !clientSubscribed)
        return;
    uint8_t payload[6]; // <--- Khai báo mảng 6 Byte

    // Gán dữ liệu 1 Byte (đơn giản)
    payload[0] = (uint8_t)data.heartRate;
    payload[1] = (uint8_t)data.spo2;

    // Xử lý dữ liệu 4 Bytes (Steps) bằng kỹ thuật dịch bit (Bit Shifting)
    // Cắt nhỏ số to (Steps) thành 4 miếng nhỏ để nhét vừa vào từng Byte
    payload[2] = (uint8_t)(data.steps & 0xFF);
    payload[3] = (uint8_t)((data.steps >> 8) & 0xFF);
    payload[4] = (uint8_t)((data.steps >> 16) & 0xFF);
    payload[5] = (uint8_t)((data.steps >> 24) & 0xFF);

    // <--- Gửi đi đúng 6 Byte
    pCharacteristic->setValue(payload, 6);
    pCharacteristic->notify(); // Notify – “đẩy dữ liệu”
}

void recoverI2CBus() {}

class MyServerCallbacks : public NimBLEServerCallbacks
{
    void onConnect(NimBLEServer *pServer) override { deviceConnected = true; }; // Connect = vào phòng riêng nói chuyện
    void onDisconnect(NimBLEServer *pServer) override
    {
        deviceConnected = false;
        clientSubscribed = false;
    }
};

class MyCharacteristicCallbacks : public NimBLECharacteristicCallbacks
{
    void onSubscribe(NimBLECharacteristic *pChar, ble_gap_conn_desc *desc, uint16_t subValue) override
    {
        clientSubscribed = (subValue != 0);
    };
};

void TaskBLE(void *parameter)
{
    esp_task_wdt_add(NULL);
    TickType_t xLastWakeTime = xTaskGetTickCount();
    const TickType_t xFrequency = 1000 / portTICK_PERIOD_MS;
    HealthData localData;
    while (1)
    {
        vTaskDelayUntil(&xLastWakeTime, xFrequency);
        if (!deviceConnected && oldDeviceConnected)
        {
            vTaskDelay(500 / portTICK_PERIOD_MS);
            pServer->startAdvertising();
            oldDeviceConnected = deviceConnected;
        }
        if (deviceConnected && !oldDeviceConnected)
        {
            oldDeviceConnected = deviceConnected;
        }
        if (deviceConnected && clientSubscribed)
        {
            if (xSemaphoreTake(xDataMutex, (TickType_t)10) == pdTRUE)
            {
                localData.steps = sharedStepCount;
                localData.heartRate = sharedHeartRate;
                localData.spo2 = sharedSpO2;
                xSemaphoreGive(xDataMutex);
                sendBleData(localData);
            }
        }
        esp_task_wdt_reset();
    }
}

// =================================================================================
// SETUP & LOOP (CẬP NHẬT)
// =================================================================================
void setup()
{
    // Khởi tạo chân Nguồn ngay lập tức để giữ mạch chạy
    pinMode(PIN_POWER_OUT, OUTPUT);
    digitalWrite(PIN_POWER_OUT, HIGH); // Kích hoạt mạch nguồn

    pinMode(PIN_BUTTON_IN, INPUT);
    pinMode(PIN_BUZZER, OUTPUT);

    Serial.begin(115200);

    // Tăng WDT
    esp_task_wdt_init(20, true);
    esp_task_wdt_add(NULL);

    xDataMutex = xSemaphoreCreateMutex();
    xI2CMutex = xSemaphoreCreateMutex();
    xMaxIntSemaphore = xSemaphoreCreateBinary();

    initSensors();
    initDisplay();
    initWifiTime();

    // Init BLE
    NimBLEDevice::init(DEVICE_NAME);
    NimBLEDevice::setPower(ESP_PWR_LVL_P9);
    pServer = NimBLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());
    NimBLEService *pService = pServer->createService(SERVICE_UUID);
    pCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::NOTIFY);
    pCharacteristic->setCallbacks(new MyCharacteristicCallbacks());
    pService->start();
    NimBLEAdvertising *pAdvertising = NimBLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->start(); // kết nối mở của, treo biển

    xTaskCreatePinnedToCore(TaskBLE, "BLE_Task", 4096, NULL, 3, NULL, 0); // pinnedToCore để kiểm soát hành vi realtime của hệ thống
    Serial.println("System Ready!");
}

void loop()
{
    loopDisplay();

    // [MỚI] Gọi hàm xử lý nút bấm mỗi vòng lặp
    handlePowerButton();

    checkWifiStatus();

    static unsigned long lastUpdate = 0;
    if (millis() - lastUpdate > 1000)
    {
        HealthData uiData;
        if (xSemaphoreTake(xDataMutex, 0) == pdTRUE)
        {
            uiData.steps = sharedStepCount;
            uiData.heartRate = sharedHeartRate;
            uiData.spo2 = sharedSpO2;
            xSemaphoreGive(xDataMutex);
            updateUI(uiData.steps, uiData.heartRate, uiData.spo2,
                     wifiConnected, deviceConnected, batteryLevel);
        }
        lastUpdate = millis();
    }

    esp_task_wdt_reset();
    delay(5); // Delay nhỏ để nhường CPU
}