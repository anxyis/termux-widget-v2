package com.termux.widget;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.termux.shared.logger.Logger;
import com.termux.widget.icons.IconHelper;
import com.termux.widget.icons.IconRegistry;
import com.termux.widget.theme.ThemeManager;

public final class TermuxButtonWidgetProvider extends AppWidgetProvider {

    private static final String LOG_TAG = "TermuxButtonWidgetProvider";
    public static final String ACTION_RUN_BUTTON_SHORTCUT = "com.termux.widget.ACTION_RUN_BUTTON_SHORTCUT";
    public static final String EXTRA_BUTTON_SCRIPT_PATH = "button_script_path";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        if (appWidgetIds == null || appWidgetIds.length == 0) return;

        for (int appWidgetId : appWidgetIds) {
            updateButtonWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateButtonWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return;

        String scriptPath = TermuxButtonWidgetPreferences.getScriptPath(context, appWidgetId);
        String label = TermuxButtonWidgetPreferences.getLabel(context, appWidgetId);
        String iconId = TermuxButtonWidgetPreferences.getIconId(context, appWidgetId);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_button_layout);
        boolean isAmoled = ThemeManager.isAmoledMode(context);
        int cornerRadius = TermuxWidgetStylePreferences.getCornerRadiusDp(context);
        int textSizeSp = TermuxWidgetStylePreferences.getTextSizeSp(context);
        int iconSizeDp = TermuxWidgetStylePreferences.getIconSizeDp(context);

        views.setTextViewTextSize(R.id.widget_button_label, android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        views.setTextViewTextSize(R.id.widget_button_sublabel, android.util.TypedValue.COMPLEX_UNIT_SP, Math.max(9, textSizeSp - 2));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            views.setViewLayoutWidth(R.id.widget_button_icon, iconSizeDp, android.util.TypedValue.COMPLEX_UNIT_DIP);
            views.setViewLayoutHeight(R.id.widget_button_icon, iconSizeDp, android.util.TypedValue.COMPLEX_UNIT_DIP);
        }

        if (scriptPath != null && !scriptPath.isEmpty()) {
            boolean isToggle = scriptPath.startsWith(com.termux.widget.toggle.TermuxTogglePreferences.PREFIX_TOGGLE);
            com.termux.widget.toggle.ToggleShortcut toggle = isToggle ?
                    com.termux.widget.toggle.TermuxTogglePreferences.getToggle(context, scriptPath.substring(com.termux.widget.toggle.TermuxTogglePreferences.PREFIX_TOGGLE.length())) : null;

            String displayLabel = label != null ? label : (toggle != null ? toggle.label : "Shortcut");
            views.setTextViewText(R.id.widget_button_label, displayLabel);

            boolean isToggleOn = toggle != null && toggle.isOn;
            int bgRes = WidgetBackgroundHelper.getButtonBackground(isAmoled, isToggleOn, cornerRadius);
            views.setInt(R.id.widget_button_root, "setBackgroundResource", bgRes);

            if (isAmoled) {
                if (toggle != null) {
                    views.setTextViewText(R.id.widget_button_sublabel, toggle.isOn ? "State: ON" : "State: OFF");
                    if (toggle.isOn) {
                        views.setTextColor(R.id.widget_button_label, Color.BLACK);
                        views.setTextColor(R.id.widget_button_sublabel, Color.BLACK);
                        views.setInt(R.id.widget_button_icon, "setColorFilter", Color.BLACK);
                    } else {
                        views.setTextColor(R.id.widget_button_label, Color.WHITE);
                        views.setTextColor(R.id.widget_button_sublabel, Color.parseColor("#9E9E9E"));
                        views.setInt(R.id.widget_button_icon, "setColorFilter", Color.WHITE);
                    }
                } else {
                    views.setTextViewText(R.id.widget_button_sublabel, "Tap to run");
                    views.setTextColor(R.id.widget_button_label, Color.WHITE);
                    views.setTextColor(R.id.widget_button_sublabel, Color.parseColor("#9E9E9E"));
                    views.setInt(R.id.widget_button_icon, "setColorFilter", Color.WHITE);
                }
            } else {
                if (toggle != null) {
                    views.setTextViewText(R.id.widget_button_sublabel, toggle.isOn ? "State: ON" : "State: OFF");
                    if (toggle.isOn) {
                        views.setTextColor(R.id.widget_button_label, context.getColor(R.color.widget_toggle_text_on));
                        views.setTextColor(R.id.widget_button_sublabel, context.getColor(R.color.widget_toggle_text_on));
                    } else {
                        views.setTextColor(R.id.widget_button_label, context.getColor(R.color.m3_on_surface));
                        views.setTextColor(R.id.widget_button_sublabel, context.getColor(R.color.m3_on_surface_variant));
                    }
                } else {
                    views.setTextViewText(R.id.widget_button_sublabel, "Tap to run");
                    views.setTextColor(R.id.widget_button_label, context.getColor(R.color.m3_on_surface));
                    views.setTextColor(R.id.widget_button_sublabel, context.getColor(R.color.m3_on_surface_variant));
                }
            }

            IconHelper.applyToRemoteViews(views, R.id.widget_button_icon, 0, iconId, R.drawable.ic_lucide_terminal);

            Intent clickIntent = new Intent(context, TermuxButtonWidgetProvider.class);
            clickIntent.setAction(ACTION_RUN_BUTTON_SHORTCUT);
            clickIntent.putExtra(EXTRA_BUTTON_SCRIPT_PATH, scriptPath);
            clickIntent.setData(Uri.parse("termuxbutton://" + appWidgetId));

            @SuppressLint("UnspecifiedImmutableFlag")
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_button_root, pendingIntent);
        } else {
            int emptyBgRes = WidgetBackgroundHelper.getButtonBackground(isAmoled, false, cornerRadius);
            views.setInt(R.id.widget_button_root, "setBackgroundResource", emptyBgRes);
            views.setTextViewText(R.id.widget_button_label, "Setup Button");
            views.setTextViewText(R.id.widget_button_sublabel, "Tap to configure");
            views.setImageViewResource(R.id.widget_button_icon, R.drawable.ic_lucide_settings);
            if (isAmoled) {
                views.setTextColor(R.id.widget_button_label, Color.WHITE);
                views.setTextColor(R.id.widget_button_sublabel, Color.parseColor("#9E9E9E"));
                views.setInt(R.id.widget_button_icon, "setColorFilter", Color.WHITE);
            }

            Intent configIntent = new Intent(context, com.termux.widget.activities.TermuxButtonWidgetConfigureActivity.class);
            configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            @SuppressLint("UnspecifiedImmutableFlag")
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_button_root, pendingIntent);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_RUN_BUTTON_SHORTCUT.equals(intent.getAction())) {
            String scriptPath = intent.getStringExtra(EXTRA_BUTTON_SCRIPT_PATH);
            if (scriptPath != null && !scriptPath.isEmpty()) {
                if (com.termux.widget.toggle.TermuxTogglePreferences.handleToggleClick(context, scriptPath)) {
                    AppWidgetManager awm = AppWidgetManager.getInstance(context);
                    int[] ids = awm.getAppWidgetIds(new ComponentName(context, TermuxButtonWidgetProvider.class));
                    onUpdate(context, awm, ids);
                    return;
                }
                TermuxWidgetProvider.sendExecutionIntentToTermuxService(context, scriptPath, LOG_TAG);
                return;
            }
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            TermuxButtonWidgetPreferences.removeButtonWidgetConfig(context, appWidgetId);
        }
        super.onDeleted(context, appWidgetIds);
    }
}
