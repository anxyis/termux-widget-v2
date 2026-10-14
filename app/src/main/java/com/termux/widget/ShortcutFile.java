package com.termux.widget;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.termux.widget.theme.ThemeManager;

import com.termux.shared.data.DataUtils;
import com.termux.shared.file.FileUtils;
import com.termux.shared.file.filesystem.FileType;
import com.termux.shared.logger.Logger;
import com.termux.shared.shell.ShellUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;
import com.termux.shared.termux.TermuxConstants.TERMUX_WIDGET_APP;
import com.termux.shared.termux.TermuxConstants.TERMUX_WIDGET_APP.TERMUX_WIDGET_PROVIDER;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.shared.termux.settings.preferences.TermuxWidgetAppSharedPreferences;
import com.termux.widget.icons.IconHelper;
import com.termux.widget.icons.IconRegistry;
import com.termux.widget.utils.ShortcutUtils;

import java.io.File;

public final class ShortcutFile {

    private static final String LOG_TAG = "ShortcutFile";

    public final String mPath;
    public String mLabel;
    public boolean mIsHeader = false;
    public String mGroup;
    public int mGroupCount = 0;
    public String mIconId;
    public boolean mIsCustomCommand = false;
    public String mCustomCommand;
    public boolean mIsTask = false;
    public boolean mIsToggle = false;
    public boolean mToggleIsOn = false;
    public String mToggleId;

    private String mDefaultLabel;

    public ShortcutFile(@NonNull com.termux.widget.toggle.ToggleShortcut toggle) {
        mPath = com.termux.widget.toggle.TermuxTogglePreferences.PREFIX_TOGGLE + toggle.id;
        mLabel = toggle.label;
        mDefaultLabel = toggle.label;
        mGroup = "Toggles";
        mIconId = toggle.iconId;
        mIsToggle = true;
        mToggleId = toggle.id;
        mToggleIsOn = toggle.isOn;
        applyStoredCustomMetadata();
    }

    public ShortcutFile(@NonNull TermuxCustomCommandPreferences.CustomCommand cmd) {
        mPath = "custom:" + cmd.id;
        mLabel = cmd.name;
        mDefaultLabel = cmd.name;
        mGroup = cmd.group;
        mIconId = cmd.iconId;
        mIsTask = cmd.isTask;
        mCustomCommand = cmd.command;
        mIsCustomCommand = true;
        applyStoredCustomMetadata();
    }

    public ShortcutFile(@NonNull String path) {
        this(path, null);
    }

    public ShortcutFile(@NonNull File file) {
        this(file.getAbsolutePath(), null);
    }

    public ShortcutFile(@NonNull File file, int depth) {
        this(file.getAbsolutePath(),
                (depth > 0 && file.getParentFile() != null ? (file.getParentFile().getName() + "/") : "") + file.getName());
    }

    public ShortcutFile(@NonNull String path, @Nullable String defaultLabel) {
        mPath = path;
        mDefaultLabel = getLabelForShortcut(defaultLabel);
        mLabel = mDefaultLabel;
        applyStoredCustomMetadata();
    }

    public void applyStoredCustomMetadata() {
        Context context = TermuxWidgetApplication.getAppContext();
        if (context != null) {
            applyCustomMetadata(context);
        }
    }

    public void applyCustomMetadata(@NonNull Context context) {
        if (mPath == null) return;
        String customLabel = TermuxShortcutMetadataPreferences.getCustomLabel(context, mPath);
        if (customLabel != null && !customLabel.trim().isEmpty()) {
            mLabel = customLabel.trim();
        }
        String customIconId = TermuxShortcutMetadataPreferences.getCustomIconId(context, mPath);
        if (customIconId != null && !customIconId.trim().isEmpty()) {
            mIconId = customIconId.trim();
        }
        String customGroup = TermuxShortcutMetadataPreferences.getCustomGroup(context, mPath);
        if (customGroup != null && !customGroup.trim().isEmpty()) {
            mGroup = customGroup.trim();
        }
    }

