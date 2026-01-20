#include <lvgl.h>
#include "Arduino_GFX_Library.h"
#include "GlobalConfig.h"
#include "icons.h"
#include "WifiTimeManager.h" // Đã tách file

// ============================================================================
// 1. KHỞI TẠO PHẦN CỨNG
// ============================================================================
Arduino_DataBus *bus = new Arduino_ESP32SPI(LCD_DC, LCD_CS, LCD_SCK, LCD_MOSI);
Arduino_GFX *gfx = new Arduino_ST7789(bus, LCD_RST, 0, true, LCD_WIDTH, LCD_HEIGHT, 0, 20, 0, 0);

// Khai báo Label
lv_obj_t *label_steps = NULL;
lv_obj_t *label_hr = NULL;
lv_obj_t *label_spo2 = NULL;
lv_obj_t *label_wifi_status = NULL;
lv_obj_t *label_bt_status = NULL;
lv_obj_t *label_bat_status = NULL;

// KHAI BÁO LABEL ĐỒNG HỒ MỚI
lv_obj_t *label_clock_time = NULL;
lv_obj_t *label_clock_date = NULL;

static lv_disp_draw_buf_t draw_buf;
static lv_color_t buf[LCD_WIDTH * LCD_HEIGHT / 10];

void my_disp_flush(lv_disp_drv_t *disp, const lv_area_t *area, lv_color_t *color_p)
{
    uint32_t w = (area->x2 - area->x1 + 1);
    uint32_t h = (area->y2 - area->y1 + 1);
    gfx->draw16bitRGBBitmap(area->x1, area->y1, (uint16_t *)&color_p->full, w, h);
    lv_disp_flush_ready(disp);
}

// --- HÀM TẠO UI ĐỒNG HỒ ---
void create_clock_ui()
{
    // 1. Giờ Phút (Font To, Mạnh mẽ)
    label_clock_time = lv_label_create(lv_scr_act());
    lv_label_set_text(label_clock_time, "00:00");

    // Dùng Font to (Nhớ bật trong lv_conf.h), nếu lỗi thì đổi về font 14
    lv_obj_set_style_text_font(label_clock_time, &lv_font_montserrat_34, 0);
    lv_obj_set_style_text_color(label_clock_time, lv_color_hex(0xFFFFFF), 0);
    // Căn giữa, cách đỉnh 35px (để nhường chỗ cho icon status bên dưới tí nữa)
    lv_obj_align(label_clock_time, LV_ALIGN_TOP_MID, 0, 35);

    // 2. Ngày Tháng (Nhỏ hơn nằm dưới giờ)
    label_clock_date = lv_label_create(lv_scr_act());
    lv_label_set_text(label_clock_date, "--/--");
    lv_obj_set_style_text_font(label_clock_date, &lv_font_montserrat_20, 0);
    lv_obj_set_style_text_color(label_clock_date, lv_color_hex(0xBBBBBB), 0); // Màu xám
    lv_obj_align_to(label_clock_date, label_clock_time, LV_ALIGN_OUT_BOTTOM_MID, 0, 2);
}

void initDisplay()
{
    Serial.println(">> Init Display Hardware...");
    gfx->begin();
    gfx->fillScreen(BLACK);

    Serial.println(">> Init LVGL...");
    lv_init();
    lv_disp_draw_buf_init(&draw_buf, buf, NULL, LCD_WIDTH * LCD_HEIGHT / 10);

    static lv_disp_drv_t disp_drv;
    lv_disp_drv_init(&disp_drv);
    disp_drv.hor_res = LCD_WIDTH;
    disp_drv.ver_res = LCD_HEIGHT;
    disp_drv.flush_cb = my_disp_flush;
    disp_drv.draw_buf = &draw_buf;
    lv_disp_drv_register(&disp_drv);

    const esp_timer_create_args_t t = {.callback = [](void *)
                                       { lv_tick_inc(2); }};
    esp_timer_handle_t timer;
    esp_timer_create(&t, &timer);
    esp_timer_start_periodic(timer, 2000);

    Serial.println(">> Building UI...");
    lv_obj_set_style_bg_color(lv_scr_act(), lv_color_hex(0x000000), 0);

    create_all_icons(); // Tạo các Icon (Tim, Pin, Box...)
    create_clock_ui();  // Tạo Đồng hồ

    Serial.println(">> Display Ready!");
}

void updateUI(int steps, int hr, int spo2, bool isWifiConnected, bool isBleConnected, int batteryLevel)
{
    char tmp_buf[16];

    // --- CẬP NHẬT GIỜ ---
    if (label_clock_time && label_clock_date)
    {
        char timeStr[10];
        char dateStr[16];
        get_time_string(timeStr, sizeof(timeStr));
        get_date_string(dateStr, sizeof(dateStr));
        lv_label_set_text(label_clock_time, timeStr);
        lv_label_set_text(label_clock_date, dateStr);
    }

    // --- CẬP NHẬT SENSOR ---
    if (label_steps)
    {
        sprintf(tmp_buf, "%d", steps);
        lv_label_set_text(label_steps, tmp_buf);
    }
    if (label_hr)
    {
        if (hr > 0)
            sprintf(tmp_buf, "%d", hr);
        else
            sprintf(tmp_buf, "--");
        lv_label_set_text(label_hr, tmp_buf);
    }
    if (label_spo2)
    {
        if (spo2 > 0)
            sprintf(tmp_buf, "%d%%", spo2);
        else
            sprintf(tmp_buf, "--");
        lv_label_set_text(label_spo2, tmp_buf);
    }

    // --- CẬP NHẬT STATUS ---
    update_status_labels(isWifiConnected, isBleConnected, batteryLevel);
}

void loopDisplay()
{
    lv_timer_handler();
}