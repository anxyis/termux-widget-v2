package com.termux.widget.theme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.termux.widget.R;

public final class ThemeManager {

    private static final String PREFS_NAME = "termux_widget_theme_prefs";
    private static final String KEY_AMOLED_MODE = "key_amoled_pure_black";

    private ThemeManager() {}

    public static boolean isAmoledMode(Context context) {
        if (context == null) return false;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_AMOLED_MODE, false);
    }

    public static void setAmoledMode(Context context, boolean enabled) {
        if (context == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_AMOLED_MODE, enabled).apply();
    }

    public static void applyTheme(Activity activity) {
        if (activity == null) return;
        if (isAmoledMode(activity)) {
            activity.setTheme(R.style.Theme_TermuxWidget_Material3_Amoled);
            if (activity.getWindow() != null) {
                activity.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK));
                activity.getWindow().setStatusBarColor(android.graphics.Color.BLACK);
                activity.getWindow().setNavigationBarColor(android.graphics.Color.BLACK);
            }
        } else {
            activity.setTheme(R.style.Theme_TermuxWidget_Material3);
        }
    }
}
