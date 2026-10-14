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
import com.termux.widget.theme.ThemeManager;
import com.termux.widget.utils.ShortcutUtils;

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
            viewMapping.put(new android.util.SizeF(60f, 60f), buildStackRemoteViews(context, appWidgetId, TermuxGridWidgetProvider.class, TermuxGridWidgetService.class));
            viewMapping.put(new android.util.SizeF(120f, 120f), buildSingleGridRemoteViews(context, appWidgetId, false));
            remoteViews = new RemoteViews(viewMapping);
        } else {
            int minWidth = options != null ? options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) : 0;
            int minHeight = options != null ? options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) : 0;
            boolean is1x1 = (minWidth > 0 && minWidth < 120 && minHeight < 120);
            if (is1x1) {
                remoteViews = buildStackRemoteViews(context, appWidgetId, TermuxGridWidgetProvider.class, TermuxGridWidgetService.class);
            } else {
                boolean singleCol = (minWidth > 0 && minWidth < 170);
                remoteViews = buildSingleGridRemoteViews(context, appWidgetId, singleCol);
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
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