    @NonNull
    public String getOriginalLabel() {
        return mDefaultLabel != null ? mDefaultLabel : (mLabel != null ? mLabel : "");
    }

    /** Creates a virtual header item for grouping inside collection widgets. */
    public static ShortcutFile createGroupHeader(@NonNull String groupName, int count) {
        ShortcutFile header = new ShortcutFile("HEADER:" + groupName, groupName);
        header.mIsHeader = true;
        header.mGroup = groupName;
        header.mGroupCount = count;
        return header;
    }

    public boolean isHeader() {
        return mIsHeader;
    }

    public boolean isToggle() {
        return mIsToggle;
    }

    public boolean isTask() {
        if (mIsCustomCommand) {
            return mIsTask;
        }
        if (mPath == null) return false;
        File file = new File(mPath);
        File parent = file.getParentFile();
        while (parent != null) {
            if (parent.getName().equals(TermuxConstants.TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME)) {
                return true;
            }
            parent = parent.getParentFile();
        }
        return false;
    }

    @NonNull
    public String getGroupName() {
        if (mGroup != null) return mGroup;
        if (isTask()) return "Tasks";
        if (mPath == null) return "General";
        File file = new File(mPath);
        File parent = file.getParentFile();
        if (parent != null && !parent.getAbsolutePath().equals(TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR_PATH)) {
            return parent.getName();
        }
        return "General";
    }

    @NonNull
    public String getSubtitle() {
        if (mIsCustomCommand) {
            return mCustomCommand != null ? mCustomCommand : "";
        }
        if (mPath == null) return "";
        File file = new File(mPath);
        File parent = file.getParentFile();
        if (parent != null) {
            return parent.getName();
        }
        return "shortcuts";
    }

    @NonNull
    public String getPath() {
        return mPath;
    }

    @NonNull
    public String getCanonicalPath() {
        if (mIsCustomCommand) return getPath();
        return FileUtils.getCanonicalPath(getPath(), null);
    }

    @NonNull
    public String getUnExpandedPath() {
        if (mIsCustomCommand) {
            return mCustomCommand != null ? mCustomCommand : mLabel;
        }
        return TermuxFileUtils.getUnExpandedTermuxPath(getCanonicalPath());
    }

    @NonNull
    public String getLabel() {
        return mLabel;
    }

    @NonNull
    public String getLabelForShortcut(@Nullable String defaultLabel) {
        if (!DataUtils.isNullOrEmpty(defaultLabel))
            return defaultLabel;
        if (mPath == null) return "";
        String shortcutsDir = TermuxConstants.TERMUX_SHORTCUT_SCRIPTS_DIR_PATH;
        if (mPath.startsWith(shortcutsDir)) {
            String rel = mPath.substring(shortcutsDir.length());
            while (rel.startsWith("/")) rel = rel.substring(1);
            if (!rel.isEmpty()) return rel;
        }
        File file = new File(mPath);
        File parent = file.getParentFile();
        if (parent != null && !parent.getName().equals(".shortcuts") && !parent.getName().equals("shortcuts")) {
            return parent.getName() + "/" + file.getName();
        }
        return file.getName();
    }

