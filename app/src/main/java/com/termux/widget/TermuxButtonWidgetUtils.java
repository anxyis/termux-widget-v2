package com.termux.widget;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class TermuxButtonWidgetUtils {

    private TermuxButtonWidgetUtils() {}

    @SuppressLint("UnspecifiedImmutableFlag")
    public static void pinButtonWidget(@NonNull Context context,
                                       @NonNull String scriptPath,
                                       @Nullable String label,
                                       @Nullable String iconId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, TermuxButtonWidgetProvider.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported()) {
            TermuxButtonWidgetPreferences.setPendingPin(context, scriptPath, label, iconId);

            Intent callbackIntent = new Intent(context, TermuxButtonWidgetPinnedReceiver.class);
            callbackIntent.putExtra(TermuxButtonWidgetProvider.EXTRA_BUTTON_SCRIPT_PATH, scriptPath);
            callbackIntent.putExtra("label", label);
            callbackIntent.putExtra("icon_id", iconId);

            int requestCode = (int) (System.currentTimeMillis() & 0xfffffff);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent successCallback = PendingIntent.getBroadcast(context, requestCode, callbackIntent, flags);

            boolean requested = appWidgetManager.requestPinAppWidget(provider, null, successCallback);
            if (requested) {
                Toast.makeText(context, "Adding button widget to Home Screen...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to request widget pin from launcher", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Direct widget pinning is not supported by your launcher. Please add the 'Termux:Widget Button' directly from your home screen widget menu.", Toast.LENGTH_LONG).show();
        }
    }
}
