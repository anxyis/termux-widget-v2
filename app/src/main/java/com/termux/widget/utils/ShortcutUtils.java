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
import com.termux.widget.TermuxShortcutSyncManager;
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
        List<ShortcutFile> tempList = new ArrayList<>();

        // 1. Direct Termux internal ~/.shortcuts (if readable directly)
        if (TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR.exists() && TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR.canRead()) {
            enumerateShortcutFiles(tempList, TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR, false, 0);
        }

        // 2. Synced paths from Termux ~/.shortcuts (populated via RUN_COMMAND query)
        if (tempList.isEmpty()) {
            List<String> syncedPaths = TermuxShortcutSyncManager.getSyncedPaths(context);
            for (String p : syncedPaths) {
                tempList.add(new ShortcutFile(p));
            }
        }

        // 3. Shared storage /sdcard/Termux/shortcuts/
        File sdcardShortcuts = new File(Environment.getExternalStorageDirectory(), "Termux/shortcuts");
        if (sdcardShortcuts.exists() && sdcardShortcuts.canRead()) {
            enumerateShortcutFiles(tempList, sdcardShortcuts, false, 0);
        }

        // 4. App-specific external storage shortcuts directory
        File appExternalShortcuts = context.getExternalFilesDir("shortcuts");
        if (appExternalShortcuts != null && appExternalShortcuts.exists() && appExternalShortcuts.canRead()) {
            enumerateShortcutFiles(tempList, appExternalShortcuts, false, 0);
        }

        // 5. In-app custom commands (if manually created by user, excluding legacy mock commands)
        List<TermuxCustomCommandPreferences.CustomCommand> customCmds = TermuxCustomCommandPreferences.getCustomCommands(context);
        for (TermuxCustomCommandPreferences.CustomCommand cmd : customCmds) {
            if (cmd.id != null && isLegacyDummyCommand(cmd.id)) continue;
            tempList.add(new ShortcutFile(cmd));
        }

        // Deduplicate by path
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (ShortcutFile sf : tempList) {
            if (sf.getPath() != null && seen.add(sf.getPath())) {
                files.add(sf);
            }
        }

        if (sorted) {
            files.sort((lhs, rhs) -> NaturalOrderComparator.compare(lhs.getLabel(), rhs.getLabel()));
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

    private static boolean isLegacyDummyCommand(String id) {
        return "pkg_update".equals(id) || "htop".equals(id) || "neofetch".equals(id)
                || "sshd_start".equals(id) || "sshd_stop".equals(id)
                || "http_server".equals(id) || "git_status".equals(id);
    }

}
