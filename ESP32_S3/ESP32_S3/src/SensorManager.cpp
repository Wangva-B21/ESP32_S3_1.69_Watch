#include <Arduino.h>
#include <Wire.h>
#include <esp_task_wdt.h>
#include "SensorQMI8658.hpp"
#include "MAX30105.h"
#include "spo2_algorithm.h"
#include <math.h>
#include "GlobalConfig.h"

// ============================================================================
// 1. CẤU HÌNH & TRẠNG THÁI
// ============================================================================
#define DEBUG_SENSORS 1

#if DEBUG_SENSORS
#define LOG_SENSOR(x) Serial.println("[SENSOR] " + String(x))
#else
#define LOG_SENSOR(x)
#endif

enum SensorState
{
    STATE_NO_FINGER,
    STATE_WARMING_UP,
    STATE_MEASURING
};

// ============================================================================
// 2. BIẾN TOÀN CỤC
// ============================================================================
extern SemaphoreHandle_t xDataMutex;
extern SemaphoreHandle_t xI2CMutex;
extern SemaphoreHandle_t xMaxIntSemaphore;

extern volatile int sharedStepCount;
extern volatile int sharedHeartRate;
extern volatile int sharedSpO2;

SensorQMI8658 qmi;
MAX30105 particleSensor;
IMUdata acc;

// --- BIẾN BƯỚC CHÂN ---
float lastMag = 0.0f;
float filteredMag = 0.0f;
const float ALPHA = 0.2f;
const float STEP_THRESHOLD = 1.05f;
bool canStep = true;
unsigned long lastStepTime = 0;

// --- BIẾN TIM MẠCH ---
const int32_t bufferLength = 100;
uint32_t irBuffer[bufferLength];
uint32_t redBuffer[bufferLength];
int32_t spo2;
int8_t validSPO2;
int32_t heartRate;
int8_t validHeartRate;

// --- BỘ LỌC & STATE ---
SensorState currentState = STATE_NO_FINGER;
float avgHeartRate = 0.0f;
int lastGoodHeartRate = 0;
int glitchCounter = 0;
unsigned long warmupStartTime = 0;
int warmupIndex = 0;
float warmupBuffer[10];
int invalidFrameCount = 0;

// --- CẤU HÌNH THAM SỐ ---
const int MAX_JUMP = 5; // Trung vị (Median Filter) -> nếu nhịp tim thay đổi quá nhanh trong 1 giây thì -> nhiễu và giữ lại giá trị cũ
const int GLITCH_LIMIT = 2;
const float HR_ALPHA = 0.08f;
const float STABLE_STDDEV_THRESHOLD = 2.5f;
const unsigned long MIN_WARMUP_TIME = 15000;
const unsigned long MAX_WARMUP_TIME = 20000;
const int MAX_INVALID_FRAMES = 15; // Tăng lên 15 để code kiên nhẫn hơn chút
const long IR_THRESHOLD = 60000;

// --- BIẾN BỘ LỌC MEDIAN ---
const int MEDIAN_SIZE = 5;
int medianBuffer[MEDIAN_SIZE];
int medianIndex = 0;

// ============================================================================
// 3. CÁC HÀM XỬ LÝ
// ============================================================================

void resetFilterState()
{
    if (currentState != STATE_NO_FINGER)
    {
        LOG_SENSOR("State Changed -> NO_FINGER (Reset)");
    }
    sharedHeartRate = 0;
    sharedSpO2 = 0;
    avgHeartRate = 0.0f;
    lastGoodHeartRate = 0;
    glitchCounter = 0;
    warmupStartTime = 0;
    warmupIndex = 0;
    invalidFrameCount = 0;

    for (int i = 0; i < 10; i++)
        warmupBuffer[i] = 0.0f;
    for (int i = 0; i < MEDIAN_SIZE; i++)
        medianBuffer[i] = 0;

    currentState = STATE_NO_FINGER;
}

bool checkFingerPresent()
{
    long avgRed = 0;
    for (int i = 0; i < bufferLength; i++)
        avgRed += redBuffer[i];
    avgRed /= bufferLength;
    if (avgRed < IR_THRESHOLD)
        return false;
    return true;
}

// Hàm lọc trung vị (Median Filter)
int getMedian(int newVal)
{
    medianBuffer[medianIndex] = newVal;
    medianIndex = (medianIndex + 1) % MEDIAN_SIZE;

    int sorted[MEDIAN_SIZE];
    for (int i = 0; i < MEDIAN_SIZE; i++)
        sorted[i] = medianBuffer[i];

    for (int i = 0; i < MEDIAN_SIZE - 1; i++)
    {
        for (int j = i + 1; j < MEDIAN_SIZE; j++)
        {
            if (sorted[i] > sorted[j])
            {
                int temp = sorted[i];
                sorted[i] = sorted[j];
                sorted[j] = temp;
            }
        }
    }
    return sorted[MEDIAN_SIZE / 2];
}

