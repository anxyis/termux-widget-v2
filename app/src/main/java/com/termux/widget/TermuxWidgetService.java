package com.termux.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.widget.utils.ShortcutUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TermuxWidgetService extends RemoteViewsService {

    private static final String LOG_TAG = "TermuxWidgetService";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        return new ListRemoteViewsFactory(getApplicationContext(), appWidgetId);
    }

    public static class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private final List<ShortcutFile> displayList = new ArrayList<>();
        private final Context mContext;
        private final int mAppWidgetId;

        public ListRemoteViewsFactory(Context context, int appWidgetId) {
            mContext = context;
            mAppWidgetId = appWidgetId;
        }

        @Override
        public void onCreate() {
            Logger.logDebug(LOG_TAG, "onCreate(): " + mAppWidgetId);
        }

        @Override
        public void onDestroy() {
            Logger.logDebug(LOG_TAG, "onDestroy(): " + mAppWidgetId);
            displayList.clear();
        }

        @Override
        public int getCount() {
            return displayList.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= displayList.size()) return null;
            return displayList.get(position).getListWidgetView(mContext);
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 2; // Type 0: header, Type 1: item
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public void onDataSetChanged() {
            Logger.logDebug(LOG_TAG, "onDataSetChanged(): " + mAppWidgetId);

            displayList.clear();
            try { TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR.mkdirs(); } catch (Exception ignored) {}

            List<ShortcutFile> rawFiles = new ArrayList<>();
            ShortcutUtils.enumerateShortcutFiles(mContext, rawFiles, true);

            // Group files by category/directory
            Map<String, List<ShortcutFile>> groups = new LinkedHashMap<>();
            for (ShortcutFile sf : rawFiles) {
                String group = sf.getGroupName();
                List<ShortcutFile> list = groups.computeIfAbsent(group, k -> new ArrayList<>());
                list.add(sf);
            }

            // If we have multiple groups or tasks, insert group headers
            boolean showHeaders = groups.size() > 1 || (groups.size() == 1 && !groups.containsKey("General"));

            for (Map.Entry<String, List<ShortcutFile>> entry : groups.entrySet()) {
                if (showHeaders) {
                    displayList.add(ShortcutFile.createGroupHeader(entry.getKey(), entry.getValue().size()));
                }
                displayList.addAll(entry.getValue());
            }
        }
    }
}
