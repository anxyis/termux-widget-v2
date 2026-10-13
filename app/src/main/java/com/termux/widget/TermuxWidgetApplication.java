package com.termux.widget;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.settings.preferences.TermuxWidgetAppSharedPreferences;
import com.termux.shared.termux.theme.TermuxThemeUtils;

public class TermuxWidgetApplication extends Application {

    public static final String LOG_TAG = "TermuxWidgetApplication";
    private static Context sContext;

    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        Context context = sContext;

        // Set crash handler for the app
        TermuxCrashUtils.setCrashHandler(context);

        // Set log config for the app
        setLogConfig(context, true);

        // Set NightMode.APP_NIGHT_MODE
        TermuxThemeUtils.setAppNightMode(context);

        // Apply Material You dynamic colors based on user wallpaper (Android 12+) unless AMOLED mode is active
        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(
                this,
                new com.google.android.material.color.DynamicColorsOptions.Builder()
                        .setPrecondition((activity, theme) -> !com.termux.widget.theme.ThemeManager.isAmoledMode(activity))
                        .build()
        );
    }

    public static Context getAppContext() {
        return sContext;
    }

    public static void setLogConfig(Context context, boolean commitToFile) {
        Logger.setDefaultLogTag(TermuxConstants.TERMUX_WIDGET_APP_NAME.replaceAll("[: ]", ""));

        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        TermuxWidgetAppSharedPreferences preferences = TermuxWidgetAppSharedPreferences.build(context);
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel(true), commitToFile);
    }

}
