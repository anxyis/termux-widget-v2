package com.termux.widget;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.termux.shared.data.DataUtils;
import com.termux.shared.data.IntentUtils;
import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;
import com.termux.shared.file.filesystem.FileType;
import com.termux.shared.logger.Logger;
import com.termux.shared.shell.ShellUtils;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.shell.command.ExecutionCommand.Runner;
import com.termux.shared.shell.command.result.ResultData;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;
import com.termux.shared.termux.TermuxConstants.TERMUX_WIDGET_APP.TERMUX_WIDGET_PROVIDER;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.shared.termux.settings.preferences.TermuxWidgetAppSharedPreferences;
import com.termux.widget.utils.ShortcutUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Widget providing a list to launch scripts in ~/.shortcuts/.
 * <p>
 * See https://developer.android.com/guide/topics/appwidgets/index.html
 */
public final class TermuxWidgetProvider extends AppWidgetProvider {

    private static final String LOG_TAG = "TermuxWidgetProvider";

    public void onEnabled(Context context) {
        Logger.logDebug(LOG_TAG, "onEnabled");
        if (!ShortcutUtils.isTermuxAppAccessible(context, LOG_TAG, false)) {
            Logger.logWarn(LOG_TAG, "Termux app not detected");
        }
    }

    /**
     * "This is called to update the App Widget at intervals defined by the updatePeriodMillis attribute in the
     * AppWidgetProviderInfo (see Adding the AppWidgetProviderInfo Metadata above). This method is also called when the
     * user adds the App Widget, so it should perform the essential setup, such as define event handlers for Views and
     * start a temporary Service, if necessary. However, if you have declared a configuration Activity, this method is
     * not called when the user adds the App Widget, but is called for the subsequent updates. It is the responsibility
     * of the configuration Activity to perform the first update when configuration is done. (See Creating an App Widget
     * Configuration Activity below.)"
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        Logger.logDebug(LOG_TAG, "onUpdate: " + Arrays.toString(appWidgetIds));
        if (appWidgetIds == null || appWidgetIds.length == 0) return;

        for (int appWidgetId : appWidgetIds) {
            updateAppWidgetRemoteViews(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidgetRemoteViews(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return;

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // The empty view is displayed when the collection has no items. It should be a sibling
        // of the collection view:
        remoteViews.setEmptyView(R.id.widget_list, R.id.empty_view);
        remoteViews.setTextViewText(R.id.txt_empty_message, context.getString(R.string.msg_no_shortcut_scripts));

        List<ShortcutFile> shortcutCountList = new ArrayList<>();
        ShortcutUtils.enumerateShortcutFiles(context, shortcutCountList, false);
        remoteViews.setTextViewText(R.id.txt_widget_count, String.format("[%02d]", shortcutCountList.size()));

        // Setup intent which points to the TermuxWidgetService which will provide the views for this collection.
        Intent intent = new Intent(context, TermuxWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        // When intents are compared, the extras are ignored, so we need to embed the extras
        // into the data so that the extras will not be ignored.
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setRemoteAdapter(R.id.widget_list, intent);

        // Setup refresh button:
        Intent refreshIntent = new Intent(context, TermuxWidgetProvider.class);
        refreshIntent.setAction(TERMUX_WIDGET_PROVIDER.ACTION_REFRESH_WIDGET);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        refreshIntent.setData(Uri.parse(refreshIntent.toUri(Intent.URI_INTENT_SCHEME)));
        @SuppressLint("UnspecifiedImmutableFlag") // Must be mutable
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);

        // Here we setup the a pending intent template. Individuals items of a collection
        // cannot setup their own pending intents, instead, the collection as a whole can
        // setup a pending intent template, and the individual items can set a fillInIntent
        // to create unique before on an item to item basis.
        Intent toastIntent = new Intent(context, TermuxWidgetProvider.class);
        toastIntent.setAction(TERMUX_WIDGET_PROVIDER.ACTION_WIDGET_ITEM_CLICKED);
        toastIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        @SuppressLint("UnspecifiedImmutableFlag") // Must be mutable
        PendingIntent toastPendingIntent = PendingIntent.getBroadcast(context, 0, toastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setPendingIntentTemplate(R.id.widget_list, toastPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Logger.logDebug(LOG_TAG, "onDeleted");
    }

    @Override
    public void onDisabled(Context context) {
        Logger.logDebug(LOG_TAG, "onDisabled");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (action == null) return;

        Logger.logDebug(LOG_TAG, "onReceive(): " + action);
        Logger.logVerbose(LOG_TAG, "Intent Received\n" + IntentUtils.getIntentString(intent));

        switch (action) {
            case AppWidgetManager.ACTION_APPWIDGET_UPDATE: {
                // The super class already handles this to call onUpdate to update remove views, but
                // we handle this ourselves and call notifyAppWidgetViewDataChanged as well afterwards.
                if (!ShortcutUtils.isTermuxAppAccessible(context, LOG_TAG, false)) return;

                refreshAppWidgets(context, intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS), true);

                return;
            } case TERMUX_WIDGET_PROVIDER.ACTION_WIDGET_ITEM_CLICKED: {
                String clickedFilePath = intent.getStringExtra(TERMUX_WIDGET_PROVIDER.EXTRA_FILE_CLICKED);
                if (clickedFilePath == null || clickedFilePath.isEmpty()) {
                    Logger.logError(LOG_TAG, "Ignoring unset clicked file");
                    return;
                }

                if (FileUtils.getFileType(clickedFilePath, true) == FileType.DIRECTORY) {
                    Logger.logError(LOG_TAG, "Ignoring clicked directory file");
                    return;
                }
                
                sendExecutionIntentToTermuxService(context, clickedFilePath, LOG_TAG);
                return;

            } case TERMUX_WIDGET_PROVIDER.ACTION_REFRESH_WIDGET: {
                if (!ShortcutUtils.isTermuxAppAccessible(context, LOG_TAG, true)) return;

                // Explicitly trigger a forced background sync from Termux!
                TermuxShortcutSyncManager.syncShortcutsFromTermux(context, true);

                int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                int[] appWidgetIds;
                boolean updateRemoteViews = false;
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    appWidgetIds = new int[]{appWidgetId};
                } else {
                    appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, TermuxWidgetProvider.class));
                    Logger.logDebug(LOG_TAG, "Refreshing all widget ids: " + Arrays.toString(appWidgetIds));

                    // Only update remote views if sendIntentToRefreshAllWidgets() is called or if
                    // user sent intent with "am broadcast" command.
                    // A valid id would normally only be sent if refresh button of widget was successfully
                    // pressed and widget was not in a non-responsive state, so no need to update remote views.
                    updateRemoteViews = true;
                }

                List<Integer> updatedAppWidgetIds = refreshAppWidgets(context, appWidgetIds, updateRemoteViews);
                if (updatedAppWidgetIds != null)
                    Logger.logDebugAndShowToast(context, LOG_TAG, context.getString(R.string.msg_widgets_refreshed, Arrays.toString(appWidgetIds)));
                else
                    Logger.logDebugAndShowToast(context, LOG_TAG, context.getString(R.string.msg_no_widgets_found_to_refresh));
                return;

            } default: {
                Logger.logDebug(LOG_TAG, "Unhandled action: " + action);
                break;

            }
        }

        // Allow super to handle other actions
        super.onReceive(context, intent);
    }

    public static List<Integer> refreshAppWidgets(@NonNull Context context, int[] appWidgetIds, boolean updateRemoteViews) {
        if (appWidgetIds == null || appWidgetIds.length == 0) return null;
        List<Integer> updatedAppWidgetIds = new ArrayList<>();
        for (int appWidgetId : appWidgetIds) {
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) continue;
            updatedAppWidgetIds.add(appWidgetId);
            if (updateRemoteViews)
                updateAppWidgetRemoteViews(context, AppWidgetManager.getInstance(context), appWidgetId);

            AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list);
        }

        return updatedAppWidgetIds.size() > 0 ? updatedAppWidgetIds : null;
    }

    public static void sendIntentToRefreshAllWidgets(@NonNull Context context, @NonNull String logTag) {
        Intent intent = new Intent(TERMUX_WIDGET_PROVIDER.ACTION_REFRESH_WIDGET);
        intent.setClass(context, TermuxWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        try {
            Logger.logDebug(logTag, "Sending intent to refresh all widgets");
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Logger.showToast(context, e.getMessage(), true);
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to send intent to refresh all widgets", e);
        }
    }

    /**
     * Extract termux shortcut file path from an intent and send intent to TermuxService to execute it.
     *
     * @param context The {@link Context} that will be used to send execution intent to the TermuxService.
     * @param intent The {@link Intent} received for the shortcut file.
     */
    public static void handleTermuxShortcutExecutionIntent(Context context, Intent intent, String logTag) {
        if (context == null || intent == null) return;
        logTag = DataUtils.getDefaultIfNull(logTag, LOG_TAG);
        String token = intent.getStringExtra(TermuxConstants.TERMUX_WIDGET_APP.EXTRA_TOKEN_NAME);
        if (token == null || !token.equals(TermuxWidgetAppSharedPreferences.getGeneratedToken(context))) {
            Logger.logWarn(logTag, "Invalid token \"" + token + "\" for intent:\n" + IntentUtils.getIntentString(intent));
            Toast.makeText(context, R.string.msg_bad_token, Toast.LENGTH_LONG).show();
            return;
        }

        String target = null;
        if (intent.getData() != null) {
            target = intent.getData().getQueryParameter("path");
            if (target == null) target = intent.getData().getPath();
        }
        if (target != null) {
            sendExecutionIntentToTermuxService(context, target, logTag);
        }
    }

