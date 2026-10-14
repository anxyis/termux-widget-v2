package com.termux.widget;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class TermuxWidgetStylePreferences {

    private static final String PREF_NAME = "termux_widget_style_preferences";

    public static final String KEY_CORNER_RADIUS_DP = "widget_corner_radius_dp";
    public static final String KEY_TEXT_SIZE_SP = "widget_text_size_sp";
    public static final String KEY_ICON_SIZE_DP = "widget_icon_size_dp";
    public static final String KEY_GRID_SPACING_DP = "widget_grid_spacing_dp";
    public static final String KEY_BLUR_EFFECT = "widget_blur_effect";
    public static final String KEY_USE_CUSTOM_COLORS = "widget_use_custom_colors";
    public static final String KEY_CUSTOM_PRIMARY_COLOR = "widget_custom_primary_color";
    public static final String KEY_CUSTOM_CONTAINER_COLOR = "widget_custom_container_color";
    public static final String KEY_CUSTOM_TEXT_COLOR = "widget_custom_text_color";

    public static final int DEFAULT_CORNER_RADIUS_DP = 24;
    public static final int DEFAULT_TEXT_SIZE_SP = 13;
    public static final int DEFAULT_ICON_SIZE_DP = 24;
    public static final int DEFAULT_GRID_SPACING_DP = 14;
    public static final boolean DEFAULT_BLUR_EFFECT = false;
    public static final boolean DEFAULT_USE_CUSTOM_COLORS = false;
    public static final int DEFAULT_CUSTOM_PRIMARY_COLOR = 0xFF6366F1; // Electric Indigo
    public static final int DEFAULT_CUSTOM_CONTAINER_COLOR = 0xFF18181B; // Deep Zinc
    public static final int DEFAULT_CUSTOM_TEXT_COLOR = 0xFFFFFFFF; // Pure White

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

    public static int getGridSpacingDp(@NonNull Context context) {
        return getPrefs(context).getInt(KEY_GRID_SPACING_DP, DEFAULT_GRID_SPACING_DP);
    }

    public static void setGridSpacingDp(@NonNull Context context, int gridSpacingDp) {
        getPrefs(context).edit().putInt(KEY_GRID_SPACING_DP, gridSpacingDp).apply();
    }

    public static boolean isBlurEnabled(@NonNull Context context) {
        return getPrefs(context).getBoolean(KEY_BLUR_EFFECT, DEFAULT_BLUR_EFFECT);
    }

    public static void setBlurEnabled(@NonNull Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_BLUR_EFFECT, enabled).apply();
    }

    public static boolean isCustomColorsEnabled(@NonNull Context context) {
        return getPrefs(context).getBoolean(KEY_USE_CUSTOM_COLORS, DEFAULT_USE_CUSTOM_COLORS);
    }

    public static void setCustomColorsEnabled(@NonNull Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_USE_CUSTOM_COLORS, enabled).apply();
    }

    public static int getCustomPrimaryColor(@NonNull Context context) {
        return getPrefs(context).getInt(KEY_CUSTOM_PRIMARY_COLOR, DEFAULT_CUSTOM_PRIMARY_COLOR);
    }

    public static void setCustomPrimaryColor(@NonNull Context context, int color) {
        getPrefs(context).edit().putInt(KEY_CUSTOM_PRIMARY_COLOR, color).apply();
    }

    public static int getCustomContainerColor(@NonNull Context context) {
        return getPrefs(context).getInt(KEY_CUSTOM_CONTAINER_COLOR, DEFAULT_CUSTOM_CONTAINER_COLOR);
    }

    public static void setCustomContainerColor(@NonNull Context context, int color) {
        getPrefs(context).edit().putInt(KEY_CUSTOM_CONTAINER_COLOR, color).apply();
    }

    public static int getCustomTextColor(@NonNull Context context) {
        return getPrefs(context).getInt(KEY_CUSTOM_TEXT_COLOR, DEFAULT_CUSTOM_TEXT_COLOR);
    }

    public static void setCustomTextColor(@NonNull Context context, int color) {
        getPrefs(context).edit().putInt(KEY_CUSTOM_TEXT_COLOR, color).apply();
    }

    public static void resetToDefaults(@NonNull Context context) {
        getPrefs(context).edit()
                .putInt(KEY_CORNER_RADIUS_DP, DEFAULT_CORNER_RADIUS_DP)
                .putInt(KEY_TEXT_SIZE_SP, DEFAULT_TEXT_SIZE_SP)
                .putInt(KEY_ICON_SIZE_DP, DEFAULT_ICON_SIZE_DP)
                .putInt(KEY_GRID_SPACING_DP, DEFAULT_GRID_SPACING_DP)
                .putBoolean(KEY_BLUR_EFFECT, DEFAULT_BLUR_EFFECT)
                .putBoolean(KEY_USE_CUSTOM_COLORS, DEFAULT_USE_CUSTOM_COLORS)
                .putInt(KEY_CUSTOM_PRIMARY_COLOR, DEFAULT_CUSTOM_PRIMARY_COLOR)
                .putInt(KEY_CUSTOM_CONTAINER_COLOR, DEFAULT_CUSTOM_CONTAINER_COLOR)
                .putInt(KEY_CUSTOM_TEXT_COLOR, DEFAULT_CUSTOM_TEXT_COLOR)
                .apply();
    }
}