    public Intent getExecutionIntent(Context context) {
        Uri scriptUri = new Uri.Builder()
                .scheme("termux-widget")
                .authority("execute")
                .appendQueryParameter("path", getPath())
                .build();
        Intent executionIntent = new Intent(context, TermuxLaunchShortcutActivity.class);
        executionIntent.setAction(Intent.ACTION_VIEW);
        executionIntent.setData(scriptUri);
        executionIntent.putExtra(TERMUX_WIDGET_APP.EXTRA_TOKEN_NAME, TermuxWidgetAppSharedPreferences.getGeneratedToken(context));
        return executionIntent;
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    public ShortcutInfo getShortcutInfo(Context context, boolean showToastForIconUsed) {
        ShortcutInfo.Builder builder = new ShortcutInfo.Builder(context, getPath());
        builder.setIntent(getExecutionIntent(context));
        builder.setShortLabel(getLabel());

        File shortcutIconFile = getIconFile(context, showToastForIconUsed);
        if (shortcutIconFile != null) {
            builder.setIcon(Icon.createWithBitmap(((BitmapDrawable) Drawable.createFromPath(shortcutIconFile.getAbsolutePath())).getBitmap()));
        } else if (IconHelper.isCustomFile(mIconId)) {
            android.graphics.Bitmap bmp = IconHelper.loadCustomBitmap(mIconId);
            if (bmp != null) {
                builder.setIcon(Icon.createWithBitmap(bmp));
            } else {
                builder.setIcon(Icon.createWithResource(context, R.drawable.ic_launcher));
            }
        } else if (mIconId != null) {
            int resId = IconRegistry.getDrawableResId(mIconId, R.drawable.ic_launcher);
            builder.setIcon(Icon.createWithResource(context, resId));
        } else {
            builder.setIcon(Icon.createWithResource(context, R.drawable.ic_launcher));
        }

        return builder.build();
    }

    public Intent getStaticShortcutIntent(Context context) {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, getExecutionIntent(context));
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getLabel());

