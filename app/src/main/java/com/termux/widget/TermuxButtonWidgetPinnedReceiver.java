package com.termux.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.termux.shared.logger.Logger;

public class TermuxButtonWidgetPinnedReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "TermuxButtonWidgetPinnedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        String scriptPath = intent.getStringExtra(TermuxButtonWidgetProvider.EXTRA_BUTTON_SCRIPT_PATH);
        String label = intent.getStringExtra("label");
        String iconId = intent.getStringExtra("icon_id");

        Logger.logDebug(LOG_TAG, "Pinned widget callback received for id=" + appWidgetId + ", path=" + scriptPath);

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && scriptPath != null) {
            TermuxButtonWidgetPreferences.saveButtonWidgetConfig(
                    context, appWidgetId, scriptPath, label != null ? label : "Shortcut", iconId);
            AppWidgetManager awm = AppWidgetManager.getInstance(context);
            TermuxButtonWidgetProvider.updateButtonWidget(context, awm, appWidgetId);
            Toast.makeText(context, "Added button widget to Home Screen!", Toast.LENGTH_SHORT).show();
            TermuxButtonWidgetPreferences.clearPendingPin(context);
        }
    }
}
