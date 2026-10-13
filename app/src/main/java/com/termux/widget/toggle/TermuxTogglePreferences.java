package com.termux.widget.toggle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.widget.TermuxWidgetProvider;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public final class TermuxTogglePreferences {

    private static final String PREFS_NAME = "termux_toggle_shortcuts_prefs";
    private static final String KEY_TOGGLES_JSON = "key_toggles_json";

    public static final String PREFIX_TOGGLE = "toggle:";

    private TermuxTogglePreferences() {}

    @NonNull
    public static synchronized List<ToggleShortcut> getToggles(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_TOGGLES_JSON, "[]");
        List<ToggleShortcut> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                ToggleShortcut t = ToggleShortcut.fromJsonObject(arr.getJSONObject(i));
                if (t != null) {
                    list.add(t);
                }
            }
        } catch (JSONException ignored) {}
        return list;
    }

    @Nullable
    public static synchronized ToggleShortcut getToggle(@NonNull Context context, @NonNull String id) {
        List<ToggleShortcut> list = getToggles(context);
        for (ToggleShortcut t : list) {
            if (t.id.equals(id)) return t;
        }
        return null;
    }

    public static synchronized void saveToggle(@NonNull Context context, @NonNull ToggleShortcut toggle) {
        List<ToggleShortcut> list = getToggles(context);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(toggle.id)) {
                list.set(i, toggle);
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(toggle);
        }
        saveAll(context, list);
    }

    public static synchronized void deleteToggle(@NonNull Context context, @NonNull String id) {
        List<ToggleShortcut> list = getToggles(context);
        List<ToggleShortcut> updated = new ArrayList<>();
        for (ToggleShortcut t : list) {
            if (!t.id.equals(id)) {
                updated.add(t);
            }
        }
        saveAll(context, updated);
    }

    private static void saveAll(@NonNull Context context, @NonNull List<ToggleShortcut> list) {
        JSONArray arr = new JSONArray();
        for (ToggleShortcut t : list) {
            arr.put(t.toJsonObject());
        }
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TOGGLES_JSON, arr.toString()).apply();
    }

    /**
     * Toggles state, executes corresponding ON/OFF script, updates widgets and quick tiles.
     */
    public static synchronized boolean toggleState(@NonNull Context context, @NonNull String toggleId) {
        ToggleShortcut toggle = getToggle(context, toggleId);
        if (toggle == null) return false;

        boolean newState = !toggle.isOn;
        toggle.isOn = newState;
        saveToggle(context, toggle);

        String scriptToRun = newState ? toggle.onScriptPath : toggle.offScriptPath;
        if (scriptToRun != null && !scriptToRun.isEmpty()) {
            TermuxWidgetProvider.sendExecutionIntentToTermuxService(context, scriptToRun, "ToggleShortcut");
            Toast.makeText(context, toggle.label + " turned " + (newState ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        }

        // Refresh all collection widgets immediately so UI reflects new state
        TermuxWidgetProvider.sendIntentToRefreshAllWidgets(context, "ToggleAction");

        // Request Quick Settings tiles to re-listen
        requestAllQuickTilesUpdate(context);

        return true;
    }

    /**
     * Intercepts widget item clicks: returns true if handled as a toggle shortcut.
     */
    public static boolean handleToggleClick(@NonNull Context context, @Nullable String clickedPath) {
        if (clickedPath != null && clickedPath.startsWith(PREFIX_TOGGLE)) {
            String toggleId = clickedPath.substring(PREFIX_TOGGLE.length());
            return toggleState(context, toggleId);
        }
        return false;
    }

    public static void requestAllQuickTilesUpdate(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                for (int slot = 1; slot <= 8; slot++) {
                    String className = "com.termux.widget.tiles.TermuxTileService" + slot;
                    Class<?> clazz = Class.forName(className);
                    if (TileService.class.isAssignableFrom(clazz)) {
                        TileService.requestListeningState(context, new android.content.ComponentName(context, clazz));
                    }
                }
            } catch (Throwable ignored) {}
        }
    }
}
