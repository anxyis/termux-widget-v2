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

    private static final String KEY_PENDING_PIN_PATH = "pending_pin_path";
    private static final String KEY_PENDING_PIN_LABEL = "pending_pin_label";
    private static final String KEY_PENDING_PIN_ICON = "pending_pin_icon";
    private static final String KEY_PENDING_PIN_TIME = "pending_pin_time";

    public static void setPendingPin(@NonNull Context context, @NonNull String scriptPath,
                                     @Nullable String label, @Nullable String iconId) {
        getPrefs(context).edit()
                .putString(KEY_PENDING_PIN_PATH, scriptPath)
                .putString(KEY_PENDING_PIN_LABEL, label)
                .putString(KEY_PENDING_PIN_ICON, iconId)
                .putLong(KEY_PENDING_PIN_TIME, System.currentTimeMillis())
                .apply();
    }

    @Nullable
    public static String getPendingPinScriptPath(@NonNull Context context) {
        long time = getPrefs(context).getLong(KEY_PENDING_PIN_TIME, 0);
        // Valid for up to 30 seconds
        if (System.currentTimeMillis() - time > 30000) return null;
        return getPrefs(context).getString(KEY_PENDING_PIN_PATH, null);
    }

    @Nullable
    public static String getPendingPinLabel(@NonNull Context context) {
        return getPrefs(context).getString(KEY_PENDING_PIN_LABEL, null);
    }

    @Nullable
    public static String getPendingPinIconId(@NonNull Context context) {
        return getPrefs(context).getString(KEY_PENDING_PIN_ICON, null);
    }

    public static void clearPendingPin(@NonNull Context context) {
        getPrefs(context).edit()
                .remove(KEY_PENDING_PIN_PATH)
                .remove(KEY_PENDING_PIN_LABEL)
                .remove(KEY_PENDING_PIN_ICON)
                .remove(KEY_PENDING_PIN_TIME)
                .apply();
    }

    public static void removeButtonWidgetConfig(@NonNull Context context, int appWidgetId) {
        getPrefs(context).edit()
                .remove(KEY_PATH_PREFIX + appWidgetId)
                .remove(KEY_LABEL_PREFIX + appWidgetId)
                .remove(KEY_ICON_PREFIX + appWidgetId)
                .apply();
    }
}
