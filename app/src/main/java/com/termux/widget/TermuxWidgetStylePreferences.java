package com.termux.widget;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class TermuxWidgetStylePreferences {

    private static final String PREF_NAME = "termux_widget_style_preferences";

    public static final String KEY_CORNER_RADIUS_DP = "widget_corner_radius_dp";
    public static final String KEY_TEXT_SIZE_SP = "widget_text_size_sp";
    public static final String KEY_ICON_SIZE_DP = "widget_icon_size_dp";

    public static final int DEFAULT_CORNER_RADIUS_DP = 24;
    public static final int DEFAULT_TEXT_SIZE_SP = 13;
    public static final int DEFAULT_ICON_SIZE_DP = 24;

    private TermuxWidgetStylePreferences() {}

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static int getCornerRadiusDp(@NonNull Context context) {
        return getPrefs(context).getInt(KEY_CORNER_RADIUS_DP, DEFAULT_CORNER_RADIUS_DP);
    }

    public static void setCornerRadiusDp(@NonNull Context context, int cornerRadiusDp) {
        getPrefs(context).edit().putInt(KEY_CORNER_RADIUS_DP, cornerRadiusDp).apply();
    }

    public static int getTextSizeSp(@NonNull Context context) {
        return getPrefs(context).getInt(KEY_TEXT_SIZE_SP, DEFAULT_TEXT_SIZE_SP);
    }

    public static void setTextSizeSp(@NonNull Context context, int textSizeSp) {
        getPrefs(context).edit().putInt(KEY_TEXT_SIZE_SP, textSizeSp).apply();
    }

    public static int getIconSizeDp(@NonNull Context context) {
        return getPrefs(context).getInt(KEY_ICON_SIZE_DP, DEFAULT_ICON_SIZE_DP);
    }

    public static void setIconSizeDp(@NonNull Context context, int iconSizeDp) {
        getPrefs(context).edit().putInt(KEY_ICON_SIZE_DP, iconSizeDp).apply();
    }

    public static void resetToDefaults(@NonNull Context context) {
        getPrefs(context).edit()
                .putInt(KEY_CORNER_RADIUS_DP, DEFAULT_CORNER_RADIUS_DP)
                .putInt(KEY_TEXT_SIZE_SP, DEFAULT_TEXT_SIZE_SP)
                .putInt(KEY_ICON_SIZE_DP, DEFAULT_ICON_SIZE_DP)
                .apply();
    }
}
