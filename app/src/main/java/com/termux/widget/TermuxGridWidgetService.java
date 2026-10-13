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
import java.util.List;

public final class TermuxGridWidgetService extends RemoteViewsService {

    private static final String LOG_TAG = "TermuxGridWidgetService";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        return new GridRemoteViewsFactory(getApplicationContext(), appWidgetId);
    }

    public static class GridRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private final List<ShortcutFile> shortcutFiles = new ArrayList<>();
        private final Context mContext;
        private final int mAppWidgetId;

        public GridRemoteViewsFactory(Context context, int appWidgetId) {
            mContext = context;
            mAppWidgetId = appWidgetId;
        }

        @Override
        public void onCreate() {
            Logger.logDebug(LOG_TAG, "onCreate(): " + mAppWidgetId);
        }

        @Override
        public void onDestroy() {
            shortcutFiles.clear();
        }

        @Override
        public int getCount() {
            return shortcutFiles.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= shortcutFiles.size()) return null;
            return shortcutFiles.get(position).getGridWidgetView(mContext);
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void onDataSetChanged() {
            shortcutFiles.clear();
            TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR.mkdirs();
            ShortcutUtils.enumerateShortcutFiles(shortcutFiles, true);
        }
    }
}
