package com.termux.widget.tiles;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class TermuxTilePreferences {

    private static final String PREF_NAME = "termux_quick_tiles";
    private static final String KEY_SCRIPT_PREFIX = "tile_script_";
    private static final String KEY_LABEL_PREFIX = "tile_label_";
    private static final String KEY_ICON_PREFIX = "tile_icon_";

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveTile(@NonNull Context context, int slot,
                                @NonNull String scriptPath,
                                @NonNull String label,
                                @Nullable String iconId) {
        getPrefs(context).edit()
                .putString(KEY_SCRIPT_PREFIX + slot, scriptPath)
                .putString(KEY_LABEL_PREFIX + slot, label)
                .putString(KEY_ICON_PREFIX + slot, iconId)
                .apply();
    }

    @Nullable
    public static String getScriptPath(@NonNull Context context, int slot) {
        return getPrefs(context).getString(KEY_SCRIPT_PREFIX + slot, null);
    }

    @Nullable
    public static String getLabel(@NonNull Context context, int slot) {
        return getPrefs(context).getString(KEY_LABEL_PREFIX + slot, null);
    }

    @Nullable
    public static String getIconId(@NonNull Context context, int slot) {
        return getPrefs(context).getString(KEY_ICON_PREFIX + slot, null);
    }

    public static void clearTile(@NonNull Context context, int slot) {
        getPrefs(context).edit()
                .remove(KEY_SCRIPT_PREFIX + slot)
                .remove(KEY_LABEL_PREFIX + slot)
                .remove(KEY_ICON_PREFIX + slot)
                .apply();
    }

    public static boolean isTileConfigured(@NonNull Context context, int slot) {
        return getScriptPath(context, slot) != null;
    }
}
