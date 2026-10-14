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

import com.termux.shared.file.FileUtils;
import com.termux.shared.file.filesystem.FileType;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants.TERMUX_WIDGET_APP.TERMUX_WIDGET_PROVIDER;
import com.termux.widget.icons.IconHelper;
import com.termux.widget.theme.ThemeManager;
import com.termux.widget.utils.ShortcutUtils;

import java.util.ArrayList;
import java.util.Arrays;

public final class TermuxGridWidgetProvider extends AppWidgetProvider {

    private static final String LOG_TAG = "TermuxGridWidgetProvider";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        if (appWidgetIds == null || appWidgetIds.length == 0) return;

        for (int appWidgetId : appWidgetIds) {
            updateAppWidgetRemoteViews(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, android.os.Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        updateAppWidgetRemoteViews(context, appWidgetManager, appWidgetId);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_grid);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_stack_view);
    }

    public static void updateAppWidgetRemoteViews(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return;

        android.os.Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        RemoteViews remoteViews;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            java.util.Map<android.util.SizeF, RemoteViews> viewMapping = new java.util.HashMap<>();
            viewMapping.put(new android.util.SizeF(60f, 60f), build4TileRemoteViews(context, appWidgetId));
            viewMapping.put(new android.util.SizeF(120f, 120f), buildSingleGridRemoteViews(context, appWidgetId, false));
            remoteViews = new RemoteViews(viewMapping);
        } else {
            int minWidth = options != null ? options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) : 0;
            int minHeight = options != null ? options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) : 0;
            boolean is1x1 = (minWidth > 0 && minWidth < 120 && minHeight < 120);
            if (is1x1) {
                remoteViews = build4TileRemoteViews(context, appWidgetId);
            } else {
                boolean singleCol = (minWidth > 0 && minWidth < 170);
                remoteViews = buildSingleGridRemoteViews(context, appWidgetId, singleCol);
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    public static RemoteViews build4TileRemoteViews(@NonNull Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_grid_4tile_layout);

        boolean isAmoled = ThemeManager.isAmoledMode(context);
        boolean isTrans = TermuxWidgetStylePreferences.isTransparencyEnabled(context);
        int transPercent = TermuxWidgetStylePreferences.getTransparencyPercent(context);
        float cardAlpha = isTrans ? Math.max(0.08f, Math.min(1.0f, (100 - transPercent) / 100f)) : 1.0f;

        boolean isCustom = TermuxWidgetStylePreferences.isCustomColorsEnabled(context);
        int customPrimary = TermuxWidgetStylePreferences.getCustomPrimaryColor(context);
        int cornerRadius = TermuxWidgetStylePreferences.getCornerRadiusDp(context);

        ArrayList<ShortcutFile> list = new ArrayList<>();
        ShortcutUtils.enumerateShortcutFiles(context, list, false);

        if (list.isEmpty()) {
            views.setViewVisibility(R.id.layout_4tile_grid, android.view.View.GONE);
            views.setViewVisibility(R.id.layout_1tile, android.view.View.GONE);
            views.setViewVisibility(R.id.empty_view_4tile, android.view.View.VISIBLE);
            return views;
        }

        if (list.size() == 1) {
            views.setViewVisibility(R.id.layout_4tile_grid, android.view.View.GONE);
            views.setViewVisibility(R.id.empty_view_4tile, android.view.View.GONE);
            views.setViewVisibility(R.id.layout_1tile, android.view.View.VISIBLE);

            ShortcutFile file = list.get(0);
            int bgRes = WidgetBackgroundHelper.getButtonBackground(isAmoled, file.isToggleOn(), cornerRadius, false);
            views.setInt(R.id.layout_1tile, "setBackgroundResource", bgRes);
            views.setFloat(R.id.layout_1tile, "setAlpha", cardAlpha);

            int badgeBgColor;
            int iconColor;
            if (isAmoled) {
                badgeBgColor = file.isToggleOn() ? Color.WHITE : Color.parseColor("#262626");
                iconColor = file.isToggleOn() ? Color.BLACK : Color.WHITE;
            } else if (isCustom) {
                badgeBgColor = file.isToggleOn() ? Color.WHITE : customPrimary;
                iconColor = file.isToggleOn() ? Color.BLACK : (androidx.core.graphics.ColorUtils.calculateLuminance(customPrimary) > 0.55 ? Color.BLACK : Color.WHITE);
            } else {
                badgeBgColor = file.isToggleOn() ? context.getColor(R.color.widget_toggle_badge_on) : context.getColor(R.color.widget_accent_circle_bg);
                iconColor = file.isToggleOn() ? context.getColor(R.color.widget_toggle_icon_on) : context.getColor(R.color.widget_accent_circle_icon);
            }

            views.setInt(R.id.tile_single_badge_bg, "setColorFilter", badgeBgColor);
            views.setInt(R.id.tile_single_icon, "setColorFilter", iconColor);
            IconHelper.applyToRemoteViews(views, R.id.tile_single_icon, R.id.tile_single_custom_image, file.getIconId(), file.isTask() ? R.drawable.ic_hero_bolt : R.drawable.ic_lucide_terminal);

            bindTileClick(context, views, R.id.layout_1tile, file, appWidgetId, 0);
            return views;
        }

        // 2 to 4 tiles bento layout matching user Image 4
        views.setViewVisibility(R.id.layout_4tile_grid, android.view.View.VISIBLE);
        views.setViewVisibility(R.id.layout_1tile, android.view.View.GONE);
        views.setViewVisibility(R.id.empty_view_4tile, android.view.View.GONE);

        int[] tileIds = {R.id.tile_1, R.id.tile_2, R.id.tile_3, R.id.tile_4};
        int[] badgeBgIds = {R.id.tile_1_badge_bg, R.id.tile_2_badge_bg, R.id.tile_3_badge_bg, R.id.tile_4_badge_bg};
        int[] iconIds = {R.id.tile_1_icon, R.id.tile_2_icon, R.id.tile_3_icon, R.id.tile_4_icon};
        int[] customImgIds = {R.id.tile_1_custom_image, R.id.tile_2_custom_image, R.id.tile_3_custom_image, R.id.tile_4_custom_image};

        // Distinct vibrant accent hues matching Image 4 (green, orange, pink, purple)
        int[] rainbowHues = {
            0xFF10B981, // Emerald Green
            0xFFF97316, // Orange
            0xFFEC4899, // Pink
            0xFF8B5CF6  // Purple
        };

        int count = Math.min(4, list.size());
        if (count == 2) {
            views.setViewVisibility(R.id.row_bottom, android.view.View.GONE);
        } else {
            views.setViewVisibility(R.id.row_bottom, android.view.View.VISIBLE);
            if (count == 3) {
                views.setViewVisibility(R.id.tile_4, android.view.View.INVISIBLE);
            } else {
                views.setViewVisibility(R.id.tile_4, android.view.View.VISIBLE);
            }
        }

        for (int i = 0; i < count; i++) {
            ShortcutFile file = list.get(i);
            int bgRes = WidgetBackgroundHelper.getButtonBackground(isAmoled, file.isToggleOn(), cornerRadius, false);
            views.setInt(tileIds[i], "setBackgroundResource", bgRes);
            views.setFloat(tileIds[i], "setAlpha", cardAlpha);
            views.setViewVisibility(tileIds[i], android.view.View.VISIBLE);

            int badgeBgColor;
            int iconColor;
            if (isAmoled) {
                badgeBgColor = file.isToggleOn() ? Color.WHITE : Color.parseColor("#262626");
                iconColor = file.isToggleOn() ? Color.BLACK : Color.WHITE;
            } else if (isCustom) {
                badgeBgColor = file.isToggleOn() ? Color.WHITE : customPrimary;
                iconColor = file.isToggleOn() ? Color.BLACK : (androidx.core.graphics.ColorUtils.calculateLuminance(customPrimary) > 0.55 ? Color.BLACK : Color.WHITE);
            } else {
                badgeBgColor = file.isToggleOn() ? context.getColor(R.color.widget_toggle_badge_on) : rainbowHues[i % rainbowHues.length];
                iconColor = file.isToggleOn() ? context.getColor(R.color.widget_toggle_icon_on) : Color.WHITE;
            }

            views.setInt(badgeBgIds[i], "setColorFilter", badgeBgColor);
            views.setInt(iconIds[i], "setColorFilter", iconColor);
            IconHelper.applyToRemoteViews(views, iconIds[i], customImgIds[i], file.getIconId(), file.isTask() ? R.drawable.ic_hero_bolt : R.drawable.ic_lucide_terminal);

            bindTileClick(context, views, tileIds[i], file, appWidgetId, i);
        }

        return views;
    }

    private static void bindTileClick(Context context, RemoteViews views, int viewId, ShortcutFile file, int appWidgetId, int index) {
        if (file.isToggle()) {
            Intent toggleIntent = new Intent(context, TermuxButtonWidgetProvider.class);
            toggleIntent.setAction(TermuxButtonWidgetProvider.ACTION_RUN_BUTTON_SHORTCUT);
            toggleIntent.putExtra(TermuxButtonWidgetProvider.EXTRA_BUTTON_SCRIPT_PATH, file.getPath());
            toggleIntent.setData(Uri.parse("termux4tile://" + appWidgetId + "/" + index));
            @SuppressLint("UnspecifiedImmutableFlag")
            PendingIntent pi = PendingIntent.getBroadcast(
                    context, appWidgetId * 10 + index, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(viewId, pi);
        } else {
            Intent execIntent = file.getExecutionIntent(context);
            execIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            @SuppressLint("UnspecifiedImmutableFlag")
            PendingIntent pi = PendingIntent.getActivity(
                    context, appWidgetId * 10 + index, execIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(viewId, pi);
        }
    }

    public static RemoteViews buildStackRemoteViews(@NonNull Context context, int appWidgetId, Class<?> providerClass, Class<?> serviceClass) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_stack_layout);
        remoteViews.setEmptyView(R.id.widget_stack_view, R.id.empty_view);
        if (ThemeManager.isAmoledMode(context)) {
            remoteViews.setTextColor(R.id.txt_empty_message, Color.WHITE);
        }

        Intent intent = new Intent(context, serviceClass);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra("IS_STACK_VIEW", true);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setRemoteAdapter(R.id.widget_stack_view, intent);

        Intent clickIntent = new Intent(context, providerClass);
        clickIntent.setAction(TERMUX_WIDGET_PROVIDER.ACTION_WIDGET_ITEM_CLICKED);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        clickIntent.setData(Uri.parse(clickIntent.toUri(Intent.URI_INTENT_SCHEME)));
        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setPendingIntentTemplate(R.id.widget_stack_view, clickPendingIntent);

        return remoteViews;
    }

    private static RemoteViews buildSingleGridRemoteViews(@NonNull Context context, int appWidgetId, boolean singleColumn) {
        int layoutId = singleColumn ? R.layout.widget_grid_layout_1col : R.layout.widget_grid_layout;
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), layoutId);
        remoteViews.setEmptyView(R.id.widget_grid, R.id.empty_view);
        if (ThemeManager.isAmoledMode(context)) {
            remoteViews.setTextColor(R.id.txt_empty_message, Color.WHITE);
        }

        Intent intent = new Intent(context, TermuxGridWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setRemoteAdapter(R.id.widget_grid, intent);

        // Setup pending intent template for grid items
        Intent clickIntent = new Intent(context, TermuxGridWidgetProvider.class);
        clickIntent.setAction(TERMUX_WIDGET_PROVIDER.ACTION_WIDGET_ITEM_CLICKED);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        clickIntent.setData(Uri.parse(clickIntent.toUri(Intent.URI_INTENT_SCHEME)));
        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setPendingIntentTemplate(R.id.widget_grid, clickPendingIntent);

        return remoteViews;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (action == null) return;

        switch (action) {
            case AppWidgetManager.ACTION_APPWIDGET_UPDATE: {
                if (!ShortcutUtils.isTermuxAppAccessible(context, LOG_TAG, false)) return;
                int[] ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                if (ids != null) {
                    for (int id : ids) {
                        updateAppWidgetRemoteViews(context, AppWidgetManager.getInstance(context), id);
                        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(id, R.id.widget_grid);
                        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(id, R.id.widget_stack_view);
                    }
                }
                return;
            }
            case TERMUX_WIDGET_PROVIDER.ACTION_WIDGET_ITEM_CLICKED: {
                String clickedFilePath = intent.getStringExtra(TERMUX_WIDGET_PROVIDER.EXTRA_FILE_CLICKED);
                if (clickedFilePath == null || clickedFilePath.isEmpty()) return;
                if (com.termux.widget.toggle.TermuxTogglePreferences.handleToggleClick(context, clickedFilePath)) {
                    return;
                }
                if (FileUtils.getFileType(clickedFilePath, true) == FileType.DIRECTORY) return;
                TermuxWidgetProvider.sendExecutionIntentToTermuxService(context, clickedFilePath, LOG_TAG);
                return;
            }
            case TERMUX_WIDGET_PROVIDER.ACTION_REFRESH_WIDGET: {
                int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                int[] appWidgetIds;
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    appWidgetIds = new int[]{appWidgetId};
                } else {
                    appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, TermuxGridWidgetProvider.class));
                }
                for (int id : appWidgetIds) {
                    updateAppWidgetRemoteViews(context, AppWidgetManager.getInstance(context), id);
                    AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(id, R.id.widget_grid);
                    AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(id, R.id.widget_stack_view);
                }
                return;
            }
        }

        super.onReceive(context, intent);
    }
}
