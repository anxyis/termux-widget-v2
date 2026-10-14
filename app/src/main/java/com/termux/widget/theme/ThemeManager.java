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

    /**
     * Recursively resets any hardcoded typography font families so that all
     * TextViews, Buttons, and text widgets strictly honor the user's OS system font
     * (e.g. MiSans, OneUI font, or user-selected font in Android Display settings).
     */
    public static void applySystemFont(android.view.View view) {
        if (view == null) return;
        if (view instanceof android.widget.TextView) {
            android.widget.TextView tv = (android.widget.TextView) view;
            android.graphics.Typeface current = tv.getTypeface();
            int style = current != null ? current.getStyle() : android.graphics.Typeface.NORMAL;
            tv.setTypeface(android.graphics.Typeface.DEFAULT, style);
        } else if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applySystemFont(vg.getChildAt(i));
            }
        }
    }
}
