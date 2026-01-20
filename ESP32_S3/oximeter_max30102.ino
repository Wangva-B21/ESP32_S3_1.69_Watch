#include <lvgl.h>
#include <TFT_eSPI.h>  // Thư viện driver LCD, tùy board mà dùng TFT_eSPI

// ------ Chân control theo hướng dẫn ------ 
#define BUZZER_PIN 33  // GPIO33
// -----------------------------------------

TFT_eSPI tft = TFT_eSPI(); // Khởi tạo driver TFT
lv_disp_draw_buf_t draw_buf;
static lv_color_t buf[LV_HOR_RES_MAX * 10]; // buffer LVGL

void setup() {
  // Khởi động Serial
  Serial.begin(115200);

  // ----- Kéo xuống buzzer để giảm nhiệt -----
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW); // kéo xuống
  // -----------------------------------------

  // Khởi tạo TFT
  tft.begin();
  tft.setRotation(1); // tùy màn hình mà chỉnh

  // Khởi tạo LVGL
  lv_init();
  lv_disp_draw_buf_init(&draw_buf, buf, NULL, LV_HOR_RES_MAX * 10);

  static lv_disp_drv_t disp_drv;
  lv_disp_drv_init(&disp_drv);
  disp_drv.hor_res = 240; // độ phân giải màn hình
  disp_drv.ver_res = 240;
  disp_drv.flush_cb = my_disp_flush;
  disp_drv.draw_buf = &draw_buf;
  lv_disp_drv_register(&disp_drv);

  // ----- Tạo label hiển thị số 1 -----
  lv_obj_t *label = lv_label_create(lv_scr_act());
  lv_label_set_text(label, "1");
  lv_obj_align(label, LV_ALIGN_CENTER, 0, 0);
}

void loop() {
  lv_task_handler(); // Cập nhật LVGL
  delay(5);
}

// Hàm flush TFT cho LVGL
void my_disp_flush(lv_disp_drv_t *disp, const lv_area_t *area, lv_color_t *color_p) {
  tft.startWrite();
  tft.setAddrWindow(area->x1, area->y1, area->x2 - area->x1 + 1, area->y2 - area->y1 + 1);
  tft.pushColors(&color_p->full, (area->x2 - area->x1 + 1) * (area->y2 - area->y1 + 1), true);
  tft.endWrite();
  lv_disp_flush_ready(disp);
}