void processHeartRateLogic(float rawHR)
{
    int stableRaw = getMedian((int)rawHR);
    if (stableRaw == 0)
        stableRaw = rawHR;

    float filteredHR = stableRaw;

    if (lastGoodHeartRate == 0)
    {
        lastGoodHeartRate = stableRaw;
        avgHeartRate = stableRaw;
        filteredHR = stableRaw;
    }
    else
    {
        int diff = abs(stableRaw - lastGoodHeartRate);
        if (diff > MAX_JUMP)
        {
            glitchCounter++;
            if (glitchCounter > GLITCH_LIMIT)
            {
                // LOG_SENSOR("New HR Trend: " + String(stableRaw));
                lastGoodHeartRate = stableRaw;
                avgHeartRate = (avgHeartRate * 0.7) + (stableRaw * 0.3);
                glitchCounter = 0;
                filteredHR = avgHeartRate;
            }
            else
            {
                filteredHR = avgHeartRate;
            }
        }
        else
        {
            glitchCounter = 0;
            lastGoodHeartRate = stableRaw;
            avgHeartRate = (HR_ALPHA * stableRaw) + ((1.0f - HR_ALPHA) * avgHeartRate);
            filteredHR = avgHeartRate;
        }
    }

    if (currentState == STATE_NO_FINGER)
    {
        currentState = STATE_WARMING_UP;
        warmupStartTime = millis();
        for (int i = 0; i < MEDIAN_SIZE; i++)
            medianBuffer[i] = stableRaw;
        LOG_SENSOR("State Changed -> WARMING_UP");
    }

    if (currentState == STATE_WARMING_UP)
    {
        warmupBuffer[warmupIndex] = filteredHR;
        warmupIndex = (warmupIndex + 1) % 10;

        float sum = 0, mean = 0, variance = 0, stdDev = 0;
        for (int i = 0; i < 10; i++)
            sum += warmupBuffer[i];
        mean = sum / 10.0f;
        for (int i = 0; i < 10; i++)
            variance += pow(warmupBuffer[i] - mean, 2);
        stdDev = sqrt(variance / 10.0f);

        unsigned long elapsed = millis() - warmupStartTime;
        if ((elapsed > MIN_WARMUP_TIME && stdDev < STABLE_STDDEV_THRESHOLD) || (elapsed > MAX_WARMUP_TIME))
        {
            currentState = STATE_MEASURING;
            LOG_SENSOR("State Changed -> MEASURING");
        }
    }

    if (currentState == STATE_WARMING_UP)
    {
        sharedHeartRate = 0;
        sharedSpO2 = 0;
    }
    else if (currentState == STATE_MEASURING)
    {
        sharedHeartRate = (int)(filteredHR + 0.5f);
        if (validSPO2 == 1 && spo2 > 70 && spo2 <= 100)
        {
            sharedSpO2 = spo2;
        }
    }
}

// ============================================================================
// 4. ISR & TASKS
// ============================================================================
void IRAM_ATTR onMax30102Interrupt()
{
    BaseType_t xHigherPriorityTaskWoken = pdFALSE;
    xSemaphoreGiveFromISR(xMaxIntSemaphore, &xHigherPriorityTaskWoken);
    if (xHigherPriorityTaskWoken)
        portYIELD_FROM_ISR();
}