        File shortcutIconFile = getIconFile(context, true);
        if (shortcutIconFile != null) {
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, ((BitmapDrawable) Drawable.createFromPath(shortcutIconFile.getAbsolutePath())).getBitmap());
        } else if (IconHelper.isCustomFile(mIconId)) {
            android.graphics.Bitmap bmp = IconHelper.loadCustomBitmap(mIconId);
            if (bmp != null) {
                intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bmp);
            } else {
                intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_launcher));
            }
        } else {
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_launcher));
        }

        return intent;
    }

    public RemoteViews getListWidgetView(Context context) {
        return getListWidgetView(context, 0, 1);
    }

    public RemoteViews getListWidgetView(Context context, int position, int count) {
        if (context != null) applyCustomMetadata(context);
        boolean isAmoled = ThemeManager.isAmoledMode(context);

        if (mIsHeader) {
            RemoteViews headerViews = new RemoteViews(context.getPackageName(), R.layout.widget_group_header);
            headerViews.setTextViewText(R.id.widget_group_header_title, getLabel());
            headerViews.setTextViewText(R.id.widget_group_header_count, String.valueOf(mGroupCount));
            if (isAmoled) {
                headerViews.setTextColor(R.id.widget_group_header_title, Color.WHITE);
                headerViews.setInt(R.id.widget_group_header_count, "setBackgroundResource", R.drawable.widget_pill_badge_amoled_bg);
                headerViews.setTextColor(R.id.widget_group_header_count, Color.WHITE);
            }
            return headerViews;
        }

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_item);
        remoteViews.setTextViewText(R.id.widget_item, getLabel());

        int cornerRadius = TermuxWidgetStylePreferences.getCornerRadiusDp(context);
        int textSizeSp = TermuxWidgetStylePreferences.getTextSizeSp(context);
        int iconSizeDp = TermuxWidgetStylePreferences.getIconSizeDp(context);

        remoteViews.setTextViewTextSize(R.id.widget_item, TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            remoteViews.setViewLayoutWidth(R.id.widget_item_icon, iconSizeDp, TypedValue.COMPLEX_UNIT_DIP);
            remoteViews.setViewLayoutHeight(R.id.widget_item_icon, iconSizeDp, TypedValue.COMPLEX_UNIT_DIP);
        }

        boolean isTaskScript = isTask();
        int defaultIcon = isTaskScript ? R.drawable.ic_hero_bolt : R.drawable.ic_lucide_terminal;
        boolean isToggleOn = mIsToggle && mToggleIsOn;

        int cardBgRes = WidgetBackgroundHelper.getListCardBackground(isAmoled, isToggleOn, cornerRadius);
        remoteViews.setInt(R.id.widget_item_card_container, "setBackgroundResource", cardBgRes);

        if (isAmoled) {
            if (mIsToggle) {
                if (mToggleIsOn) {
                    remoteViews.setInt(R.id.widget_item_icon_badge, "setBackgroundResource", R.drawable.widget_toggle_badge_amoled_on_bg);
                    remoteViews.setTextColor(R.id.widget_item, Color.BLACK);
                    IconHelper.applyToRemoteViews(remoteViews, R.id.widget_item_icon, R.id.widget_item_custom_image, mIconId, defaultIcon);
                    remoteViews.setInt(R.id.widget_item_icon, "setColorFilter", Color.WHITE);
                } else {
                    remoteViews.setInt(R.id.widget_item_icon_badge, "setBackgroundResource", R.drawable.widget_list_circle_icon_amoled_bg);
                    remoteViews.setTextColor(R.id.widget_item, Color.WHITE);
                    IconHelper.applyToRemoteViews(remoteViews, R.id.widget_item_icon, R.id.widget_item_custom_image, mIconId, defaultIcon);
                    remoteViews.setInt(R.id.widget_item_icon, "setColorFilter", Color.WHITE);
                }
            } else {
                remoteViews.setInt(R.id.widget_item_icon_badge, "setBackgroundResource", R.drawable.widget_list_circle_icon_amoled_bg);
                remoteViews.setTextColor(R.id.widget_item, Color.WHITE);
                IconHelper.applyToRemoteViews(remoteViews, R.id.widget_item_icon, R.id.widget_item_custom_image, mIconId, defaultIcon);
                remoteViews.setInt(R.id.widget_item_icon, "setColorFilter", Color.WHITE);
            }
        } else {
            if (mIsToggle) {
                if (mToggleIsOn) {
                    remoteViews.setInt(R.id.widget_item_icon_badge, "setBackgroundResource", R.drawable.widget_toggle_badge_on_bg);
                    remoteViews.setTextColor(R.id.widget_item, context.getColor(R.color.widget_toggle_text_on));
                    IconHelper.applyToRemoteViews(remoteViews, R.id.widget_item_icon, R.id.widget_item_custom_image, mIconId, defaultIcon);
                    remoteViews.setInt(R.id.widget_item_icon, "setColorFilter", context.getColor(R.color.widget_toggle_icon_on));
                } else {
                    remoteViews.setInt(R.id.widget_item_icon_badge, "setBackgroundResource", R.drawable.widget_list_circle_icon_bg);
                    remoteViews.setTextColor(R.id.widget_item, context.getColor(R.color.widget_text_color));
                    IconHelper.applyToRemoteViews(remoteViews, R.id.widget_item_icon, R.id.widget_item_custom_image, mIconId, defaultIcon);
                    remoteViews.setInt(R.id.widget_item_icon, "setColorFilter", context.getColor(R.color.widget_list_badge_icon));
                }
            } else {
                IconHelper.applyToRemoteViews(remoteViews, R.id.widget_item_icon, R.id.widget_item_custom_image, mIconId, defaultIcon);
            }
        }

        Intent fillInIntent = new Intent().putExtra(TERMUX_WIDGET_PROVIDER.EXTRA_FILE_CLICKED, getPath());
        remoteViews.setOnClickFillInIntent(R.id.widget_item_layout, fillInIntent);

        return remoteViews;
    }

    public RemoteViews getGridWidgetView(Context context) {
        if (context != null) applyCustomMetadata(context);
        boolean isAmoled = ThemeManager.isAmoledMode(context);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_grid_item);
        remoteViews.setTextViewText(R.id.widget_grid_item_label, getLabel());

        int cornerRadius = TermuxWidgetStylePreferences.getCornerRadiusDp(context);
        int textSizeSp = TermuxWidgetStylePreferences.getTextSizeSp(context);
        int iconSizeDp = TermuxWidgetStylePreferences.getIconSizeDp(context);

        remoteViews.setTextViewTextSize(R.id.widget_grid_item_label, TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            remoteViews.setViewLayoutWidth(R.id.widget_grid_item_icon, iconSizeDp, TypedValue.COMPLEX_UNIT_DIP);
            remoteViews.setViewLayoutHeight(R.id.widget_grid_item_icon, iconSizeDp, TypedValue.COMPLEX_UNIT_DIP);
        }

        boolean isTaskScript = isTask();
        int defaultIcon = isTaskScript ? R.drawable.ic_hero_bolt : R.drawable.ic_lucide_terminal;
        boolean isToggleOn = mIsToggle && mToggleIsOn;

        int gridBgRes = WidgetBackgroundHelper.getGridCardBackground(isAmoled, isToggleOn, cornerRadius);
        remoteViews.setInt(R.id.widget_grid_item_layout, "setBackgroundResource", gridBgRes);

        if (isAmoled) {
            if (mIsToggle) {
                if (mToggleIsOn) {
                    remoteViews.setInt(R.id.widget_grid_item_icon_badge, "setBackgroundResource", R.drawable.widget_toggle_badge_amoled_on_bg);
                    remoteViews.setTextColor(R.id.widget_grid_item_label, Color.BLACK);
                    remoteViews.setInt(R.id.widget_grid_item_icon, "setColorFilter", Color.WHITE);
                } else {
                    remoteViews.setInt(R.id.widget_grid_item_icon_badge, "setBackgroundResource", R.drawable.widget_grid_circle_amoled_bg);
                    remoteViews.setTextColor(R.id.widget_grid_item_label, Color.WHITE);
                    remoteViews.setInt(R.id.widget_grid_item_icon, "setColorFilter", Color.BLACK);
                }
            } else {
                remoteViews.setInt(R.id.widget_grid_item_icon_badge, "setBackgroundResource", R.drawable.widget_grid_circle_amoled_bg);
                remoteViews.setTextColor(R.id.widget_grid_item_label, Color.WHITE);
                remoteViews.setInt(R.id.widget_grid_item_icon, "setColorFilter", Color.BLACK);
            }
            IconHelper.applyToRemoteViews(remoteViews, R.id.widget_grid_item_icon, R.id.widget_grid_item_custom_image, mIconId, defaultIcon);
        } else {
            if (mIsToggle) {
                if (mToggleIsOn) {
                    remoteViews.setTextColor(R.id.widget_grid_item_label, context.getColor(R.color.widget_toggle_text_on));
                } else {
                    remoteViews.setTextColor(R.id.widget_grid_item_label, context.getColor(R.color.widget_text_color));
                }
            }
            IconHelper.applyToRemoteViews(remoteViews, R.id.widget_grid_item_icon, R.id.widget_grid_item_custom_image, mIconId, defaultIcon);
        }

        Intent fillInIntent = new Intent().putExtra(TERMUX_WIDGET_PROVIDER.EXTRA_FILE_CLICKED, getPath());
        remoteViews.setOnClickFillInIntent(R.id.widget_grid_item_layout, fillInIntent);

        return remoteViews;
    }

    @Nullable
    private File getIconFile(Context context, boolean showToastForIconUsed) {
        String shortcutIconFilePath = TermuxConstants.TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH +
                "/" + ShellUtils.getExecutableBasename(getPath()) + ".png";

        FileType fileType = FileUtils.getFileType(shortcutIconFilePath, true);
        if (fileType != FileType.REGULAR) {
            return null;
        }

        if (!FileUtils.isPathInDirPaths(shortcutIconFilePath, ShortcutUtils.SHORTCUT_ICONS_FILES_ALLOWED_PATHS_LIST, true)) {
            return null;
        }

        return new File(shortcutIconFilePath);
    }
}
