package com.termux.widget;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

/**
 * Stores cosmetic-only metadata for shortcuts (custom display names and custom icon IDs).
 * This does NOT alter the physical script files in Termux.
 */
public final class TermuxShortcutMetadataPreferences {

    private static final String PREF_NAME = "termux_shortcut_metadata_preferences";
    private static final String KEY_PREFIX_LABEL = "label_";
    private static final String KEY_PREFIX_ICON = "icon_";

    private TermuxShortcutMetadataPreferences() {}

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void setShortcutMetadata(@NonNull Context context, @NonNull String path,
                                           @Nullable String label, @Nullable String iconId) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        if (label != null && !label.trim().isEmpty()) {
            editor.putString(KEY_PREFIX_LABEL + path, label.trim());
        } else {
            editor.remove(KEY_PREFIX_LABEL + path);
        }

        if (iconId != null && !iconId.trim().isEmpty()) {
            editor.putString(KEY_PREFIX_ICON + path, iconId.trim());
        } else {
            editor.remove(KEY_PREFIX_ICON + path);
        }
        editor.apply();
    }

    public static void setCustomLabel(@NonNull Context context, @NonNull String path, @Nullable String label) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        if (label != null && !label.trim().isEmpty()) {
            editor.putString(KEY_PREFIX_LABEL + path, label.trim());
        } else {
            editor.remove(KEY_PREFIX_LABEL + path);
        }
        editor.apply();
    }

    @Nullable
    public static String getCustomLabel(@NonNull Context context, @NonNull String path) {
        return getPrefs(context).getString(KEY_PREFIX_LABEL + path, null);
    }

    public static void setCustomIconId(@NonNull Context context, @NonNull String path, @Nullable String iconId) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        if (iconId != null && !iconId.trim().isEmpty()) {
            editor.putString(KEY_PREFIX_ICON + path, iconId.trim());
        } else {
            editor.remove(KEY_PREFIX_ICON + path);
        }
        editor.apply();
    }

    @Nullable
    public static String getCustomIconId(@NonNull Context context, @NonNull String path) {
        return getPrefs(context).getString(KEY_PREFIX_ICON + path, null);
    }

    public static void clearShortcutMetadata(@NonNull Context context, @NonNull String path) {
        getPrefs(context).edit()
                .remove(KEY_PREFIX_LABEL + path)
                .remove(KEY_PREFIX_ICON + path)
                .apply();
    }

    public static boolean hasCustomMetadata(@NonNull Context context, @NonNull String path) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.contains(KEY_PREFIX_LABEL + path) || prefs.contains(KEY_PREFIX_ICON + path);
    }

    public static Map<String, ?> getAllMetadata(@NonNull Context context) {
        return getPrefs(context).getAll();
    }
}
