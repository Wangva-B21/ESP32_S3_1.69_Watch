#include "icons.h"
#include "GlobalConfig.h"

// ============================================================================
// 1. DỮ LIỆU BITMAP (HEX ARRAYS) - (Giữ nguyên dữ liệu cũ của bạn)
// ============================================================================

// --- WIFI DATA ---
static const uint8_t PROGMEM wifi_icon_connected[] = {
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x01, 0xff, 0x80, 0x07, 0xff, 0xe0, 0x1f, 0xef, 0xf8,
    0x3e, 0x00, 0x7c, 0x7c, 0x00, 0x3e, 0xf0, 0x00, 0x0f, 0x60, 0xff, 0x06,
    0x01, 0xff, 0x80, 0x07, 0xef, 0xc0, 0x07, 0x81, 0xe0, 0x07, 0x00, 0x60,
    0x02, 0x00, 0x00, 0x00, 0x3c, 0x00, 0x00, 0x7e, 0x00, 0x00, 0x7e, 0x00,
    0x00, 0x3c, 0x00, 0x00, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

static const uint8_t PROGMEM wifi_icon_no_wifi[] = {
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x60,
    0x00, 0x7e, 0xc0, 0x03, 0xff, 0x80, 0x0f, 0x83, 0xb0, 0x1c, 0x03, 0x38,
    0x38, 0x1e, 0x1c, 0x30, 0xfe, 0x0c, 0x03, 0xed, 0xc0, 0x07, 0x18, 0xe0,
    0x06, 0x18, 0x60, 0x00, 0x76, 0x00, 0x00, 0xef, 0x00, 0x00, 0xe3, 0x00,
    0x00, 0xc0, 0x00, 0x01, 0x98, 0x00, 0x01, 0x98, 0x00, 0x03, 0x18, 0x00,
    0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

// --- BLUETOOTH DATA ---
static const uint8_t PROGMEM bt_icon_data[] = {
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x30,
    0x00,
    0x00,
    0x3c,
    0x00,
    0x00,
    0x3f,
    0x00,
    0x03,
    0x33,
    0x80,
    0x03,
    0xb1,
    0x80,
    0x01,
    0xf3,
    0x80,
    0x00,
    0x7f,
    0x00,
    0x00,
    0x3c,
    0x00,
    0x00,
    0x3c,
    0x00,
    0x00,
    0x7f,
    0x00,
    0x01,
    0xf3,
    0x80,
    0x03,
    0xb1,
    0x80,
    0x03,
    0x33,
    0x80,
    0x00,
    0x3f,
    0x00,
    0x00,
    0x3c,
    0x00,
    0x00,
    0x30,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
};

static const uint8_t PROGMEM bt_icon_data_no_connect[] = {
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x30,
    0x00,
    0x00,
    0x3c,
    0x00,
    0x00,
    0x3f,
    0x00,
    0x03,
    0x33,
    0x80,
    0x03,
    0xb1,
    0x80,
    0x01,
    0xf3,
    0x80,
    0x00,
    0x7f,
    0x00,
    0x00,
    0x3c,
    0x00,
    0x00,
    0x3c,
    0x00,
    0x00,
    0x7f,
    0x00,
    0x01,
    0xf3,
    0x80,
    0x03,
    0xb1,
    0x80,
    0x03,
    0x33,
    0x80,
    0x00,
    0x3f,
    0x00,
    0x00,
    0x3c,
    0x00,
    0x00,
    0x30,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
};

// --- BATTERY DATA ---
static const uint8_t PROGMEM bat_icon_data_full[] = {
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x3f,
    0xff,
    0xf8,
    0xff,
    0xff,
    0xfc,
    0x80,
    0x00,
    0x02,
    0xbf,
    0xff,
    0xfa,
    0xbf,
    0xff,
    0xfa,
    0xbf,
    0xff,
    0xfb,
    0xbf,
    0xff,
    0xfb,
    0xbf,
    0xff,
    0xfb,
    0xbf,
    0xff,
    0xfb,
    0xbf,
    0xff,
    0xfa,
    0xbf,
    0xff,
    0xfa,
    0x80,
    0x00,
    0x02,
    0xff,
    0xff,
    0xfc,
    0x3f,
    0xff,
    0xf8,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
};

// --- HEALTH ICONS ---
static const uint8_t PROGMEM heart[] = {
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x03,
    0xf0,
    0x0f,
    0xc0,
    0x0f,
    0xf8,
    0x1f,
    0xf0,
    0x1f,
    0xfe,
    0x7f,
    0xf8,
    0x3f,
    0xfe,
    0x7f,
    0xfc,
    0x3f,
    0xff,
    0xff,
    0xfc,
    0x7f,
    0xff,
    0xff,
    0xfe,
    0x7f,
    0xff,
    0xff,
    0xfe,
    0x7f,
    0xfb,
    0xff,
    0xfe,
    0x7f,
    0xfb,
    0xff,
    0xfe,
    0x7f,
    0xf9,
    0xff,
    0xfe,
    0x7f,
    0xf8,
    0xff,
    0xfe,
    0x7f,
    0x70,
    0xff,
    0xfe,
    0x3e,
    0x32,
    0x78,
    0xfe,
    0x00,
    0x37,
    0x00,
    0xfc,
    0x00,
    0x87,
    0xf8,
    0xfc,
    0x1f,
    0x87,
    0xff,
    0xf8,
    0x0f,
    0xcf,
    0xff,
    0xf8,
    0x0f,
    0xcf,
    0xff,
    0xf0,
    0x07,
    0xef,
    0xff,
    0xe0,
    0x03,
    0xef,
    0xff,
    0xc0,
    0x01,
    0xff,
    0xff,
    0x80,
    0x00,
    0xff,
    0xff,
    0x00,
    0x00,
    0x3f,
    0xfe,
    0x00,
    0x00,
    0x1f,
    0xf8,
    0x00,
    0x00,
    0x07,
    0xe0,
    0x00,
    0x00,
    0x01,
    0x80,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
};

static const uint8_t PROGMEM Sp02[] = {
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x0c,
    0x00,
    0x00,
    0x00,
    0x1e,
    0x00,
    0x00,
    0x00,
    0x3f,
    0x00,
    0x00,
    0x00,
    0x71,
    0x80,
    0x00,
    0x00,
    0xe0,
    0xc0,
    0x00,
    0x00,
    0xc0,
    0x60,
    0x00,
    0x01,
    0x80,
    0x30,
    0x00,
    0x03,
    0x00,
    0x18,
    0x00,
    0x07,
    0x00,
    0x18,
    0x00,
    0x06,
    0x00,
    0x0c,
    0x00,
    0x0c,
    0x00,
    0x06,
    0x00,
    0x0c,
    0x00,
    0x06,
    0x00,
    0x18,
    0x00,
    0x06,
    0x00,
    0x18,
    0x00,
    0x00,
    0x00,
    0x18,
    0x00,
    0x00,
    0x00,
    0x18,
    0x00,
    0x00,
    0x00,
    0x18,
    0x01,
    0xf8,
    0x00,
    0x18,
    0x01,
    0x98,
    0x00,
    0x18,
    0x01,
    0x99,
    0xf0,
    0x0c,
    0x01,
    0x99,
    0xf8,
    0x0c,
    0x01,
    0x98,
    0x18,
    0x06,
    0x01,
    0x98,
    0x18,
    0x07,
    0x01,
    0xf9,
    0xf8,
    0x01,
    0xc0,
    0x01,
    0x00,
    0x00,
    0xf0,
    0x01,
    0x00,
    0x00,
    0x38,
    0x01,
    0xf8,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
};

static const uint8_t PROGMEM steps[] = {
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x07,
    0x00,
    0x00,
    0x00,
    0x08,
    0xc0,
    0x00,
    0x00,
    0x10,
    0x40,
    0x00,
    0x00,
    0x10,
    0x20,
    0x01,
    0xf0,
    0x20,
    0x20,
    0x02,
    0x18,
    0x20,
    0x20,
    0x04,
    0x08,
    0x20,
    0x20,
    0x04,
    0x08,
    0x10,
    0x20,
    0x04,
    0x04,
    0x10,
    0x20,
    0x04,
    0x04,
    0x10,
    0x40,
    0x04,
    0x08,
    0x10,
    0x40,
    0x04,
    0x08,
    0x10,
    0xc0,
    0x02,
    0x08,
    0x18,
    0x80,
    0x02,
    0x08,
    0x07,
    0x00,
    0x02,
    0x08,
    0x20,
    0x00,
    0x01,
    0x0c,
    0x7c,
    0x00,
    0x01,
    0xf0,
    0x42,
    0x00,
    0x00,
    0x00,
    0x42,
    0x00,
    0x00,
    0x1e,
    0x42,
    0x00,
    0x00,
    0x62,
    0x3c,
    0x00,
    0x00,
    0x42,
    0x00,
    0x00,
    0x00,
    0x42,
    0x00,
    0x00,
    0x00,
    0x26,
    0x00,
    0x00,
    0x00,
    0x18,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
    0x00,
};

// ============================================================================
// 2. BIẾN TOÀN CỤC
// ============================================================================
Icon icon_wifi_connected = {0};
Icon icon_no_wifi = {0};
Icon icon_heart = {0};
Icon icon_Sp02 = {0};
Icon icon_steps = {0};
Icon icon_bluetooth_connected = {0};
Icon icon_bluetooth_no_connected = {0};
Icon icon_battery = {0};

// Đối tượng Box chứa
lv_obj_t *ui_bottom_box = NULL;

// ============================================================================
// 3. HÀM TIỆN ÍCH
// ============================================================================
void init_icon(Icon *icon, const uint8_t *data, uint16_t w, uint16_t h, lv_color_t color)
{
  icon->data = data;
  icon->w = w;
  icon->h = h;
  icon->dsc.header.always_zero = 0;
  icon->dsc.header.w = w;
  icon->dsc.header.h = h;
  icon->dsc.header.cf = LV_IMG_CF_ALPHA_1BIT;
  icon->dsc.data = data;
  icon->dsc.data_size = w * h / 8;
  icon->obj = lv_img_create(lv_scr_act()); // Tạo icon trên màn hình chính, sau đó sẽ dùng set_pos đè lên box
  lv_img_set_src(icon->obj, &icon->dsc);
  lv_obj_set_style_img_recolor_opa(icon->obj, LV_OPA_COVER, 0);
  lv_obj_set_style_img_recolor(icon->obj, color, 0);
}

void create_icon_at(Icon *icon, int16_t x, int16_t y, lv_color_t color)
{
  init_icon(icon, icon->data, icon->w, icon->h, color);
  lv_obj_set_pos(icon->obj, x, y);
}

// MACRO CHUYỂN TỌA ĐỘ
#define Y_UP(Y_FROM_BOTTOM, OBJ_HEIGHT) (LCD_HEIGHT - (Y_FROM_BOTTOM) - (OBJ_HEIGHT))

// Hàm tạo Label được NÂNG CẤP: Cho phép chọn Font chữ
void create_label_below_icon(lv_obj_t **label_ptr, Icon *icon, int y_offset_from_bottom, const char *default_text, lv_color_t color, const lv_font_t *font)
{
  if (*label_ptr == NULL)
  {
    *label_ptr = lv_label_create(lv_scr_act());
  }
  lv_label_set_text(*label_ptr, default_text);
  lv_obj_set_style_text_color(*label_ptr, color, 0);

  // Sử dụng Font chữ được truyền vào
  lv_obj_set_style_text_font(*label_ptr, font, 0);

  lv_obj_update_layout(*label_ptr);
  int icon_x = lv_obj_get_x(icon->obj);
  int label_w = lv_obj_get_width(*label_ptr);
  int label_x = icon_x + (icon->w / 2) - (label_w / 2);
  int label_h = lv_obj_get_height(*label_ptr);
  lv_obj_set_pos(*label_ptr, label_x, Y_UP(y_offset_from_bottom, label_h));
}

// ============================================================================
// 4. HÀM CHÍNH: TẠO TOÀN BỘ GIAO DIỆN
// ============================================================================
void create_all_icons()
{
  const int CENTER_X = LCD_WIDTH / 2;
  const int COL_GAP = 75; // Khoảng cách cột (Rộng hơn chút cho thoáng)

  // --- 1. VẼ BOX CHỨA (HỘP TRÒN) ---
  ui_bottom_box = lv_obj_create(lv_scr_act());
  lv_obj_set_size(ui_bottom_box, 230, 135);                            // Rộng 230, Cao 135
  lv_obj_align(ui_bottom_box, LV_ALIGN_BOTTOM_MID, 0, -10);            // Căn dưới giữa, cách đáy 5px
  lv_obj_set_style_radius(ui_bottom_box, 20, 0);                       // Bo tròn 20px
  lv_obj_set_style_bg_color(ui_bottom_box, lv_color_hex(0x202020), 0); // Màu xám tối
  lv_obj_set_style_border_width(ui_bottom_box, 0, 0);                  // Không viền
  lv_obj_clear_flag(ui_bottom_box, LV_OBJ_FLAG_SCROLLABLE);            // Tắt cuộn

  // --- 2. TÍNH TOÁN TỌA ĐỘ TRONG BOX ---
  // Hàng Status (Wifi, BT, Pin) - Nằm phía trên của Box
  // Cách đáy màn hình khoảng 95px (Icon) và 75px (Text)
  const int ROW_STATUS_ICON_Y = 100;
  const int ROW_STATUS_TEXT_Y = 80;

  // Hàng Data (Tim, SpO2, Bước) - Nằm phía dưới của Box
  // Cách đáy màn hình khoảng 45px (Icon) và 15px (Text)
  const int ROW_DATA_ICON_Y = 40;
  const int ROW_DATA_TEXT_Y = 18;

  // Tọa độ X cho 3 cột
  int x_left = CENTER_X - COL_GAP - 12;
  int x_center = CENTER_X - 12;
  int x_right = CENTER_X + COL_GAP - 12;

  // =====================================================
  // HÀNG TRÊN: TRẠNG THÁI (Font 12)
  // =====================================================

  // -- Cột Trái: WIFI --
  icon_wifi_connected.data = wifi_icon_connected;
  icon_wifi_connected.w = 24;
  icon_wifi_connected.h = 24;
  create_icon_at(&icon_wifi_connected, x_left, Y_UP(ROW_STATUS_ICON_Y, 24), lv_color_hex(0x00FF00));
  lv_obj_add_flag(icon_wifi_connected.obj, LV_OBJ_FLAG_HIDDEN);

  icon_no_wifi.data = wifi_icon_no_wifi;
  icon_no_wifi.w = 24;
  icon_no_wifi.h = 24;
  create_icon_at(&icon_no_wifi, x_left, Y_UP(ROW_STATUS_ICON_Y, 24), lv_color_hex(0xFF0000));

  // Label Font 12
  create_label_below_icon(&label_wifi_status, &icon_no_wifi, ROW_STATUS_TEXT_Y, "No WiFi", lv_color_hex(0xFFFFFF), &lv_font_montserrat_12);

  // -- Cột Giữa: BLUETOOTH --
  icon_bluetooth_connected.data = bt_icon_data;
  icon_bluetooth_connected.w = 24;
  icon_bluetooth_connected.h = 24;
  create_icon_at(&icon_bluetooth_connected, x_center, Y_UP(ROW_STATUS_ICON_Y, 24), lv_color_hex(0x0000FF));
  lv_obj_add_flag(icon_bluetooth_connected.obj, LV_OBJ_FLAG_HIDDEN);

  icon_bluetooth_no_connected.data = bt_icon_data_no_connect;
  icon_bluetooth_no_connected.w = 24;
  icon_bluetooth_no_connected.h = 24;
  create_icon_at(&icon_bluetooth_no_connected, x_center, Y_UP(ROW_STATUS_ICON_Y, 24), lv_color_hex(0x808080));

  // Label Font 12
  create_label_below_icon(&label_bt_status, &icon_bluetooth_no_connected, ROW_STATUS_TEXT_Y, "No BT", lv_color_hex(0xFFFFFF), &lv_font_montserrat_12);

  // -- Cột Phải: PIN --
  icon_battery.data = bat_icon_data_full;
  icon_battery.w = 24;
  icon_battery.h = 24;
  create_icon_at(&icon_battery, x_right + 4, Y_UP(ROW_STATUS_ICON_Y, 24), lv_color_hex(0xFFFF00));

  // Label Font 12
  create_label_below_icon(&label_bat_status, &icon_battery, ROW_STATUS_TEXT_Y, "High", lv_color_hex(0xFFFFFF), &lv_font_montserrat_12);

  // =====================================================
  // HÀNG DƯỚI: DỮ LIỆU SỨC KHỎE (Font 18)
  // =====================================================

  // -- Cột Trái: TIM --
  icon_heart.data = heart;
  icon_heart.w = 32;
  icon_heart.h = 32;
  create_icon_at(&icon_heart, x_left - 4, Y_UP(ROW_DATA_ICON_Y, 32), lv_color_hex(0xFF0000));

  // Label Font 18
  create_label_below_icon(&label_hr, &icon_heart, ROW_DATA_TEXT_Y, "--", lv_color_hex(0xFFFFFF), &lv_font_montserrat_18);

  // -- Cột Giữa: SPO2 --
  icon_Sp02.data = Sp02;
  icon_Sp02.w = 32;
  icon_Sp02.h = 32;
  create_icon_at(&icon_Sp02, CENTER_X - 16, Y_UP(ROW_DATA_ICON_Y, 32), lv_color_hex(0x3399FF));

  // Label Font 18
  create_label_below_icon(&label_spo2, &icon_Sp02, ROW_DATA_TEXT_Y, "--%", lv_color_hex(0xFFFFFF), &lv_font_montserrat_18);

  // -- Cột Phải: BƯỚC --
  icon_steps.data = steps;
  icon_steps.w = 32;
  icon_steps.h = 32;
  create_icon_at(&icon_steps, CENTER_X + COL_GAP - 16, Y_UP(ROW_DATA_ICON_Y, 32), lv_color_hex(0xFF9900));

  // Label Font 18
  create_label_below_icon(&label_steps, &icon_steps, ROW_DATA_TEXT_Y, "0", lv_color_hex(0xFFFFFF), &lv_font_montserrat_18);
}

// === UPDATE STATUS LABELS ===
void update_status_labels(bool wifi_on, bool bt_on, int battery_level)
{
  // Tọa độ Y cho Text hàng Status (Phải khớp với bên trên)
  const int ROW_STATUS_TEXT_Y = 80;

  // 1. Cập nhật WiFi
  if (wifi_on)
  {
    lv_obj_add_flag(icon_no_wifi.obj, LV_OBJ_FLAG_HIDDEN);
    lv_obj_clear_flag(icon_wifi_connected.obj, LV_OBJ_FLAG_HIDDEN);
    lv_label_set_text(label_wifi_status, "WiFi On");
  }
  else
  {
    lv_obj_add_flag(icon_wifi_connected.obj, LV_OBJ_FLAG_HIDDEN);
    lv_obj_clear_flag(icon_no_wifi.obj, LV_OBJ_FLAG_HIDDEN);
    lv_label_set_text(label_wifi_status, "No WiFi");
  }

  // 2. Cập nhật Bluetooth
  if (bt_on)
  {
    lv_obj_add_flag(icon_bluetooth_no_connected.obj, LV_OBJ_FLAG_HIDDEN);
    lv_obj_clear_flag(icon_bluetooth_connected.obj, LV_OBJ_FLAG_HIDDEN);
    lv_label_set_text(label_bt_status, "Paired");
  }
  else
  {
    lv_obj_add_flag(icon_bluetooth_connected.obj, LV_OBJ_FLAG_HIDDEN);
    lv_obj_clear_flag(icon_bluetooth_no_connected.obj, LV_OBJ_FLAG_HIDDEN);
    lv_label_set_text(label_bt_status, "No BT");
  }

  // 3. Cập nhật Pin
  if (battery_level > 20)
  {
    lv_label_set_text(label_bat_status, "High");
    lv_obj_set_style_text_color(label_bat_status, lv_color_hex(0x00FF00), 0);
  }
  else
  {
    lv_label_set_text(label_bat_status, "Low");
    lv_obj_set_style_text_color(label_bat_status, lv_color_hex(0xFF0000), 0);
  }

  // Căn giữa lại các label (Vẫn dùng Font 12 cho Status)
  create_label_below_icon(&label_wifi_status, &icon_no_wifi, ROW_STATUS_TEXT_Y, lv_label_get_text(label_wifi_status), lv_color_hex(0xFFFFFF), &lv_font_montserrat_12);
  create_label_below_icon(&label_bt_status, &icon_bluetooth_no_connected, ROW_STATUS_TEXT_Y, lv_label_get_text(label_bt_status), lv_color_hex(0xFFFFFF), &lv_font_montserrat_12);
  create_label_below_icon(&label_bat_status, &icon_battery, ROW_STATUS_TEXT_Y, lv_label_get_text(label_bat_status), lv_color_hex(0xFFFFFF), &lv_font_montserrat_12);
}