void TaskHealthRead(void *parameter)
{
    esp_task_wdt_add(NULL);
    int bufferIndex = 0;

    while (1)
    {
        esp_task_wdt_reset(); // Reset WDT ở vòng lặp ngoài

        if (xSemaphoreTake(xMaxIntSemaphore, (TickType_t)1000) == pdTRUE)
        {
            if (xSemaphoreTake(xI2CMutex, (TickType_t)10) == pdTRUE)
            {
                particleSensor.check();
                while (particleSensor.available())
                {

                    esp_task_wdt_reset(); // <--- [FIX QUAN TRỌNG] Reset WDT ở đây để tránh crash khi loop quá lâu

                    // --- ĐỌC DỮ LIỆU ---
                    if (bufferIndex < bufferLength)
                    {
                        redBuffer[bufferIndex] = particleSensor.getFIFORed();
                        irBuffer[bufferIndex] = particleSensor.getFIFOIR();
                        particleSensor.nextSample();
                        bufferIndex++;
                    }
                    else
                    {
                        // --- XỬ LÝ BUFFER ĐẦY ---
                        if (!checkFingerPresent())
                        {
                            if (xSemaphoreTake(xDataMutex, (TickType_t)10) == pdTRUE)
                            {
                                resetFilterState();
                                xSemaphoreGive(xDataMutex);
                            }
                        }
                        else
                        { /*Thuật toán Peak Detection của Maxim để phát hiện đỉnh tín hiệu PPG
                            -> kiểm tra tính hợp lệ của nhịp tim theo ngưỡng sinh lý
                            -> dùng median filter kết hợp bộ lọc thông thấp theo thời gian để loại nhiễu và tránh nhảy giá trị*/
                            maxim_heart_rate_and_oxygen_saturation(irBuffer, bufferLength, redBuffer, &spo2, &validSPO2, &heartRate, &validHeartRate);

                            if (xSemaphoreTake(xDataMutex, (TickType_t)10) == pdTRUE)
                            {
                                if (validHeartRate == 1 && heartRate > 40 && heartRate < 200)
                                {
                                    invalidFrameCount = 0;
                                    processHeartRateLogic((float)heartRate);
                                }
                                else
                                {
                                    invalidFrameCount++;
                                    // Chỉ reset khi lỗi liên tiếp quá nhiều
                                    if (invalidFrameCount > MAX_INVALID_FRAMES)
                                    {
                                        // LOG_SENSOR("Too many invalid frames -> Reset");
                                        resetFilterState();
                                    }
                                }
                                xSemaphoreGive(xDataMutex);
                            }
                        }

                        // Sliding window
                        for (byte i = 0; i < bufferLength - 25; i++)
                        {
                            redBuffer[i] = redBuffer[i + 25];
                            irBuffer[i] = irBuffer[i + 25];
                        }
                        bufferIndex = bufferLength - 25;
                        redBuffer[bufferIndex] = particleSensor.getFIFORed();
                        irBuffer[bufferIndex] = particleSensor.getFIFOIR();
                        particleSensor.nextSample();
                        bufferIndex++;
                    }
                }
                xSemaphoreGive(xI2CMutex);
            }
        }
        else
        {
            vTaskDelay(10);
        }
    }
}

void TaskStepRead(void *parameter)
{
    esp_task_wdt_add(NULL);
    while (1)
    {
        esp_task_wdt_reset();
        bool hasData = false;

        if (xSemaphoreTake(xI2CMutex, (TickType_t)5) == pdTRUE)
        {
            if (qmi.getDataReady())
            {
                if (qmi.getAccelerometer(acc.x, acc.y, acc.z))
                    hasData = true;
            }
            xSemaphoreGive(xI2CMutex);
        }

        if (hasData)
        { // phát hiện đỉnh (Peak Detection) kết hợp với ngưỡng (Threshold) -> bộ lọc thông thấp (Low Pass Filter)
            float mag = sqrt(acc.x * acc.x + acc.y * acc.y + acc.z * acc.z);
            filteredMag = ALPHA * mag + (1.0 - ALPHA) * filteredMag;

            if (lastMag < STEP_THRESHOLD && filteredMag >= STEP_THRESHOLD && canStep)
            {
                if (xSemaphoreTake(xDataMutex, (TickType_t)10) == pdTRUE)
                {
                    sharedStepCount++;
                    xSemaphoreGive(xDataMutex);
                }
                canStep = false;
                lastStepTime = millis();
            }

            if (filteredMag < STEP_THRESHOLD * 0.9f)
                canStep = true;
            if (!canStep && millis() - lastStepTime > 250)
                canStep = true;
            lastMag = filteredMag;
        }
        vTaskDelay(20 / portTICK_PERIOD_MS);
    }
}

void initSensors()
{
    Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);

    if (!qmi.begin(Wire, QMI8658_L_SLAVE_ADDRESS, I2C_SDA_PIN, I2C_SCL_PIN))
    {
        LOG_SENSOR("QMI8658 Fail!");
    }
    else
    {
        qmi.configAccelerometer(SensorQMI8658::ACC_RANGE_4G, SensorQMI8658::ACC_ODR_1000Hz, SensorQMI8658::LPF_MODE_0);
        qmi.enableAccelerometer();
        LOG_SENSOR("QMI8658 OK");
    }

    pinMode(MAX30102_INT_PIN, INPUT_PULLUP);
    if (!particleSensor.begin(Wire, I2C_SPEED_FAST))
    {
        LOG_SENSOR("MAX30102 Not Found!");
    }
    else
    {
        // [FIX QUAN TRỌNG] Quay về 8192 để tránh bão hòa tín hiệu (lỗi invalid frames)
        particleSensor.setup(100, 4, 2, 100, 411, 8192);
        particleSensor.writeRegister8(MAX30105_ADDRESS, 0x02, 0x40);
        LOG_SENSOR("MAX30102 Configured");
    }

    particleSensor.readRegister8(MAX30105_ADDRESS, 0x00);
    attachInterrupt(digitalPinToInterrupt(MAX30102_INT_PIN), onMax30102Interrupt, FALLING);

    xTaskCreatePinnedToCore(TaskHealthRead, "HealthTask", 8192, NULL, 2, NULL, 0);
    xTaskCreatePinnedToCore(TaskStepRead, "StepTask", 4096, NULL, 1, NULL, 0);
}