package com.termux.widget;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class TermuxButtonWidgetPreferences {

    private static final String PREF_NAME = "termux_button_widgets";
    private static final String KEY_PATH_PREFIX = "button_path_";
    private static final String KEY_LABEL_PREFIX = "button_label_";
    private static final String KEY_ICON_PREFIX = "button_icon_";

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveButtonWidgetConfig(@NonNull Context context, int appWidgetId,
                                              @NonNull String scriptPath,
                                              @NonNull String label,
                                              @Nullable String iconId) {
        getPrefs(context).edit()
                .putString(KEY_PATH_PREFIX + appWidgetId, scriptPath)
                .putString(KEY_LABEL_PREFIX + appWidgetId, label)
                .putString(KEY_ICON_PREFIX + appWidgetId, iconId)
                .apply();
    }

    @Nullable
    public static String getScriptPath(@NonNull Context context, int appWidgetId) {
        return getPrefs(context).getString(KEY_PATH_PREFIX + appWidgetId, null);
    }

    @Nullable
    public static String getLabel(@NonNull Context context, int appWidgetId) {
        return getPrefs(context).getString(KEY_LABEL_PREFIX + appWidgetId, null);
    }

    @Nullable
    public static String getIconId(@NonNull Context context, int appWidgetId) {
        return getPrefs(context).getString(KEY_ICON_PREFIX + appWidgetId, null);
    }

    public static void removeButtonWidgetConfig(@NonNull Context context, int appWidgetId) {
        getPrefs(context).edit()
                .remove(KEY_PATH_PREFIX + appWidgetId)
                .remove(KEY_LABEL_PREFIX + appWidgetId)
                .remove(KEY_ICON_PREFIX + appWidgetId)
                .apply();
    }
}