    /**
     * Send execution intent to Termux using the official com.termux.RUN_COMMAND API.
     *
     * @param context The {@link Context} that will be used to send execution intent.
     * @param shortcutFilePath The path to the shortcut file or custom command ID.
     */
    public static void sendExecutionIntentToTermuxService(final Context context, String shortcutFilePath, String logTag) {
        if (context == null || shortcutFilePath == null || shortcutFilePath.isEmpty()) return;
        logTag = DataUtils.getDefaultIfNull(logTag, LOG_TAG);

        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("com.termux", 0);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.logErrorAndShowToast(context, logTag, "Termux app (com.termux) is not installed!");
            return;
        }

        String executablePath;
        String[] arguments = null;
        boolean isBackground = false;

        if (shortcutFilePath.startsWith("custom:")) {
            String cmdId = shortcutFilePath.substring("custom:".length());
            TermuxCustomCommandPreferences.CustomCommand cmd = TermuxCustomCommandPreferences.getCommandById(context, cmdId);
            if (cmd == null) {
                Toast.makeText(context, "Command not found", Toast.LENGTH_SHORT).show();
                return;
            }
            executablePath = "/data/data/com.termux/files/usr/bin/bash";
            arguments = new String[]{"-c", cmd.command};
            isBackground = cmd.isTask;
            if (isBackground) {
                Toast toast = Toast.makeText(context, context.getString(R.string.msg_executing_task, cmd.name), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.show();
            }
        } else {
            File shortcutFile = new File(shortcutFilePath);
            File shortcutParentDirFile = shortcutFile.getParentFile();
            isBackground = (shortcutParentDirFile != null && shortcutParentDirFile.getName().equals(TermuxConstants.TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME));

            if (isBackground) {
                Toast toast = Toast.makeText(context, context.getString(R.string.msg_executing_task, ShellUtils.getExecutableBasename(shortcutFilePath)), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.show();
            }

            if (shortcutFilePath.startsWith("/storage") || shortcutFilePath.startsWith("/sdcard") || shortcutFilePath.contains("/Android/data/")) {
                executablePath = "/data/data/com.termux/files/usr/bin/bash";
                arguments = new String[]{shortcutFilePath};
            } else {
                executablePath = shortcutFilePath;
            }
        }

        Intent runIntent = new Intent("com.termux.RUN_COMMAND");
        runIntent.setClassName("com.termux", "com.termux.app.RunCommandService");
        runIntent.putExtra("com.termux.RUN_COMMAND_PATH", executablePath);
        if (arguments != null) {
            runIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arguments);
        }
        runIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        runIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", isBackground);
        runIntent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");

        Logger.logDebug(logTag, "Sending RUN_COMMAND to Termux for " + executablePath + ", isBackground=" + isBackground);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(runIntent);
            } else {
                context.startService(runIntent);
            }
        } catch (SecurityException se) {
            Toast.makeText(context, "Permission error: Please enable 'Run commands in Termux' in Android Settings -> Apps -> Termux:Widget v2 -> Permissions", Toast.LENGTH_LONG).show();
            Logger.logStackTraceWithMessage(logTag, "SecurityException sending RUN_COMMAND", se);
        } catch (Exception e) {
            Toast.makeText(context, "Failed to launch in Termux: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Logger.logStackTraceWithMessage(logTag, "Failed to send execution intent to Termux", e);
        }
    }

}
