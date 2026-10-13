package com.termux.widget.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.widget.NaturalOrderComparator;
import com.termux.widget.R;
import com.termux.widget.ShortcutFile;
import com.termux.widget.TermuxCustomCommandPreferences;
import com.termux.widget.TermuxWidgetApplication;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShortcutUtils {

    public static final int TERMUX_SHORTCUTS_SCRIPTS_DIR_MAX_SEARCH_DEPTH = 5;

    /* Allowed paths under which shortcut files can exist. */
    public static final List<String> SHORTCUT_FILES_ALLOWED_PATHS_LIST = Arrays.asList(
            TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR_PATH,
            TermuxConstants.TERMUX_DATA_HOME_DIR_PATH);

    /* Allowed paths under which shortcut icons files can exist. */
    public static final List<String> SHORTCUT_ICONS_FILES_ALLOWED_PATHS_LIST = Arrays.asList(
            TermuxConstants.TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH,
            TermuxConstants.TERMUX_DATA_HOME_DIR_PATH);

    public static final FileFilter SHORTCUT_FILES_FILTER = new FileFilter() {
        public boolean accept(File file) {
            if (file.getName().startsWith(".")) {
                // Do not show hidden files starting with a dot.
                return false;
            } else if (!file.exists()) {
                // Do not show broken symlinks
                return false;
            } else if (file.getName().equals(".icons") || file.getName().equals("icons")) {
                return false;
            }
            return true;
        }
    };

    public static void enumerateShortcutFiles(@NonNull Context context, @NonNull List<ShortcutFile> files, boolean sorted) {
        // 1. In-app custom commands from preferences
        List<TermuxCustomCommandPreferences.CustomCommand> customCmds = TermuxCustomCommandPreferences.getCustomCommands(context);
        for (TermuxCustomCommandPreferences.CustomCommand cmd : customCmds) {
            files.add(new ShortcutFile(cmd));
        }

        // 2. Shared storage /sdcard/Termux/shortcuts/
        File sdcardShortcuts = new File(Environment.getExternalStorageDirectory(), "Termux/shortcuts");
        if (sdcardShortcuts.exists() && sdcardShortcuts.canRead()) {
            enumerateShortcutFiles(files, sdcardShortcuts, false, 0);
        }

        // 3. App-specific external storage shortcuts directory
        File appExternalShortcuts = context.getExternalFilesDir("shortcuts");
        if (appExternalShortcuts != null && appExternalShortcuts.exists() && appExternalShortcuts.canRead()) {
            enumerateShortcutFiles(files, appExternalShortcuts, false, 0);
        }

        // 4. Termux internal ~/.shortcuts (if readable)
        if (TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR.exists() && TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR.canRead()) {
            enumerateShortcutFiles(files, TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR, false, 0);
        }

        if (sorted) {
            files.sort((lhs, rhs) -> {
                int groupCmp = lhs.getGroupName().compareToIgnoreCase(rhs.getGroupName());
                if (groupCmp != 0) return groupCmp;
                return NaturalOrderComparator.compare(lhs.getLabel(), rhs.getLabel());
            });
        }
    }

    public static void enumerateShortcutFiles(List<ShortcutFile> files, boolean sorted) {
        Context context = TermuxWidgetApplication.getAppContext();
        if (context != null) {
            enumerateShortcutFiles(context, files, sorted);
        } else {
            enumerateShortcutFiles(files, TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR, sorted, 0);
        }
    }

    public static void enumerateShortcutFiles(List<ShortcutFile> files, File dir, boolean sorted) {
        enumerateShortcutFiles(files, dir, sorted, 0);
    }

    public static void enumerateShortcutFiles(List<ShortcutFile> files, File dir, boolean sorted, int depth) {
        if (depth > TERMUX_SHORTCUTS_SCRIPTS_DIR_MAX_SEARCH_DEPTH) return;

        File[] current_files = dir.listFiles(SHORTCUT_FILES_FILTER);

        if (current_files == null) return;

        if (sorted) {
            Arrays.sort(current_files, (lhs, rhs) -> {
                if (lhs.isDirectory() != rhs.isDirectory()) {
                    return lhs.isDirectory() ? 1 : -1;
                }
                return NaturalOrderComparator.compare(lhs.getName(), rhs.getName());
            });
        }

        for (File file : current_files) {
            if (file.isDirectory()) {
                enumerateShortcutFiles(files, file, sorted, depth + 1);
            } else {
                files.add(new ShortcutFile(file, depth));
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    public static ShortcutManager getShortcutManager(@NonNull Context context, @NonNull String logTag, boolean showErrorToast) {
        ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
        if (shortcutManager == null)  {
            Logger.logErrorAndShowToast(showErrorToast ? context : null, logTag, context.getString(R.string.error_failed_to_get_shortcut_manager));
            return null;
        }
        return shortcutManager;
    }

    public static boolean isTermuxAppAccessible(@NonNull Context context, @NonNull String logTag, boolean showErrorToast) {
        try {
            context.getPackageManager().getPackageInfo("com.termux", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            if (showErrorToast) {
                Logger.logErrorAndShowToast(context, logTag, "Termux application (com.termux) is not installed.");
            }
            return false;
        }
    }

}
