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
        boolean isStack = intent != null && intent.getBooleanExtra("IS_STACK_VIEW", false);
        return new ListRemoteViewsFactory(getApplicationContext(), appWidgetId, isStack);
    }

    public static class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private final List<ShortcutFile> displayList = new ArrayList<>();
        private final Context mContext;
        private final int mAppWidgetId;
        private final boolean mIsStack;

        public ListRemoteViewsFactory(Context context, int appWidgetId, boolean isStack) {
            mContext = context;
            mAppWidgetId = appWidgetId;
            mIsStack = isStack;
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
            if (mIsStack) {
                return displayList.get(position).getStackWidgetView(mContext);
            }
            return displayList.get(position).getListWidgetView(mContext, position, displayList.size());
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return mIsStack ? 1 : 2;
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

            ShortcutUtils.enumerateShortcutFiles(mContext, displayList, !mIsStack);
        }
    }
}
