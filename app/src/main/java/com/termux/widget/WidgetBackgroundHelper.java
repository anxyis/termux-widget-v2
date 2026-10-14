package com.termux.widget;

import androidx.annotation.DrawableRes;

public final class WidgetBackgroundHelper {

    private WidgetBackgroundHelper() {}

    public static int getTier(int cornerRadiusDp) {
        if (cornerRadiusDp < 4) return 0;
        if (cornerRadiusDp < 12) return 8;
        if (cornerRadiusDp < 20) return 16;
        if (cornerRadiusDp < 28) return 24;
        return 32;
    }

    @DrawableRes
    public static int getButtonBackground(boolean isAmoled, boolean isToggleOn, int cornerRadiusDp) {
        int tier = getTier(cornerRadiusDp);
        if (isAmoled) {
            if (isToggleOn) {
                switch (tier) {
                    case 0: return R.drawable.widget_toggle_card_amoled_on_bg_r0;
                    case 8: return R.drawable.widget_toggle_card_amoled_on_bg_r8;
                    case 16: return R.drawable.widget_toggle_card_amoled_on_bg_r16;
                    case 32: return R.drawable.widget_toggle_card_amoled_on_bg_r32;
                    case 24:
                    default: return R.drawable.widget_toggle_card_amoled_on_bg;
                }
            } else {
                switch (tier) {
                    case 0: return R.drawable.widget_button_single_amoled_bg_r0;
                    case 8: return R.drawable.widget_button_single_amoled_bg_r8;
                    case 16: return R.drawable.widget_button_single_amoled_bg_r16;
                    case 32: return R.drawable.widget_button_single_amoled_bg_r32;
                    case 24:
                    default: return R.drawable.widget_button_single_amoled_bg;
                }
            }
        } else {
            if (isToggleOn) {
                switch (tier) {
                    case 0: return R.drawable.widget_toggle_card_on_bg_r0;
                    case 8: return R.drawable.widget_toggle_card_on_bg_r8;
                    case 16: return R.drawable.widget_toggle_card_on_bg_r16;
                    case 32: return R.drawable.widget_toggle_card_on_bg_r32;
                    case 24:
                    default: return R.drawable.widget_toggle_card_on_bg;
                }
            } else {
                switch (tier) {
                    case 0: return R.drawable.widget_button_single_bg_r0;
                    case 8: return R.drawable.widget_button_single_bg_r8;
                    case 16: return R.drawable.widget_button_single_bg_r16;
                    case 32: return R.drawable.widget_button_single_bg_r32;
                    case 24:
                    default: return R.drawable.widget_button_single_bg;
                }
            }
        }
    }

    @DrawableRes
    public static int getListCardBackground(boolean isAmoled, boolean isToggleOn, int cornerRadiusDp) {
        int tier = getTier(cornerRadiusDp);
        if (isAmoled) {
            if (isToggleOn) {
                switch (tier) {
                    case 0: return R.drawable.widget_toggle_card_amoled_on_bg_r0;
                    case 8: return R.drawable.widget_toggle_card_amoled_on_bg_r8;
                    case 16: return R.drawable.widget_toggle_card_amoled_on_bg_r16;
                    case 32: return R.drawable.widget_toggle_card_amoled_on_bg_r32;
                    case 24:
                    default: return R.drawable.widget_toggle_card_amoled_on_bg;
                }
            } else {
                switch (tier) {
                    case 0: return R.drawable.widget_list_card_amoled_bg_r0;
                    case 8: return R.drawable.widget_list_card_amoled_bg_r8;
                    case 16: return R.drawable.widget_list_card_amoled_bg_r16;
                    case 32: return R.drawable.widget_list_card_amoled_bg_r32;
                    case 24:
                    default: return R.drawable.widget_list_card_amoled_bg;
                }
            }
        } else {
            if (isToggleOn) {
                switch (tier) {
                    case 0: return R.drawable.widget_toggle_card_on_bg_r0;
                    case 8: return R.drawable.widget_toggle_card_on_bg_r8;
                    case 16: return R.drawable.widget_toggle_card_on_bg_r16;
                    case 32: return R.drawable.widget_toggle_card_on_bg_r32;
                    case 24:
                    default: return R.drawable.widget_toggle_card_on_bg;
                }
            } else {
                switch (tier) {
                    case 0: return R.drawable.widget_list_card_bg_r0;
                    case 8: return R.drawable.widget_list_card_bg_r8;
                    case 16: return R.drawable.widget_list_card_bg_r16;
                    case 32: return R.drawable.widget_list_card_bg_r32;
                    case 24:
                    default: return R.drawable.widget_list_card_bg;
                }
            }
        }
    }

    @DrawableRes
    public static int getGridCardBackground(boolean isAmoled, boolean isToggleOn, int cornerRadiusDp) {
        int tier = getTier(cornerRadiusDp);
        if (isAmoled) {
            if (isToggleOn) {
                switch (tier) {
                    case 0: return R.drawable.widget_toggle_card_amoled_on_bg_r0;
                    case 8: return R.drawable.widget_toggle_card_amoled_on_bg_r8;
                    case 16: return R.drawable.widget_toggle_card_amoled_on_bg_r16;
                    case 32: return R.drawable.widget_toggle_card_amoled_on_bg_r32;
                    case 24:
                    default: return R.drawable.widget_toggle_grid_card_amoled_on_bg;
                }
            } else {
                switch (tier) {
                    case 0: return R.drawable.widget_grid_card_amoled_bg_r0;
                    case 8: return R.drawable.widget_grid_card_amoled_bg_r8;
                    case 16: return R.drawable.widget_grid_card_amoled_bg_r16;
                    case 32: return R.drawable.widget_grid_card_amoled_bg_r32;
                    case 24:
                    default: return R.drawable.widget_grid_card_amoled_bg;
                }
            }
        } else {
            if (isToggleOn) {
                switch (tier) {
                    case 0: return R.drawable.widget_toggle_card_on_bg_r0;
                    case 8: return R.drawable.widget_toggle_card_on_bg_r8;
                    case 16: return R.drawable.widget_toggle_card_on_bg_r16;
                    case 32: return R.drawable.widget_toggle_card_on_bg_r32;
                    case 24:
                    default: return R.drawable.widget_toggle_grid_card_on_bg;
                }
            } else {
                switch (tier) {
                    case 0: return R.drawable.widget_grid_card_bg_r0;
                    case 8: return R.drawable.widget_grid_card_bg_r8;
                    case 16: return R.drawable.widget_grid_card_bg_r16;
                    case 32: return R.drawable.widget_grid_card_bg_r32;
                    case 24:
                    default: return R.drawable.widget_grid_card_bg;
                }
            }
        }
    }
}
