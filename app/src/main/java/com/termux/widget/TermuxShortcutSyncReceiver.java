package com.termux.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;

import java.util.ArrayList;
import java.util.List;

public class TermuxShortcutSyncReceiver extends BroadcastReceiver {

    public static final String ACTION_SYNC_RESULT = "com.termux.widget.v2.SYNC_RESULT";
    private static final String LOG_TAG = "TermuxShortcutSyncReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        Logger.logDebug(LOG_TAG, "onReceive sync result");

        Bundle resultBundle = intent.getBundleExtra(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE);
        if (resultBundle == null) {
            resultBundle = intent.getExtras();
        }

        if (resultBundle != null) {
            String stdout = resultBundle.getString(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT);
            if (stdout == null) {
                stdout = resultBundle.getString("stdout");
            }

            if (stdout != null && !stdout.trim().isEmpty()) {
                List<String> paths = new ArrayList<>();
                for (String line : stdout.split("\n")) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.contains("/.icons/") && !trimmed.endsWith("/icons")) {
                        paths.add(trimmed);
                    }
                }
                List<String> oldPaths = TermuxShortcutSyncManager.getSyncedPaths(context);
                if (!paths.equals(oldPaths)) {
                    Logger.logDebug(LOG_TAG, "Synced " + paths.size() + " shortcut paths from Termux");
                    TermuxShortcutSyncManager.saveSyncedPaths(context, paths);
                    TermuxWidgetProvider.sendIntentToRefreshAllWidgets(context, LOG_TAG);
                    context.sendBroadcast(new Intent("com.termux.widget.v2.SHORTCUTS_UPDATED"));
                }
            }
        }
    }
}
