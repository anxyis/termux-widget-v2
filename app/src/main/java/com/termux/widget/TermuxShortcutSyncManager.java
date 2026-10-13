package com.termux.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TermuxShortcutSyncManager {

    private static final String LOG_TAG = "TermuxShortcutSyncManager";
    private static final String PREF_NAME = "termux_synced_shortcuts";
    private static final String KEY_SYNCED_PATHS = "synced_paths";
    private static long sLastSyncTimestamp = 0;

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public static List<String> getSyncedPaths(@NonNull Context context) {
        List<String> list = new ArrayList<>();
        String jsonStr = getPrefs(context).getString(KEY_SYNCED_PATHS, null);
        if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(jsonStr);
                for (int i = 0; i < arr.length(); i++) {
                    list.add(arr.getString(i));
                }
            } catch (JSONException ignored) {}
        }
        return list;
    }

    public static void saveSyncedPaths(@NonNull Context context, @NonNull List<String> paths) {
        JSONArray arr = new JSONArray();
        for (String p : paths) {
            arr.put(p);
        }
        getPrefs(context).edit().putString(KEY_SYNCED_PATHS, arr.toString()).apply();
    }

    public static void syncShortcutsFromTermux(@NonNull Context context) {
        syncShortcutsFromTermux(context, false);
    }

    public static void syncShortcutsFromTermux(@NonNull Context context, boolean force) {
        // Debounce: don't sync more than once every 3 seconds unless forced
        long now = System.currentTimeMillis();
        if (!force && (now - sLastSyncTimestamp < 3000)) return;
        sLastSyncTimestamp = now;

        // 1. Direct check: If ~/.shortcuts is directly readable by this app process, read it directly
        File shortcutsDir = TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR;
        if (shortcutsDir.exists() && shortcutsDir.canRead()) {
            File[] files = shortcutsDir.listFiles();
            if (files != null && files.length > 0) {
                List<String> paths = new ArrayList<>();
                findPathsRecursive(shortcutsDir, paths);
                if (!paths.isEmpty()) {
                    saveSyncedPaths(context, paths);
                    return;
                }
            }
        }

        // 2. Query Termux via RUN_COMMAND in the background
        try {
            context.getPackageManager().getPackageInfo("com.termux", 0);
        } catch (Exception e) {
            return;
        }

        Intent runIntent = new Intent("com.termux.RUN_COMMAND");
        runIntent.setClassName("com.termux", "com.termux.app.RunCommandService");
        runIntent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
        runIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-c", "find \"$HOME/.shortcuts\" -maxdepth 5 -type f ! -name '.*' ! -path '*/.icons*' ! -path '*/icons*' 2>/dev/null"});
        runIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        runIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);

        Intent callbackIntent = new Intent(context, TermuxShortcutSyncReceiver.class);
        callbackIntent.setAction(TermuxShortcutSyncReceiver.ACTION_SYNC_RESULT);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 2001, callbackIntent, flags);
        runIntent.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pendingIntent);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(runIntent);
            } else {
                context.startService(runIntent);
            }
            Logger.logDebug(LOG_TAG, "Dispatched sync command to Termux");
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to start sync service", e);
        }
    }

    private static void findPathsRecursive(File dir, List<String> paths) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) {
                findPathsRecursive(f, paths);
            } else {
                paths.add(f.getAbsolutePath());
            }
        }
    }
}
