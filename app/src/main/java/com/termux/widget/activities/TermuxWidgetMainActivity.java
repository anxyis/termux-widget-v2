package com.termux.widget.activities;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.widget.R;
import com.termux.widget.ScriptInfoBottomSheet;
import com.termux.widget.ShortcutAppearanceBottomSheet;
import com.termux.widget.ShortcutFile;
import com.termux.widget.TermuxCustomCommandPreferences;
import com.termux.widget.TermuxCustomCommandPreferences.CustomCommand;
import com.termux.widget.TermuxShortcutSyncManager;
import com.termux.widget.TermuxWidgetApplication;
import com.termux.widget.TermuxWidgetProvider;
import com.termux.widget.icons.IconPickerBottomSheet;
import com.termux.widget.icons.IconRegistry;
import com.termux.widget.tiles.TermuxTilePreferences;
import com.termux.widget.utils.ShortcutUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TermuxWidgetMainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "TermuxWidgetMainActivity";

    public static final String TERMUX_WIDGET_DATA_HOME_DIR_PATH = TermuxConstants.TERMUX_DATA_HOME_DIR_PATH + "/widget";
    public static final String TERMUX_WIDGET_DYNAMIC_SHORTCUTS_DIR_PATH = TERMUX_WIDGET_DATA_HOME_DIR_PATH + "/dynamic_shortcuts";

    private GridLayout mGridQuickTiles;
    private LinearLayout mContainerDetectedShortcuts;
    private TextView mTxtStatusTitle;
    private TextView mTxtStatusSubtitle;
    private TextView mTxtShortcutsCount;
    private TextView mTxtTilesSummary;
    private LinearLayout mContainerTilesExpandable;
    private ImageView mImgTilesExpandIcon;
    private boolean mTilesExpanded = false;

    private final BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            populateDashboard();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply Material You dynamic colors if available on Android 12+
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);

        Logger.logDebug(LOG_TAG, "onCreate");

        setContentView(R.layout.activity_termux_widget_main);

        MaterialToolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        mGridQuickTiles = findViewById(R.id.grid_quick_tiles);
        mContainerDetectedShortcuts = findViewById(R.id.container_detected_shortcuts);
        mTxtStatusTitle = findViewById(R.id.txt_status_title);
        mTxtStatusSubtitle = findViewById(R.id.txt_status_subtitle);
        mTxtShortcutsCount = findViewById(R.id.txt_shortcuts_count);
        mTxtTilesSummary = findViewById(R.id.txt_tiles_summary);
        mContainerTilesExpandable = findViewById(R.id.container_tiles_expandable);
        mImgTilesExpandIcon = findViewById(R.id.img_tiles_expand_icon);

        View headerTiles = findViewById(R.id.header_quick_tiles);
        if (headerTiles != null) {
            headerTiles.setOnClickListener(v -> toggleTilesExpansion());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission("com.termux.permission.RUN_COMMAND") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"com.termux.permission.RUN_COMMAND"}, 1001);
            }
        }

        findViewById(R.id.btn_refresh_shortcuts).setOnClickListener(v -> {
            TermuxShortcutSyncManager.syncShortcutsFromTermux(this, true);
            Toast.makeText(this, "Refreshing shortcuts from Termux...", Toast.LENGTH_SHORT).show();
            populateDashboard();
        });
        findViewById(R.id.button_refresh_all_widgets).setOnClickListener(v -> sendIntentToRefreshAllWidgets());

        View btnAddCmd = findViewById(R.id.btn_add_command);
        if (btnAddCmd != null) {
            btnAddCmd.setOnClickListener(v -> showAddEditCommandDialog(null));
        }

        View btnCopy = findViewById(R.id.btn_copy_setup_command);
        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Termux Setup", "echo \"allow-external-apps = true\" >> ~/.termux/termux.properties && termux-reload-settings");
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Setup command copied! Paste & run inside Termux", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnSettings = findViewById(R.id.btn_open_app_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        }

        sendIntentToRefreshAllWidgets();
    }

    private void toggleTilesExpansion() {
        mTilesExpanded = !mTilesExpanded;
        if (mContainerTilesExpandable != null) {
            mContainerTilesExpandable.setVisibility(mTilesExpanded ? View.VISIBLE : View.GONE);
        }
        if (mImgTilesExpandIcon != null) {
            mImgTilesExpandIcon.setRotation(mTilesExpanded ? 90f : 0f);
        }
        populateQuickTiles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TermuxWidgetApplication.setLogConfig(this, false);
        Logger.logVerbose(LOG_TAG, "onResume");
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mSyncReceiver, new IntentFilter("com.termux.widget.v2.SHORTCUTS_UPDATED"), Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(mSyncReceiver, new IntentFilter("com.termux.widget.v2.SHORTCUTS_UPDATED"));
            }
        } catch (Exception ignored) {}
        if (TermuxShortcutSyncManager.getSyncedPaths(this).isEmpty()) {
            TermuxShortcutSyncManager.syncShortcutsFromTermux(this, false);
        }
        populateDashboard();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mSyncReceiver);
        } catch (Exception ignored) {}
    }

    private void populateDashboard() {
        checkTermuxStatus();
        populateQuickTiles();
        populateDetectedShortcuts();
    }

    private void checkTermuxStatus() {
        boolean accessible = ShortcutUtils.isTermuxAppAccessible(this, LOG_TAG, false);
        List<ShortcutFile> files = new ArrayList<>();
        ShortcutUtils.enumerateShortcutFiles(this, files, false);

        if (!accessible) {
            mTxtStatusTitle.setText("Termux Disconnected");
            mTxtStatusSubtitle.setText("Termux app (com.termux) not detected");
        } else {
            mTxtStatusTitle.setText("Connected to Termux");
            mTxtStatusSubtitle.setText("~/.shortcuts/ • " + files.size() + " shortcuts loaded");
        }
    }

    private void populateQuickTiles() {
        if (mGridQuickTiles == null) return;
        mGridQuickTiles.removeAllViews();

        int configuredCount = 0;
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int slot = 1; slot <= 8; slot++) {
            final int slotIndex = slot;
            View slotView = inflater.inflate(R.layout.item_main_tile_slot, mGridQuickTiles, false);

            TextView txtSlotNum = slotView.findViewById(R.id.slot_number_badge);
            TextView txtLabel = slotView.findViewById(R.id.slot_tile_label);
            TextView txtStatus = slotView.findViewById(R.id.slot_tile_status);
            ImageView imgIcon = slotView.findViewById(R.id.slot_tile_icon);

            txtSlotNum.setText(String.format("%02d", slot));

            String scriptPath = TermuxTilePreferences.getScriptPath(this, slot);
            String label = TermuxTilePreferences.getLabel(this, slot);
            String iconId = TermuxTilePreferences.getIconId(this, slot);

            if (scriptPath != null && !scriptPath.isEmpty()) {
                configuredCount++;
                txtLabel.setText(label != null ? label : ("Tile " + slot));
                txtStatus.setText("Ready");
                int iconRes = IconRegistry.getDrawableResId(iconId, R.drawable.ic_lucide_terminal);
                imgIcon.setImageResource(iconRes);
            } else {
                txtLabel.setText("Unassigned");
                txtStatus.setText("Tap to setup");
                imgIcon.setImageResource(R.drawable.ic_lucide_terminal);
            }

            slotView.setOnClickListener(v -> {
                Intent intent = new Intent(this, TermuxTileConfigActivity.class);
                intent.putExtra(TermuxTileConfigActivity.EXTRA_SLOT_INDEX, slotIndex);
                startActivity(intent);
            });

            mGridQuickTiles.addView(slotView);
        }

        if (mTxtTilesSummary != null) {
            mTxtTilesSummary.setText(configuredCount + " of 8 tiles configured • Tap to " + (mTilesExpanded ? "collapse" : "view & edit"));
        }
    }

    private void populateDetectedShortcuts() {
        if (mContainerDetectedShortcuts == null) return;
        mContainerDetectedShortcuts.removeAllViews();

        List<ShortcutFile> files = new ArrayList<>();
        ShortcutUtils.enumerateShortcutFiles(this, files, true);

        if (mTxtShortcutsCount != null) {
            mTxtShortcutsCount.setText(files.size() + " script" + (files.size() == 1 ? "" : "s") + " detected • Tap to run or view options");
        }

        if (files.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText(R.string.msg_no_shortcut_scripts);
            emptyText.setTextSize(13);
            emptyText.setPadding(0, 16, 0, 16);
            mContainerDetectedShortcuts.addView(emptyText);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ShortcutFile sf : files) {
            View row = inflater.inflate(R.layout.item_main_shortcut_row, mContainerDetectedShortcuts, false);

            TextView txtTitle = row.findViewById(R.id.row_shortcut_title);
            TextView txtPath = row.findViewById(R.id.row_shortcut_path);
            TextView txtBadge = row.findViewById(R.id.row_shortcut_badge);
            ImageView imgIcon = row.findViewById(R.id.row_shortcut_icon);
            MaterialButton btnRun = row.findViewById(R.id.row_btn_run);

            txtTitle.setText(sf.getLabel());
            txtPath.setText(sf.getUnExpandedPath());

            boolean isTask = sf.isTask();
            txtBadge.setText(isTask ? "TASK" : "TERM");
            txtBadge.setBackgroundResource(isTask ? R.drawable.widget_pill_badge_bg : R.drawable.widget_pill_session_bg);
            txtBadge.setTextColor(getResources().getColor(isTask ? R.color.badge_task_text : R.color.badge_session_text));

            int defaultIcon = isTask ? R.drawable.ic_hero_bolt : R.drawable.ic_lucide_terminal;
            int iconRes = sf.mIconId != null ? IconRegistry.getDrawableResId(sf.mIconId, defaultIcon) : defaultIcon;
            imgIcon.setImageResource(iconRes);

            btnRun.setOnClickListener(v -> {
                TermuxWidgetProvider.sendExecutionIntentToTermuxService(this, sf.getPath(), LOG_TAG);
                Toast.makeText(this, "Executing: " + sf.getLabel(), Toast.LENGTH_SHORT).show();
            });

            row.setOnClickListener(v -> {
                ScriptInfoBottomSheet sheet = ScriptInfoBottomSheet.newInstance(sf);
                sheet.setScriptActionListener(new ScriptInfoBottomSheet.ScriptActionListener() {
                    @Override
                    public void onEditCommand(@NonNull ShortcutFile shortcutFile) {
                        String cmdId = shortcutFile.getPath().replace("custom:", "");
                        CustomCommand cmd = TermuxCustomCommandPreferences.getCommandById(TermuxWidgetMainActivity.this, cmdId);
                        if (cmd != null) {
                            showAddEditCommandDialog(cmd);
                        }
                    }

                    @Override
                    public void onDeleteCommand(@NonNull ShortcutFile shortcutFile) {
                        String cmdId = shortcutFile.getPath().replace("custom:", "");
                        CustomCommand cmd = TermuxCustomCommandPreferences.getCommandById(TermuxWidgetMainActivity.this, cmdId);
                        if (cmd != null) {
                            new MaterialAlertDialogBuilder(TermuxWidgetMainActivity.this)
                                    .setTitle("Delete Command")
                                    .setMessage("Are you sure you want to delete '" + cmd.name + "'?")
                                    .setPositiveButton("Delete", (d, w) -> {
                                        TermuxCustomCommandPreferences.deleteCustomCommand(TermuxWidgetMainActivity.this, cmd.id);
                                        populateDashboard();
                                        sendIntentToRefreshAllWidgets();
                                        Toast.makeText(TermuxWidgetMainActivity.this, "Command deleted", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }
                    }

                    @Override
                    public void onTileAssigned() {
                        populateQuickTiles();
                    }

                    @Override
                    public void onAppearanceChanged() {
                        populateDashboard();
                    }
                });
                sheet.show(getSupportFragmentManager(), "script_info_sheet");
            });

            row.setOnLongClickListener(v -> {
                if (sf.mIsCustomCommand) {
                    showCommandOptionsDialog(sf);
                } else {
                    ShortcutAppearanceBottomSheet appearanceSheet = ShortcutAppearanceBottomSheet.newInstance(sf);
                    appearanceSheet.setOnAppearanceSavedListener((p, newLabel, newIconId) -> populateDashboard());
                    appearanceSheet.show(getSupportFragmentManager(), "shortcut_appearance");
                }
                return true;
            });

            mContainerDetectedShortcuts.addView(row);
        }
    }

    private void showCommandOptionsDialog(@NonNull ShortcutFile sf) {
        String cmdId = sf.getPath().replace("custom:", "");
        CustomCommand cmd = TermuxCustomCommandPreferences.getCommandById(this, cmdId);
        if (cmd == null) return;

        CharSequence[] options = new CharSequence[]{"Run Command", "Edit Command", "Delete Command"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(cmd.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        TermuxWidgetProvider.sendExecutionIntentToTermuxService(this, sf.getPath(), LOG_TAG);
                    } else if (which == 1) {
                        showAddEditCommandDialog(cmd);
                    } else if (which == 2) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Delete Command")
                                .setMessage("Are you sure you want to delete '" + cmd.name + "'?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    TermuxCustomCommandPreferences.deleteCustomCommand(this, cmd.id);
                                    populateDashboard();
                                    sendIntentToRefreshAllWidgets();
                                    Toast.makeText(this, "Command deleted", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .show();
    }

    private void showAddEditCommandDialog(@Nullable CustomCommand existingCmd) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText inputName = new EditText(this);
        inputName.setHint("Command Name (e.g. Update Packages)");
        if (existingCmd != null) inputName.setText(existingCmd.name);
        layout.addView(inputName);

        EditText inputCommand = new EditText(this);
        inputCommand.setHint("Bash Command (e.g. htop or pkg update)");
        if (existingCmd != null) inputCommand.setText(existingCmd.command);
        layout.addView(inputCommand);

        EditText inputGroup = new EditText(this);
        inputGroup.setHint("Group / Category (e.g. System, Dev)");
        if (existingCmd != null) inputGroup.setText(existingCmd.group);
        else inputGroup.setText("Commands");
        layout.addView(inputGroup);

        CheckBox chkTask = new CheckBox(this);
        chkTask.setText("Run in background (Task / Silent)");
        if (existingCmd != null) chkTask.setChecked(existingCmd.isTask);
        layout.addView(chkTask);

        final String[] selectedIconId = new String[]{existingCmd != null ? existingCmd.iconId : "lucide_terminal"};
        MaterialButton btnPickIcon = new MaterialButton(this);
        btnPickIcon.setText("Select Icon: " + selectedIconId[0]);
        btnPickIcon.setIconResource(IconRegistry.getDrawableResId(selectedIconId[0], R.drawable.ic_lucide_terminal));
        btnPickIcon.setOnClickListener(v -> {
            IconPickerBottomSheet picker = IconPickerBottomSheet.newInstance();
            picker.setOnIconSelectedListener(iconId -> {
                selectedIconId[0] = iconId;
                IconRegistry.IconItem item = IconRegistry.getIcon(iconId);
                String iconName = item != null ? item.name : iconId;
                btnPickIcon.setText("Select Icon: " + iconName);
                btnPickIcon.setIconResource(IconRegistry.getDrawableResId(iconId, R.drawable.ic_lucide_terminal));
            });
            picker.show(getSupportFragmentManager(), "command_icon_picker");
        });
        layout.addView(btnPickIcon);

        new MaterialAlertDialogBuilder(this)
                .setTitle(existingCmd != null ? "Edit Command" : "New Custom Command")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = inputName.getText().toString().trim();
                    String command = inputCommand.getText().toString().trim();
                    String group = inputGroup.getText().toString().trim();
                    if (name.isEmpty() || command.isEmpty()) {
                        Toast.makeText(this, "Name and Command cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String id = existingCmd != null ? existingCmd.id : ("cmd_" + System.currentTimeMillis());
                    CustomCommand cmd = new CustomCommand(id, name, command, chkTask.isChecked(), selectedIconId[0], group);
                    TermuxCustomCommandPreferences.saveCustomCommand(this, cmd);
                    populateDashboard();
                    sendIntentToRefreshAllWidgets();
                    Toast.makeText(this, "Saved " + name, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void sendIntentToRefreshAllWidgets() {
        TermuxWidgetProvider.sendIntentToRefreshAllWidgets(this, LOG_TAG);
        Toast.makeText(this, "Widgets Refreshed", Toast.LENGTH_SHORT).show();
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private void createDynamicShortcuts(@NonNull Context context) {
        ShortcutManager shortcutManager = ShortcutUtils.getShortcutManager(context, LOG_TAG, true);
        if (shortcutManager == null) return;

        Error error = FileUtils.createDirectoryFile(TERMUX_WIDGET_DYNAMIC_SHORTCUTS_DIR_PATH);
        if (error != null) {
            Logger.logError(LOG_TAG, error.toString());
            Logger.showToast(this, error.getMinimalErrorLogString(), true);
        }

        List<ShortcutFile> shortcutFiles = new ArrayList<>();
        ShortcutUtils.enumerateShortcutFiles(shortcutFiles, new File(TERMUX_WIDGET_DYNAMIC_SHORTCUTS_DIR_PATH), false);

        if (shortcutFiles.isEmpty()) {
            Logger.showToast(context, getString(R.string.msg_no_shortcut_files_found_in_directory,
                    TermuxFileUtils.getUnExpandedTermuxPath(TERMUX_WIDGET_DYNAMIC_SHORTCUTS_DIR_PATH)), true);
            return;
        }

        List<ShortcutInfo> shortcuts = new ArrayList<>();
        for (ShortcutFile shortcutFile : shortcutFiles) {
            shortcuts.add(shortcutFile.getShortcutInfo(context, false));
        }

        int maxShortcuts = shortcutManager.getMaxShortcutCountPerActivity();
        if (shortcuts.size() > maxShortcuts) {
            Logger.logErrorAndShowToast(context, LOG_TAG, getString(R.string.msg_dynamic_shortcuts_limit_reached, maxShortcuts));
            while (shortcuts.size() > maxShortcuts) {
                shortcuts.remove(shortcuts.size() - 1);
            }
        }

        shortcutManager.removeAllDynamicShortcuts();
        shortcutManager.addDynamicShortcuts(shortcuts);
        Logger.logDebugAndShowToast(context, LOG_TAG, getString(R.string.msg_created_dynamic_shortcuts_successfully, shortcuts.size()));
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private void removeDynamicShortcuts(@NonNull Context context) {
        ShortcutManager shortcutManager = ShortcutUtils.getShortcutManager(context, LOG_TAG, true);
        if (shortcutManager == null) return;

        shortcutManager.removeAllDynamicShortcuts();
        Logger.logDebugAndShowToast(context, LOG_TAG, getString(R.string.msg_removed_dynamic_shortcuts_successfully));
    }
}
