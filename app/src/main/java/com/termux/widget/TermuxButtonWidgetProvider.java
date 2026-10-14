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

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, android.os.Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        updateButtonWidget(context, appWidgetManager, appWidgetId);
    }

    public static void updateButtonWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return;

        RemoteViews views;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            java.util.Map<android.util.SizeF, RemoteViews> viewMapping = new java.util.HashMap<>();
            viewMapping.put(new android.util.SizeF(50f, 50f), buildButtonRemoteViews(context, appWidgetId, true));
            viewMapping.put(new android.util.SizeF(110f, 40f), buildButtonRemoteViews(context, appWidgetId, false));
            views = new RemoteViews(viewMapping);
        } else {
            android.os.Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minWidth = options != null ? options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) : 0;
            boolean isSquare = (minWidth > 0 && minWidth < 110);
            views = buildButtonRemoteViews(context, appWidgetId, isSquare);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static RemoteViews buildButtonRemoteViews(@NonNull Context context, int appWidgetId, boolean isSquare) {
        String scriptPath = TermuxButtonWidgetPreferences.getScriptPath(context, appWidgetId);
        String label = TermuxButtonWidgetPreferences.getLabel(context, appWidgetId);
        String iconId = TermuxButtonWidgetPreferences.getIconId(context, appWidgetId);

        int layoutId = isSquare ? R.layout.widget_button_square_layout : R.layout.widget_button_layout;
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        boolean isAmoled = ThemeManager.isAmoledMode(context);
        boolean isTrans = TermuxWidgetStylePreferences.isTransparencyEnabled(context);
        int transPercent = TermuxWidgetStylePreferences.getTransparencyPercent(context);
        float cardAlpha = isTrans ? Math.max(0.08f, Math.min(1.0f, (100 - transPercent) / 100f)) : 1.0f;

        boolean isCustom = TermuxWidgetStylePreferences.isCustomColorsEnabled(context);
        int customPrimary = TermuxWidgetStylePreferences.getCustomPrimaryColor(context);
        int customText = TermuxWidgetStylePreferences.getCustomTextColor(context);

        int cornerRadius = TermuxWidgetStylePreferences.getCornerRadiusDp(context);
        int textSizeSp = TermuxWidgetStylePreferences.getTextSizeSp(context);
        int iconSizeDp = TermuxWidgetStylePreferences.getIconSizeDp(context);
        int badgeSizeDp = Math.round(iconSizeDp * 1.6f);

        views.setTextViewTextSize(R.id.widget_button_label, android.util.TypedValue.COMPLEX_UNIT_SP, isSquare ? Math.max(10, textSizeSp - 2) : textSizeSp);
        views.setTextViewTextSize(R.id.widget_button_sublabel, android.util.TypedValue.COMPLEX_UNIT_SP, Math.max(9, textSizeSp - 3));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            views.setViewLayoutWidth(R.id.widget_button_icon, iconSizeDp, android.util.TypedValue.COMPLEX_UNIT_DIP);
            views.setViewLayoutHeight(R.id.widget_button_icon, iconSizeDp, android.util.TypedValue.COMPLEX_UNIT_DIP);
            views.setViewLayoutWidth(R.id.widget_button_icon_frame, badgeSizeDp, android.util.TypedValue.COMPLEX_UNIT_DIP);
            views.setViewLayoutHeight(R.id.widget_button_icon_frame, badgeSizeDp, android.util.TypedValue.COMPLEX_UNIT_DIP);
        }

        if (scriptPath != null && !scriptPath.isEmpty()) {
            boolean isToggle = scriptPath.startsWith(com.termux.widget.toggle.TermuxTogglePreferences.PREFIX_TOGGLE);
            com.termux.widget.toggle.ToggleShortcut toggle = isToggle ?
                    com.termux.widget.toggle.TermuxTogglePreferences.getToggle(context, scriptPath.substring(com.termux.widget.toggle.TermuxTogglePreferences.PREFIX_TOGGLE.length())) : null;

            String displayLabel = label != null ? label : (toggle != null ? toggle.label : "Shortcut");
            views.setTextViewText(R.id.widget_button_label, displayLabel);

            boolean isToggleOn = toggle != null && toggle.isOn;
            int bgRes = WidgetBackgroundHelper.getButtonBackground(isAmoled, isToggleOn, cornerRadius, false);
            views.setInt(R.id.widget_button_root, "setBackgroundResource", bgRes);
            views.setFloat(R.id.widget_button_root, "setAlpha", cardAlpha);

            int badgeBgColor;
            int iconColor;
            int textColor;
            int sublabelColor;

            if (isAmoled) {
                if (isToggleOn) {
                    badgeBgColor = Color.WHITE;
                    iconColor = Color.BLACK;
                    textColor = Color.WHITE;
                    sublabelColor = Color.parseColor("#CCCCCC");
                } else {
                    badgeBgColor = Color.parseColor("#262626");
                    iconColor = Color.WHITE;
                    textColor = Color.WHITE;
                    sublabelColor = Color.parseColor("#9E9E9E");
                }
            } else if (isCustom) {
                if (isToggleOn) {
                    badgeBgColor = Color.WHITE;
                    iconColor = Color.BLACK;
                    textColor = Color.BLACK;
                    sublabelColor = Color.BLACK;
                } else {
                    badgeBgColor = customPrimary;
                    iconColor = androidx.core.graphics.ColorUtils.calculateLuminance(customPrimary) > 0.55 ? Color.BLACK : Color.WHITE;
                    textColor = customText;
                    sublabelColor = Color.parseColor("#A1A1AA");
                }
            } else {
                // Material You
                if (isToggleOn) {
                    badgeBgColor = context.getColor(R.color.widget_toggle_badge_on);
                    iconColor = context.getColor(R.color.widget_toggle_icon_on);
                    textColor = context.getColor(R.color.widget_toggle_text_on);
                    sublabelColor = context.getColor(R.color.widget_toggle_text_on);
                } else {
                    badgeBgColor = context.getColor(R.color.widget_accent_circle_bg);
                    iconColor = context.getColor(R.color.widget_accent_circle_icon);
                    textColor = context.getColor(R.color.m3_on_surface);
                    sublabelColor = context.getColor(R.color.m3_on_surface_variant);
                }
            }

            views.setInt(R.id.widget_button_badge_bg, "setColorFilter", badgeBgColor);
            views.setInt(R.id.widget_button_icon, "setColorFilter", iconColor);
            views.setTextColor(R.id.widget_button_label, textColor);
            views.setTextColor(R.id.widget_button_sublabel, sublabelColor);
            views.setTextViewText(R.id.widget_button_sublabel, toggle != null ? (toggle.isOn ? "State: ON" : "State: OFF") : "Tap to run");

            IconHelper.applyToRemoteViews(views, R.id.widget_button_icon, R.id.widget_button_custom_image, iconId, R.drawable.ic_lucide_terminal);

            Intent clickIntent = new Intent(context, TermuxButtonWidgetProvider.class);
            clickIntent.setAction(ACTION_RUN_BUTTON_SHORTCUT);
            clickIntent.putExtra(EXTRA_BUTTON_SCRIPT_PATH, scriptPath);
            clickIntent.setData(Uri.parse("termuxbutton://" + appWidgetId));

            @SuppressLint("UnspecifiedImmutableFlag")
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_button_root, pendingIntent);
        } else {
            int emptyBgRes = WidgetBackgroundHelper.getButtonBackground(isAmoled, false, cornerRadius, false);
            views.setInt(R.id.widget_button_root, "setBackgroundResource", emptyBgRes);
            views.setFloat(R.id.widget_button_root, "setAlpha", cardAlpha);
            int emptyBadgeBgRes = isAmoled ? R.drawable.widget_icon_badge_amoled_bg : R.drawable.widget_icon_badge_bg;
            views.setInt(R.id.widget_button_icon_frame, "setBackgroundResource", emptyBadgeBgRes);
            views.setTextViewText(R.id.widget_button_label, "Setup Button");
            views.setTextViewText(R.id.widget_button_sublabel, "Tap to configure");
            views.setImageViewResource(R.id.widget_button_icon, R.drawable.ic_lucide_settings);
            if (isAmoled) {
                views.setTextColor(R.id.widget_button_label, Color.WHITE);
                views.setTextColor(R.id.widget_button_sublabel, Color.parseColor("#9E9E9E"));
                views.setInt(R.id.widget_button_icon, "setColorFilter", Color.WHITE);
            } else if (isCustom) {
                views.setTextColor(R.id.widget_button_label, customText);
                views.setTextColor(R.id.widget_button_sublabel, customText);
                views.setInt(R.id.widget_button_icon, "setColorFilter", customPrimary);
            }

            Intent configIntent = new Intent(context, com.termux.widget.activities.TermuxButtonWidgetConfigureActivity.class);
            configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            @SuppressLint("UnspecifiedImmutableFlag")
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_button_root, pendingIntent);
        }

        return views;
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
