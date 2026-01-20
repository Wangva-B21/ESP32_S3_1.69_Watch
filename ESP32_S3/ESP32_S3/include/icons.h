#ifndef ICONS_H
#define ICONS_H

#include <lvgl.h>
#include <Arduino.h>

// --- ĐỊNH NGHĨA CẤU TRÚC ICON ---
typedef struct {
  lv_obj_t *obj;
  lv_img_dsc_t dsc;
  const uint8_t *data;
  uint16_t w, h;
} Icon;

// --- KHAI BÁO BIẾN ICON (EXTERN) ---
extern Icon icon_wifi_connected;
extern Icon icon_no_wifi;
extern Icon icon_bluetooth_connected;
extern Icon icon_bluetooth_no_connected; // Icon mới
extern Icon icon_battery;
extern Icon icon_heart;
extern Icon icon_Sp02;
extern Icon icon_steps;

// --- KHAI BÁO LABEL (EXTERN) ---
extern lv_obj_t *label_steps;
extern lv_obj_t *label_hr; 
extern lv_obj_t *label_spo2;

extern lv_obj_t *label_wifi_status;
extern lv_obj_t *label_bt_status;   
extern lv_obj_t *label_bat_status;  

// --- KHAI BÁO HÀM ---
void create_all_icons();
void update_status_labels(bool wifi_on, bool bt_on, int battery_level);

#endif