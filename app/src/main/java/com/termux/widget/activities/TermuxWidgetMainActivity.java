package com.termux.widget.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.widget.R;
import com.termux.widget.ShortcutFile;
import com.termux.widget.TermuxWidgetApplication;
import com.termux.widget.TermuxWidgetProvider;
import com.termux.widget.icons.IconRegistry;
import com.termux.widget.tiles.TermuxTilePreferences;
import com.termux.widget.utils.ShortcutUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TermuxWidgetMainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "TermuxWidgetMainActivity";

    public static final String TERMUX_WIDGET_DATA_HOME_DIR_PATH = TermuxConstants.TERMUX_DATA_HOME_DIR_PATH + "/widget";
    public static final String TERMUX_WIDGET_DYNAMIC_SHORTCUTS_DIR_PATH = TERMUX_WIDGET_DATA_HOME_DIR_PATH + "/dynamic_shortcuts";

    private GridLayout mGridQuickTiles;
    private LinearLayout mContainerDetectedShortcuts;
    private TextView mTxtStatusTitle;
    private TextView mTxtStatusSubtitle;

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

        findViewById(R.id.btn_refresh_shortcuts).setOnClickListener(v -> populateDashboard());
        findViewById(R.id.button_refresh_all_widgets).setOnClickListener(v -> sendIntentToRefreshAllWidgets());

        setDynamicShortcutsButtons();
        sendIntentToRefreshAllWidgets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TermuxWidgetApplication.setLogConfig(this, false);
        Logger.logVerbose(LOG_TAG, "onResume");
        populateDashboard();
    }

    private void populateDashboard() {
        checkTermuxStatus();
        populateQuickTiles();
        populateDetectedShortcuts();
    }

    private void checkTermuxStatus() {
        String errmsg = TermuxUtils.isTermuxAppAccessible(this);
        if (errmsg != null) {
            mTxtStatusTitle.setText("Termux Not Accessible");
            mTxtStatusSubtitle.setText(errmsg);
        } else {
            mTxtStatusTitle.setText("Termux Connected");
            List<ShortcutFile> files = new ArrayList<>();
            ShortcutUtils.enumerateShortcutFiles(files, false);
            mTxtStatusSubtitle.setText(files.size() + " shortcut(s) found in ~/.shortcuts/");
        }
    }

    private void populateQuickTiles() {
        if (mGridQuickTiles == null) return;
        mGridQuickTiles.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int slot = 1; slot <= 8; slot++) {
            final int slotIndex = slot;
            View slotView = inflater.inflate(R.layout.item_main_tile_slot, mGridQuickTiles, false);

            TextView txtSlotNum = slotView.findViewById(R.id.slot_number_badge);
            TextView txtLabel = slotView.findViewById(R.id.slot_tile_label);
            TextView txtStatus = slotView.findViewById(R.id.slot_tile_status);
            ImageView imgIcon = slotView.findViewById(R.id.slot_tile_icon);

            txtSlotNum.setText("TILE " + slot);

            String scriptPath = TermuxTilePreferences.getScriptPath(this, slot);
            String label = TermuxTilePreferences.getLabel(this, slot);
            String iconId = TermuxTilePreferences.getIconId(this, slot);

            if (scriptPath != null && !scriptPath.isEmpty()) {
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
    }

    private void populateDetectedShortcuts() {
        if (mContainerDetectedShortcuts == null) return;
        mContainerDetectedShortcuts.removeAllViews();

        List<ShortcutFile> files = new ArrayList<>();
        ShortcutUtils.enumerateShortcutFiles(files, true);

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
            imgIcon.setImageResource(isTask ? R.drawable.ic_hero_bolt : R.drawable.ic_lucide_terminal);

            btnRun.setOnClickListener(v -> {
                TermuxWidgetProvider.sendExecutionIntentToTermuxService(this, sf.getPath(), LOG_TAG);
                Toast.makeText(this, "Executing: " + sf.getLabel(), Toast.LENGTH_SHORT).show();
            });

            mContainerDetectedShortcuts.addView(row);
        }
    }

    private void setDynamicShortcutsButtons() {
        MaterialButton createBtn = findViewById(R.id.button_create_dynamic_shortcuts);
        MaterialButton removeBtn = findViewById(R.id.button_remove_dynamic_shortcuts);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            createBtn.setOnClickListener(v -> createDynamicShortcuts(this));
            removeBtn.setOnClickListener(v -> removeDynamicShortcuts(this));
        } else {
            createBtn.setVisibility(View.GONE);
            removeBtn.setVisibility(View.GONE);
        }
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
