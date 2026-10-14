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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.widget.R;
import com.termux.widget.ScriptInfoBottomSheet;
import com.termux.widget.ShortcutAppearanceBottomSheet;
import com.termux.widget.ShortcutFile;
import com.termux.widget.TermuxButtonWidgetUtils;
import com.termux.widget.TermuxCustomCommandPreferences;
import com.termux.widget.TermuxCustomCommandPreferences.CustomCommand;
import com.termux.widget.TermuxShortcutSyncManager;
import com.termux.widget.TermuxWidgetApplication;
import com.termux.widget.TermuxWidgetProvider;
import com.termux.widget.icons.IconHelper;
import com.termux.widget.icons.IconPickerBottomSheet;
import com.termux.widget.icons.IconRegistry;
import com.termux.widget.theme.ThemeManager;
import com.termux.widget.tiles.TermuxTilePreferences;
import com.termux.widget.toggle.TermuxTogglePreferences;
import com.termux.widget.toggle.ToggleShortcut;
import com.termux.widget.utils.BouncyAnimationHelper;
import com.termux.widget.utils.ShortcutUtils;

import android.view.animation.OvershootInterpolator;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TermuxWidgetMainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "TermuxWidgetMainActivity";

    public static final String TERMUX_WIDGET_DATA_HOME_DIR_PATH = TermuxConstants.TERMUX_DATA_HOME_DIR_PATH + "/widget";
    public static final String TERMUX_WIDGET_DYNAMIC_SHORTCUTS_DIR_PATH = TERMUX_WIDGET_DATA_HOME_DIR_PATH + "/dynamic_shortcuts";

    private GridLayout mGridQuickTiles;
    private LinearLayout mContainerDetectedShortcuts;
    private LinearLayout mContainerToggleShortcuts;
    private LinearLayout mContainerGroupChips;
    private HorizontalScrollView mScrollGroupChips;
    private String mSelectedGroupFilter = "ALL";
    private TextView mTxtStatusTitle;
    private TextView mTxtStatusSubtitle;
    private TextView mTxtShortcutsCount;
    private TextView mTxtTogglesCount;
    private TextView mTxtTilesSummary;
    private LinearLayout mContainerTilesExpandable;
    private ImageView mImgTilesExpandIcon;
    private boolean mTilesExpanded = false;
    private boolean mScriptsExpanded = true;
    private boolean mTogglesExpanded = true;
    private LinearLayout mContainerScriptsExpandable;
    private LinearLayout mContainerTogglesExpandable;
    private ImageView mImgScriptsExpandIcon;
    private ImageView mImgTogglesExpandIcon;

    private final BroadcastReceiver mSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            populateDashboard();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        Logger.logDebug(LOG_TAG, "onCreate");

        setContentView(R.layout.activity_termux_widget_main);

        MaterialToolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        MaterialSwitch switchAmoled = findViewById(R.id.switch_amoled_theme);
        if (switchAmoled != null) {
            switchAmoled.setChecked(ThemeManager.isAmoledMode(this));
            switchAmoled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked == ThemeManager.isAmoledMode(this)) return;
                ThemeManager.setAmoledMode(this, isChecked);
                sendIntentToRefreshAllWidgets(false);
                // Allow switch thumb animation to smoothly finish before recreating activity
                buttonView.postDelayed(this::recreate, 220);
            });
        }

        mGridQuickTiles = findViewById(R.id.grid_quick_tiles);
        mContainerDetectedShortcuts = findViewById(R.id.container_detected_shortcuts);
        mContainerToggleShortcuts = findViewById(R.id.container_toggle_shortcuts);
        mContainerGroupChips = findViewById(R.id.container_group_chips);
        mScrollGroupChips = findViewById(R.id.scroll_group_chips);
        mTxtStatusTitle = findViewById(R.id.txt_status_title);
        mTxtStatusSubtitle = findViewById(R.id.txt_status_subtitle);
        mTxtShortcutsCount = findViewById(R.id.txt_shortcuts_count);
        mTxtTogglesCount = findViewById(R.id.txt_toggles_count);
        mTxtTilesSummary = findViewById(R.id.txt_tiles_summary);
        mContainerTilesExpandable = findViewById(R.id.container_tiles_expandable);
        mImgTilesExpandIcon = findViewById(R.id.img_tiles_expand_icon);
        mContainerScriptsExpandable = findViewById(R.id.container_scripts_expandable);
        mContainerTogglesExpandable = findViewById(R.id.container_toggles_expandable);
        mImgScriptsExpandIcon = findViewById(R.id.img_scripts_expand_icon);
        mImgTogglesExpandIcon = findViewById(R.id.img_toggles_expand_icon);

        View headerTiles = findViewById(R.id.header_quick_tiles);
        if (headerTiles != null) {
            BouncyAnimationHelper.applySpringPress(headerTiles, 0.98f);
            headerTiles.setOnClickListener(v -> toggleTilesExpansion());
        }

        View headerScripts = findViewById(R.id.header_scripts);
        if (headerScripts != null) {
            BouncyAnimationHelper.applySpringPress(headerScripts, 0.98f);
            headerScripts.setOnClickListener(v -> toggleScriptsExpansion());
        }

        View headerToggles = findViewById(R.id.header_toggles);
        if (headerToggles != null) {
            BouncyAnimationHelper.applySpringPress(headerToggles, 0.98f);
            headerToggles.setOnClickListener(v -> toggleTogglesExpansion());
        }

        View btnOpenSettings = findViewById(R.id.btn_open_settings);
        if (btnOpenSettings != null) {
            BouncyAnimationHelper.applySpringPress(btnOpenSettings, 0.88f);
            btnOpenSettings.setOnClickListener(v -> startActivity(new Intent(this, TermuxWidgetSettingsActivity.class)));
        }

        View btnOpenSettingsCard = findViewById(R.id.btn_open_settings_card);
        if (btnOpenSettingsCard != null) {
            BouncyAnimationHelper.applySpringPress(btnOpenSettingsCard, 0.96f);
            btnOpenSettingsCard.setOnClickListener(v -> startActivity(new Intent(this, TermuxWidgetSettingsActivity.class)));
        }

        View btnAddToggle = findViewById(R.id.btn_add_toggle);
        if (btnAddToggle != null) {
            BouncyAnimationHelper.applySpringPress(btnAddToggle, 0.95f);
            btnAddToggle.setOnClickListener(v -> showAddEditToggleDialog(null));
        }

        View btnAddCmd = findViewById(R.id.btn_add_command);
        if (btnAddCmd != null) {
            BouncyAnimationHelper.applySpringPress(btnAddCmd, 0.95f);
            btnAddCmd.setOnClickListener(v -> showAddEditCommandDialog(null));
        }

        View btnHelpScripts = findViewById(R.id.btn_help_scripts);
        if (btnHelpScripts != null) {
            BouncyAnimationHelper.applySpringPress(btnHelpScripts, 0.88f);
            btnHelpScripts.setOnClickListener(v -> showScriptsHelpDialog());
        }

        View btnHelpToggles = findViewById(R.id.btn_help_toggles);
        if (btnHelpToggles != null) {
            BouncyAnimationHelper.applySpringPress(btnHelpToggles, 0.88f);
            btnHelpToggles.setOnClickListener(v -> showTogglesHelpDialog());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission("com.termux.permission.RUN_COMMAND") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"com.termux.permission.RUN_COMMAND"}, 1001);
            }
        }

        View btnRefreshSync = findViewById(R.id.btn_refresh_shortcuts);
        if (btnRefreshSync != null) {
            BouncyAnimationHelper.applySpringPress(btnRefreshSync, 0.97f);
            btnRefreshSync.setOnClickListener(v -> {
                TermuxShortcutSyncManager.syncShortcutsFromTermux(this, true);
                Toast.makeText(this, "Refreshing shortcuts from Termux...", Toast.LENGTH_SHORT).show();
                populateDashboard();
            });
        }

        View btnRefreshWidgets = findViewById(R.id.button_refresh_all_widgets);
        if (btnRefreshWidgets != null) {
            BouncyAnimationHelper.applySpringPress(btnRefreshWidgets, 0.88f);
            btnRefreshWidgets.setOnClickListener(v -> {
                ImageView imgRefresh = findViewById(R.id.img_refresh_all_widgets);
                if (imgRefresh != null) {
                    imgRefresh.animate().rotationBy(360f).setDuration(400).setInterpolator(new FastOutSlowInInterpolator()).start();
                }
                sendIntentToRefreshAllWidgets(true);
            });
        }

        View btnCopy = findViewById(R.id.btn_copy_setup_command);
        if (btnCopy != null) {
            BouncyAnimationHelper.applySpringPress(btnCopy, 0.97f);
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
            BouncyAnimationHelper.applySpringPress(btnSettings, 0.97f);
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        }

        sendIntentToRefreshAllWidgets(false);
    }

    private void toggleTilesExpansion() {
        mTilesExpanded = !mTilesExpanded;
        if (mContainerTilesExpandable != null) {
            mContainerTilesExpandable.setVisibility(mTilesExpanded ? View.VISIBLE : View.GONE);
        }
        if (mImgTilesExpandIcon != null) {
            mImgTilesExpandIcon.animate().rotation(mTilesExpanded ? 90f : 0f).setDuration(200).start();
        }
        populateQuickTiles();
    }

    private void toggleScriptsExpansion() {
        mScriptsExpanded = !mScriptsExpanded;
        if (mContainerScriptsExpandable != null) {
            mContainerScriptsExpandable.setVisibility(mScriptsExpanded ? View.VISIBLE : View.GONE);
        }
        if (mImgScriptsExpandIcon != null) {
            mImgScriptsExpandIcon.animate().rotation(mScriptsExpanded ? 90f : 0f).setDuration(200).start();
        }
    }

    private void toggleTogglesExpansion() {
        mTogglesExpanded = !mTogglesExpanded;
        if (mContainerTogglesExpandable != null) {
            mContainerTogglesExpandable.setVisibility(mTogglesExpanded ? View.VISIBLE : View.GONE);
        }
        if (mImgTogglesExpandIcon != null) {
            mImgTogglesExpandIcon.animate().rotation(mTogglesExpanded ? 90f : 0f).setDuration(200).start();
        }
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
        applyAmoledThemeIfNeeded();
        checkTermuxStatus();
        populateQuickTiles();
        populateDetectedShortcuts();
        populateToggleShortcuts();
    }

    private void applyAmoledThemeIfNeeded() {
        if (!ThemeManager.isAmoledMode(this)) return;

        View root = findViewById(R.id.main_root_layout);
        if (root != null) root.setBackgroundColor(Color.BLACK);

        MaterialToolbar toolbar = findViewById(R.id.main_toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(Color.BLACK);
            toolbar.setTitleTextColor(Color.WHITE);
        }

        // Status Card
        MaterialCardView statusCard = findViewById(R.id.card_status_banner);
        if (statusCard != null) {
            statusCard.setCardBackgroundColor(Color.parseColor("#0A0A0A"));
            statusCard.setStrokeColor(Color.parseColor("#262626"));
        }
        View statusBadge = findViewById(R.id.status_badge_frame);
        if (statusBadge != null) statusBadge.setBackgroundResource(R.drawable.widget_icon_badge_amoled_bg);
        ImageView imgStatus = findViewById(R.id.img_status_icon);
        if (imgStatus != null) imgStatus.setColorFilter(Color.WHITE);
        if (mTxtStatusTitle != null) mTxtStatusTitle.setTextColor(Color.WHITE);
        if (mTxtStatusSubtitle != null) mTxtStatusSubtitle.setTextColor(Color.parseColor("#9E9E9E"));

        View btnRefreshWidgets = findViewById(R.id.button_refresh_all_widgets);
        if (btnRefreshWidgets != null) btnRefreshWidgets.setBackgroundResource(R.drawable.circle_refresh_button_amoled_bg);
        ImageView imgRefresh = findViewById(R.id.img_refresh_all_widgets);
        if (imgRefresh != null) imgRefresh.setColorFilter(Color.WHITE);

        MaterialButton btnSync = findViewById(R.id.btn_refresh_shortcuts);
        if (btnSync != null) {
            btnSync.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#141416")));
            btnSync.setTextColor(Color.WHITE);
            btnSync.setIconTint(ColorStateList.valueOf(Color.WHITE));
            btnSync.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2E2E32")));
            btnSync.setStrokeWidth(3);
        }

        TextView txtAmoledTitle = findViewById(R.id.txt_amoled_title);
        if (txtAmoledTitle != null) txtAmoledTitle.setTextColor(Color.WHITE);
        ImageView imgAmoledMoon = findViewById(R.id.img_amoled_moon);
        if (imgAmoledMoon != null) imgAmoledMoon.setColorFilter(Color.WHITE);

        // Section Headers
        if (mTxtShortcutsCount != null) mTxtShortcutsCount.setTextColor(Color.WHITE);
        if (mTxtTogglesCount != null) mTxtTogglesCount.setTextColor(Color.WHITE);

        ImageView imgHelpScripts = findViewById(R.id.img_help_scripts);
        if (imgHelpScripts != null) imgHelpScripts.setColorFilter(Color.WHITE);
        ImageView imgHelpToggles = findViewById(R.id.img_help_toggles);
        if (imgHelpToggles != null) imgHelpToggles.setColorFilter(Color.WHITE);

        MaterialButton btnAddCmd = findViewById(R.id.btn_add_command);
        if (btnAddCmd != null) {
            btnAddCmd.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnAddCmd.setTextColor(Color.BLACK);
            btnAddCmd.setIconTint(ColorStateList.valueOf(Color.BLACK));
        }

        MaterialButton btnAddToggle = findViewById(R.id.btn_add_toggle);
        if (btnAddToggle != null) {
            btnAddToggle.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnAddToggle.setTextColor(Color.BLACK);
            btnAddToggle.setIconTint(ColorStateList.valueOf(Color.BLACK));
        }

        // Quick Tiles Card
        MaterialCardView tilesCard = findViewById(R.id.card_tiles_section);
        if (tilesCard != null) {
            tilesCard.setCardBackgroundColor(Color.parseColor("#0A0A0A"));
            tilesCard.setStrokeColor(Color.parseColor("#262626"));
        }
        View tilesBadge = findViewById(R.id.tiles_badge_frame);
        if (tilesBadge != null) tilesBadge.setBackgroundResource(R.drawable.widget_icon_badge_amoled_bg);
        ImageView imgTiles = findViewById(R.id.img_tiles_icon);
        if (imgTiles != null) imgTiles.setColorFilter(Color.WHITE);
        TextView txtTilesTitle = findViewById(R.id.txt_tiles_title);
        if (txtTilesTitle != null) txtTilesTitle.setTextColor(Color.WHITE);
        if (mTxtTilesSummary != null) mTxtTilesSummary.setTextColor(Color.parseColor("#9E9E9E"));
        if (mImgTilesExpandIcon != null) mImgTilesExpandIcon.setColorFilter(Color.parseColor("#9E9E9E"));

        // Setup Guide Card
        MaterialCardView setupCard = findViewById(R.id.card_setup_guide);
        if (setupCard != null) {
            setupCard.setCardBackgroundColor(Color.parseColor("#0A0A0A"));
            setupCard.setStrokeColor(Color.parseColor("#262626"));
        }
        View setupBadge = findViewById(R.id.setup_badge_frame);
        if (setupBadge != null) setupBadge.setBackgroundResource(R.drawable.widget_icon_badge_amoled_bg);
        ImageView imgSetup = findViewById(R.id.img_setup_icon);
        if (imgSetup != null) imgSetup.setColorFilter(Color.WHITE);
        TextView txtSetupTitle = findViewById(R.id.txt_setup_title);
        if (txtSetupTitle != null) txtSetupTitle.setTextColor(Color.WHITE);
        TextView txtSetupSub = findViewById(R.id.txt_setup_subtitle);
        if (txtSetupSub != null) txtSetupSub.setTextColor(Color.parseColor("#9E9E9E"));

        MaterialButton btnCopy = findViewById(R.id.btn_copy_setup_command);
        if (btnCopy != null) {
            btnCopy.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#141416")));
            btnCopy.setTextColor(Color.WHITE);
            btnCopy.setIconTint(ColorStateList.valueOf(Color.WHITE));
            btnCopy.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2E2E32")));
            btnCopy.setStrokeWidth(3);
        }

        MaterialButton btnSettings = findViewById(R.id.btn_open_app_settings);
        if (btnSettings != null) {
            btnSettings.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#141416")));
            btnSettings.setTextColor(Color.WHITE);
            btnSettings.setIconTint(ColorStateList.valueOf(Color.WHITE));
            btnSettings.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2E2E32")));
            btnSettings.setStrokeWidth(3);
        }

        View btnOpenSettings = findViewById(R.id.btn_open_settings);
        if (btnOpenSettings != null) btnOpenSettings.setBackgroundResource(R.drawable.circle_refresh_button_amoled_bg);
        ImageView imgOpenSettings = findViewById(R.id.img_open_settings);
        if (imgOpenSettings != null) imgOpenSettings.setColorFilter(Color.WHITE);

        MaterialButton btnOpenSettingsCard = findViewById(R.id.btn_open_settings_card);
        if (btnOpenSettingsCard != null) {
            btnOpenSettingsCard.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#141416")));
            btnOpenSettingsCard.setTextColor(Color.WHITE);
            btnOpenSettingsCard.setIconTint(ColorStateList.valueOf(Color.WHITE));
            btnOpenSettingsCard.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2E2E32")));
            btnOpenSettingsCard.setStrokeWidth(3);
        }

        if (mImgScriptsExpandIcon != null) mImgScriptsExpandIcon.setColorFilter(Color.parseColor("#9E9E9E"));
        if (mImgTogglesExpandIcon != null) mImgTogglesExpandIcon.setColorFilter(Color.parseColor("#9E9E9E"));
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
        boolean isAmoled = ThemeManager.isAmoledMode(this);
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
                IconHelper.applyToImageView(imgIcon, null, iconId, R.drawable.ic_lucide_terminal);
            } else {
                txtLabel.setText("Unassigned");
                txtStatus.setText("Tap to setup");
                imgIcon.setImageResource(R.drawable.ic_lucide_terminal);
            }

            if (isAmoled) {
                MaterialCardView card = (MaterialCardView) slotView;
                card.setCardBackgroundColor(Color.parseColor("#0A0A0A"));
                card.setStrokeColor(Color.parseColor("#262626"));
                txtLabel.setTextColor(Color.WHITE);
                txtStatus.setTextColor(Color.parseColor("#9E9E9E"));
                txtSlotNum.setBackgroundResource(R.drawable.widget_pill_badge_amoled_bg);
                txtSlotNum.setTextColor(Color.WHITE);
                View badgeFrame = slotView.findViewById(R.id.slot_badge_frame);
                if (badgeFrame != null) badgeFrame.setBackgroundResource(R.drawable.widget_icon_badge_amoled_bg);
                imgIcon.setColorFilter(Color.WHITE);
            }

            BouncyAnimationHelper.applySpringPress(slotView, 0.95f);
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

    private View createGroupChip(String title, String groupKey, boolean isSelected, boolean isAmoled) {
        TextView chip = new TextView(this);
        chip.setText(title);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
        chip.setGravity(Gravity.CENTER);

        int padH = dpToPx(14);
        int padV = dpToPx(8);
        chip.setPadding(padH, padV, padH, padV);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(16));

        if (isAmoled) {
            if (isSelected) {
                bg.setColor(Color.WHITE);
                chip.setTextColor(Color.BLACK);
            } else {
                bg.setColor(Color.parseColor("#121214"));
                bg.setStroke(dpToPx(1), Color.parseColor("#282828"));
                chip.setTextColor(Color.parseColor("#CCCCCC"));
            }
        } else {
            if (isSelected) {
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
                bg.setColor(typedValue.data);

                TypedValue textValue = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, textValue, true);
                chip.setTextColor(textValue.data);
            } else {
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHigh, typedValue, true);
                bg.setColor(typedValue.data);

                TypedValue textValue = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, textValue, true);
                chip.setTextColor(textValue.data);
            }
        }

        chip.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMarginEnd(dpToPx(8));
        chip.setLayoutParams(lp);

        BouncyAnimationHelper.applySpringPress(chip, 0.94f);
        chip.setOnClickListener(v -> {
            mSelectedGroupFilter = groupKey;
            populateDetectedShortcuts();
        });

        return chip;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void populateDetectedShortcuts() {
        if (mContainerDetectedShortcuts == null) return;
        mContainerDetectedShortcuts.removeAllViews();

        List<ShortcutFile> allFiles = new ArrayList<>();
        ShortcutUtils.enumerateShortcutFiles(this, allFiles, true);

        List<ShortcutFile> files = new ArrayList<>();
        for (ShortcutFile sf : allFiles) {
            if (!sf.isToggle()) {
                files.add(sf);
            }
        }

        boolean isAmoled = ThemeManager.isAmoledMode(this);

        // Group counts
        Map<String, Integer> groupCounts = new LinkedHashMap<>();
        for (ShortcutFile sf : files) {
            String grp = sf.getGroupName();
            groupCounts.put(grp, groupCounts.getOrDefault(grp, 0) + 1);
        }

        // Validate current filter
        if (!"ALL".equalsIgnoreCase(mSelectedGroupFilter) && !groupCounts.containsKey(mSelectedGroupFilter)) {
            mSelectedGroupFilter = "ALL";
        }

        // Populate group filter chips
        if (mContainerGroupChips != null) {
            mContainerGroupChips.removeAllViews();
            if (files.isEmpty()) {
                if (mScrollGroupChips != null) mScrollGroupChips.setVisibility(View.GONE);
            } else {
                if (mScrollGroupChips != null) mScrollGroupChips.setVisibility(View.VISIBLE);
                mContainerGroupChips.addView(createGroupChip("All (" + files.size() + ")", "ALL", "ALL".equalsIgnoreCase(mSelectedGroupFilter), isAmoled));
                for (Map.Entry<String, Integer> entry : groupCounts.entrySet()) {
                    String grpName = entry.getKey();
                    int count = entry.getValue();
                    boolean isSel = grpName.equalsIgnoreCase(mSelectedGroupFilter);
                    mContainerGroupChips.addView(createGroupChip(grpName + " (" + count + ")", grpName, isSel, isAmoled));
                }
            }
        }

        // Filter files for display
        List<ShortcutFile> displayFiles = new ArrayList<>();
        for (ShortcutFile sf : files) {
            if ("ALL".equalsIgnoreCase(mSelectedGroupFilter) || mSelectedGroupFilter.equalsIgnoreCase(sf.getGroupName())) {
                displayFiles.add(sf);
            }
        }

        if (mTxtShortcutsCount != null) {
            if ("ALL".equalsIgnoreCase(mSelectedGroupFilter) || displayFiles.size() == files.size()) {
                mTxtShortcutsCount.setText("Scripts : " + files.size());
            } else {
                mTxtShortcutsCount.setText("Scripts : " + displayFiles.size() + "/" + files.size());
            }
        }

        if (displayFiles.isEmpty()) {
            TextView emptyText = new TextView(this);
            if (files.isEmpty()) {
                emptyText.setText(R.string.msg_no_shortcut_scripts);
            } else {
                emptyText.setText("No scripts found in group '" + mSelectedGroupFilter + "'");
            }
            emptyText.setTextSize(13);
            emptyText.setPadding(dpToPx(6), dpToPx(16), dpToPx(6), dpToPx(16));
            emptyText.setTextColor(isAmoled ? Color.parseColor("#9E9E9E") : getResources().getColor(R.color.m3_on_surface_variant));
            mContainerDetectedShortcuts.addView(emptyText);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ShortcutFile sf : displayFiles) {
            View row = inflater.inflate(R.layout.item_main_shortcut_row, mContainerDetectedShortcuts, false);

            TextView txtTitle = row.findViewById(R.id.row_shortcut_title);
            TextView txtPath = row.findViewById(R.id.row_shortcut_path);
            TextView txtBadge = row.findViewById(R.id.row_shortcut_badge);
            ImageView imgIcon = row.findViewById(R.id.row_shortcut_icon);
            ImageView imgCustom = row.findViewById(R.id.row_shortcut_custom_image);
            View btnRun = row.findViewById(R.id.row_btn_run);
            ImageView imgRun = row.findViewById(R.id.img_row_btn_run);

            txtTitle.setText(sf.getLabel());
            txtPath.setText(sf.getUnExpandedPath());

            boolean isTask = sf.isTask();
            txtBadge.setText(isTask ? "TASK" : "TERM");

            if (isAmoled) {
                MaterialCardView card = (MaterialCardView) row;
                card.setCardBackgroundColor(Color.parseColor("#0A0A0A"));
                card.setStrokeColor(Color.parseColor("#262626"));
                txtTitle.setTextColor(Color.WHITE);
                txtPath.setTextColor(Color.parseColor("#9E9E9E"));
                txtBadge.setBackgroundResource(R.drawable.widget_pill_badge_amoled_bg);
                txtBadge.setTextColor(Color.WHITE);
                View badgeFrame = row.findViewById(R.id.row_shortcut_icon_badge);
                if (badgeFrame != null) badgeFrame.setBackgroundResource(R.drawable.widget_icon_badge_amoled_bg);
                imgIcon.setColorFilter(Color.WHITE);

                btnRun.setBackgroundResource(R.drawable.circle_run_button_amoled_bg);
                if (imgRun != null) imgRun.setColorFilter(Color.BLACK);
            } else {
                txtBadge.setBackgroundResource(isTask ? R.drawable.widget_pill_badge_bg : R.drawable.widget_pill_session_bg);
                txtBadge.setTextColor(getResources().getColor(isTask ? R.color.badge_task_text : R.color.badge_session_text));

                btnRun.setBackgroundResource(R.drawable.circle_run_button_bg);
                if (imgRun != null) imgRun.setColorFilter(getColor(R.color.m3_on_primary));
            }

            int defaultIcon = isTask ? R.drawable.ic_hero_bolt : R.drawable.ic_lucide_terminal;
            IconHelper.applyToImageView(imgIcon, imgCustom, sf.mIconId, defaultIcon);

            BouncyAnimationHelper.applySpringPress(btnRun, 0.88f);
            btnRun.setOnClickListener(v -> {
                TermuxWidgetProvider.sendExecutionIntentToTermuxService(this, sf.getPath(), LOG_TAG);
                Toast.makeText(this, "Executing: " + sf.getLabel(), Toast.LENGTH_SHORT).show();
            });

            BouncyAnimationHelper.applySpringPress(row, 0.97f);
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
                                         sendIntentToRefreshAllWidgets(false);
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

    private void populateToggleShortcuts() {
        if (mContainerToggleShortcuts == null) return;
        mContainerToggleShortcuts.removeAllViews();

        List<ToggleShortcut> toggles = TermuxTogglePreferences.getToggles(this);
        if (mTxtTogglesCount != null) {
            mTxtTogglesCount.setText("Toggles : " + toggles.size());
        }

        if (toggles.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No toggles configured yet. Tap (+) to pair 2 scripts.");
            emptyText.setTextSize(12);
            emptyText.setTextColor(getResources().getColor(R.color.m3_on_surface_variant));
            emptyText.setPadding(8, 12, 8, 12);
            mContainerToggleShortcuts.addView(emptyText);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        boolean isAmoled = ThemeManager.isAmoledMode(this);
        for (ToggleShortcut toggle : toggles) {
            View row = inflater.inflate(R.layout.item_main_toggle_row, mContainerToggleShortcuts, false);

            TextView txtTitle = row.findViewById(R.id.txt_toggle_title);
            TextView txtScripts = row.findViewById(R.id.txt_toggle_scripts);
            ImageView imgIcon = row.findViewById(R.id.img_toggle_icon);
            ImageView imgCustom = row.findViewById(R.id.img_toggle_custom_image);
            MaterialSwitch switchState = row.findViewById(R.id.switch_toggle_state);
            MaterialCardView card = row.findViewById(R.id.card_toggle_row);

            txtTitle.setText(toggle.label);

            String onName = com.termux.shared.shell.ShellUtils.getExecutableBasename(toggle.onScriptPath);
            String offName = com.termux.shared.shell.ShellUtils.getExecutableBasename(toggle.offScriptPath);
            txtScripts.setText("ON: " + onName + " • OFF: " + offName);

            IconHelper.applyToImageView(imgIcon, imgCustom, toggle.iconId, R.drawable.ic_lucide_power);

            applyToggleVisualState(row, toggle.isOn, isAmoled);

            switchState.setChecked(toggle.isOn);
            switchState.setOnClickListener(v -> {
                boolean isNowOn = switchState.isChecked();
                toggle.isOn = isNowOn;
                TermuxTogglePreferences.toggleState(this, toggle.id);
                animateToggleRow(row, toggle, isNowOn, isAmoled);
                sendIntentToRefreshAllWidgets(false);
            });

            BouncyAnimationHelper.applySpringPress(card, 0.97f);
            row.setOnClickListener(v -> showToggleOptionsDialog(toggle));

            mContainerToggleShortcuts.addView(row);
        }
    }

    private void applyToggleVisualState(@NonNull View row, boolean isOn, boolean isAmoled) {
        MaterialCardView card = row.findViewById(R.id.card_toggle_row);
        TextView txtTitle = row.findViewById(R.id.txt_toggle_title);
        TextView txtScripts = row.findViewById(R.id.txt_toggle_scripts);
        TextView txtStatusPill = row.findViewById(R.id.txt_toggle_status_pill);
        ImageView imgIcon = row.findViewById(R.id.img_toggle_icon);
        View badgeFrame = row.findViewById(R.id.toggle_badge_frame);

        if (isAmoled) {
            if (isOn) {
                card.setCardBackgroundColor(Color.WHITE);
                card.setStrokeColor(Color.WHITE);
                txtTitle.setTextColor(Color.BLACK);
                txtScripts.setTextColor(Color.parseColor("#333333"));
                txtStatusPill.setText("ON");
                txtStatusPill.setBackgroundResource(R.drawable.widget_toggle_badge_amoled_on_bg);
                txtStatusPill.setTextColor(Color.WHITE);
                badgeFrame.setBackgroundResource(R.drawable.widget_toggle_badge_amoled_on_bg);
                imgIcon.setColorFilter(Color.WHITE);
            } else {
                card.setCardBackgroundColor(Color.parseColor("#0A0A0A"));
                card.setStrokeColor(Color.parseColor("#262626"));
                txtTitle.setTextColor(Color.WHITE);
                txtScripts.setTextColor(Color.parseColor("#9E9E9E"));
                txtStatusPill.setText("OFF");
                txtStatusPill.setBackgroundResource(R.drawable.widget_pill_badge_amoled_bg);
                txtStatusPill.setTextColor(Color.WHITE);
                badgeFrame.setBackgroundResource(R.drawable.widget_icon_badge_amoled_bg);
                imgIcon.setColorFilter(Color.WHITE);
            }
        } else {
            if (isOn) {
                card.setCardBackgroundColor(getResources().getColor(R.color.m3_primary));
                txtTitle.setTextColor(getResources().getColor(R.color.m3_on_primary));
                txtScripts.setTextColor(getResources().getColor(R.color.m3_on_primary));
                txtStatusPill.setText("ON");
                txtStatusPill.setBackgroundResource(R.drawable.widget_toggle_badge_on_bg);
                txtStatusPill.setTextColor(getResources().getColor(R.color.m3_primary));
                badgeFrame.setBackgroundResource(R.drawable.widget_toggle_badge_on_bg);
                imgIcon.setColorFilter(getResources().getColor(R.color.m3_primary));
            } else {
                card.setCardBackgroundColor(getResources().getColor(R.color.m3_surface_container));
                txtTitle.setTextColor(getResources().getColor(R.color.m3_on_surface));
                txtScripts.setTextColor(getResources().getColor(R.color.m3_on_surface_variant));
                txtStatusPill.setText("OFF");
                txtStatusPill.setBackgroundResource(R.drawable.widget_pill_session_bg);
                txtStatusPill.setTextColor(getResources().getColor(R.color.badge_session_text));
                badgeFrame.setBackgroundResource(R.drawable.widget_icon_badge_bg);
                imgIcon.setColorFilter(getResources().getColor(R.color.m3_primary));
            }
        }
    }

    private void animateToggleRow(@NonNull View row, @NonNull ToggleShortcut toggle, boolean isOn, boolean isAmoled) {
        MaterialCardView card = row.findViewById(R.id.card_toggle_row);
        TextView txtTitle = row.findViewById(R.id.txt_toggle_title);
        TextView txtScripts = row.findViewById(R.id.txt_toggle_scripts);
        TextView txtStatusPill = row.findViewById(R.id.txt_toggle_status_pill);
        ImageView imgIcon = row.findViewById(R.id.img_toggle_icon);
        View badgeFrame = row.findViewById(R.id.toggle_badge_frame);

        int fromBgColor, toBgColor;
        int fromTextColor, toTextColor;
        int fromSubColor, toSubColor;

        if (isAmoled) {
            fromBgColor = isOn ? Color.parseColor("#0A0A0A") : Color.WHITE;
            toBgColor = isOn ? Color.WHITE : Color.parseColor("#0A0A0A");
            fromTextColor = isOn ? Color.WHITE : Color.BLACK;
            toTextColor = isOn ? Color.BLACK : Color.WHITE;
            fromSubColor = isOn ? Color.parseColor("#9E9E9E") : Color.parseColor("#333333");
            toSubColor = isOn ? Color.parseColor("#333333") : Color.parseColor("#9E9E9E");
            int strokeColor = isOn ? Color.WHITE : Color.parseColor("#262626");
            card.setStrokeColor(strokeColor);
        } else {
            fromBgColor = isOn ? getResources().getColor(R.color.m3_surface_container) : getResources().getColor(R.color.m3_primary);
            toBgColor = isOn ? getResources().getColor(R.color.m3_primary) : getResources().getColor(R.color.m3_surface_container);
            fromTextColor = isOn ? getResources().getColor(R.color.m3_on_surface) : getResources().getColor(R.color.m3_on_primary);
            toTextColor = isOn ? getResources().getColor(R.color.m3_on_primary) : getResources().getColor(R.color.m3_on_surface);
            fromSubColor = isOn ? getResources().getColor(R.color.m3_on_surface_variant) : getResources().getColor(R.color.m3_on_primary);
            toSubColor = isOn ? getResources().getColor(R.color.m3_on_primary) : getResources().getColor(R.color.m3_on_surface_variant);
        }

        // Animate background color smoothly
        BouncyAnimationHelper.animateColor(fromBgColor, toBgColor, 220, anim -> {
            int c = (int) anim.getAnimatedValue();
            card.setCardBackgroundColor(c);
        });

        // Animate text colors smoothly
        BouncyAnimationHelper.animateColor(fromTextColor, toTextColor, 200, anim -> {
            int c = (int) anim.getAnimatedValue();
            txtTitle.setTextColor(c);
        });

        BouncyAnimationHelper.animateColor(fromSubColor, toSubColor, 200, anim -> {
            int c = (int) anim.getAnimatedValue();
            txtScripts.setTextColor(c);
        });

        // Bouncy status pill transition
        txtStatusPill.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .setDuration(70)
                .withEndAction(() -> {
                    if (isAmoled) {
                        txtStatusPill.setText(isOn ? "ON" : "OFF");
                        txtStatusPill.setBackgroundResource(isOn ? R.drawable.widget_toggle_badge_amoled_on_bg : R.drawable.widget_pill_badge_amoled_bg);
                        txtStatusPill.setTextColor(Color.WHITE);
                        badgeFrame.setBackgroundResource(isOn ? R.drawable.widget_toggle_badge_amoled_on_bg : R.drawable.widget_icon_badge_amoled_bg);
                        imgIcon.setColorFilter(Color.WHITE);
                    } else {
                        txtStatusPill.setText(isOn ? "ON" : "OFF");
                        txtStatusPill.setBackgroundResource(isOn ? R.drawable.widget_toggle_badge_on_bg : R.drawable.widget_pill_session_bg);
                        txtStatusPill.setTextColor(getResources().getColor(isOn ? R.color.m3_primary : R.color.badge_session_text));
                        badgeFrame.setBackgroundResource(isOn ? R.drawable.widget_toggle_badge_on_bg : R.drawable.widget_icon_badge_bg);
                        imgIcon.setColorFilter(getResources().getColor(R.color.m3_primary));
                    }
                    txtStatusPill.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(160)
                            .setInterpolator(new OvershootInterpolator(2.4f))
                            .start();
                })
                .start();

        // Subtle physical card bounce
        BouncyAnimationHelper.popBounce(card);
    }

    private void showScriptsHelpDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Scripts & Commands")
                .setMessage("Scripts are shell scripts loaded directly from ~/.shortcuts/ in Termux, or custom commands created in this app using the (+) button.\n\n" +
                        "• Tap any script to run it immediately.\n" +
                        "• Tap a script card to view its path, assign custom icons, or map it to Quick Settings tiles.\n" +
                        "• Tap (+) to create a new custom command.")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showTogglesHelpDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Toggle Shortcuts")
                .setMessage("Toggles let you pair two existing scripts (one for 'ON' and one for 'OFF') into a single smart toggle switch without modifying your scripts.\n\n" +
                        "• Tap (+) to pair an ON script with an OFF script.\n" +
                        "• Tapping the switch executes the corresponding script and smoothly updates the state.\n" +
                        "• Works seamlessly in home screen widgets and Quick Settings tiles with inverted active indicators.")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showToggleOptionsDialog(@NonNull ToggleShortcut toggle) {
        CharSequence[] options = new CharSequence[]{
                "Add to Home Screen (Widget)",
                "Toggle State (Switch to " + (toggle.isOn ? "OFF" : "ON") + ")",
                "Edit Toggle",
                "Delete Toggle"
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(toggle.label)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        TermuxButtonWidgetUtils.pinButtonWidget(
                                this,
                                TermuxTogglePreferences.PREFIX_TOGGLE + toggle.id,
                                toggle.label,
                                toggle.iconId);
                    } else if (which == 1) {
                        TermuxTogglePreferences.toggleState(this, toggle.id);
                        populateDashboard();
                    } else if (which == 2) {
                        showAddEditToggleDialog(toggle);
                    } else if (which == 3) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Delete Toggle")
                                .setMessage("Are you sure you want to delete toggle '" + toggle.label + "'?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    TermuxTogglePreferences.deleteToggle(this, toggle.id);
                                    populateDashboard();
                                    sendIntentToRefreshAllWidgets(false);
                                    Toast.makeText(this, "Toggle deleted", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .show();
    }

    private void showAddEditToggleDialog(@Nullable ToggleShortcut existingToggle) {
        List<ShortcutFile> allFiles = new ArrayList<>();
        ShortcutUtils.enumerateShortcutFiles(this, allFiles, true);
        List<ShortcutFile> scriptFiles = new ArrayList<>();
        for (ShortcutFile sf : allFiles) {
            if (!sf.isToggle()) {
                scriptFiles.add(sf);
            }
        }

        if (scriptFiles.size() < 2 && existingToggle == null) {
            Toast.makeText(this, "At least 2 scripts or commands are required to create a toggle pair.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] scriptNames = new String[scriptFiles.size()];
        for (int i = 0; i < scriptFiles.size(); i++) {
            scriptNames[i] = scriptFiles.get(i).getLabel() + " (" + scriptFiles.get(i).getGroupName() + ")";
        }

        final String[] onScriptPath = new String[]{existingToggle != null ? existingToggle.onScriptPath : (scriptFiles.size() > 0 ? scriptFiles.get(0).getPath() : "")};
        final String[] offScriptPath = new String[]{existingToggle != null ? existingToggle.offScriptPath : (scriptFiles.size() > 1 ? scriptFiles.get(1).getPath() : "")};
        final String[] selectedIconId = new String[]{existingToggle != null ? existingToggle.iconId : "lucide_power"};

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText inputName = new EditText(this);
        inputName.setHint("Toggle Name (e.g. SSH Server, Tailscale VPN)");
        if (existingToggle != null) inputName.setText(existingToggle.label);
        layout.addView(inputName);

        MaterialButton btnPickOn = new MaterialButton(this);
        btnPickOn.setText("ON Script: " + (onScriptPath[0].isEmpty() ? "Select..." : getScriptDisplayName(scriptFiles, onScriptPath[0])));
        btnPickOn.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Select ON Script")
                    .setItems(scriptNames, (d, which) -> {
                        onScriptPath[0] = scriptFiles.get(which).getPath();
                        btnPickOn.setText("ON Script: " + scriptFiles.get(which).getLabel());
                    })
                    .show();
        });
        layout.addView(btnPickOn);

        MaterialButton btnPickOff = new MaterialButton(this);
        btnPickOff.setText("OFF Script: " + (offScriptPath[0].isEmpty() ? "Select..." : getScriptDisplayName(scriptFiles, offScriptPath[0])));
        btnPickOff.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Select OFF Script")
                    .setItems(scriptNames, (d, which) -> {
                        offScriptPath[0] = scriptFiles.get(which).getPath();
                        btnPickOff.setText("OFF Script: " + scriptFiles.get(which).getLabel());
                    })
                    .show();
        });
        layout.addView(btnPickOff);

        MaterialButton btnPickIcon = new MaterialButton(this);
        if (IconHelper.isCustomFile(selectedIconId[0])) {
            btnPickIcon.setText("Custom Image");
        } else {
            IconRegistry.IconItem item = IconRegistry.getIcon(selectedIconId[0]);
            btnPickIcon.setText("Select Icon: " + (item != null ? item.name : selectedIconId[0]));
            btnPickIcon.setIconResource(IconRegistry.getDrawableResId(selectedIconId[0], R.drawable.ic_lucide_power));
        }
        btnPickIcon.setOnClickListener(v -> {
            IconPickerBottomSheet picker = IconPickerBottomSheet.newInstance();
            picker.setOnIconSelectedListener(iconId -> {
                selectedIconId[0] = iconId;
                if (IconHelper.isCustomFile(iconId)) {
                    btnPickIcon.setText("Custom Image");
                    btnPickIcon.setIcon(null);
                } else {
                    IconRegistry.IconItem item = IconRegistry.getIcon(iconId);
                    btnPickIcon.setText("Select Icon: " + (item != null ? item.name : iconId));
                    btnPickIcon.setIconResource(IconRegistry.getDrawableResId(iconId, R.drawable.ic_lucide_power));
                }
            });
            picker.show(getSupportFragmentManager(), "toggle_icon_picker");
        });
        layout.addView(btnPickIcon);

        CheckBox chkInitialOn = new CheckBox(this);
        chkInitialOn.setText("State is currently ON");
        if (existingToggle != null) chkInitialOn.setChecked(existingToggle.isOn);
        layout.addView(chkInitialOn);

        new MaterialAlertDialogBuilder(this)
                .setTitle(existingToggle != null ? "Edit Toggle Shortcut" : "New Toggle Shortcut")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = inputName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Toggle name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (onScriptPath[0].isEmpty() || offScriptPath[0].isEmpty()) {
                        Toast.makeText(this, "Both ON and OFF scripts must be chosen", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String id = existingToggle != null ? existingToggle.id : ("toggle_" + System.currentTimeMillis());
                    ToggleShortcut toggle = new ToggleShortcut(id, name, onScriptPath[0], offScriptPath[0], selectedIconId[0], chkInitialOn.isChecked());
                    TermuxTogglePreferences.saveToggle(this, toggle);
                    populateDashboard();
                    sendIntentToRefreshAllWidgets();
                    Toast.makeText(this, "Saved toggle: " + name, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getScriptDisplayName(List<ShortcutFile> scriptFiles, String path) {
        for (ShortcutFile sf : scriptFiles) {
            if (sf.getPath().equals(path)) {
                return sf.getLabel();
            }
        }
        return com.termux.shared.shell.ShellUtils.getExecutableBasename(path);
    }

    private void showCommandOptionsDialog(@NonNull ShortcutFile sf) {
        String cmdId = sf.getPath().replace("custom:", "");
        CustomCommand cmd = TermuxCustomCommandPreferences.getCommandById(this, cmdId);
        if (cmd == null) return;

        CharSequence[] options = new CharSequence[]{
                "Run Command",
                "Add to Home Screen (Widget)",
                "Edit Command",
                "Delete Command"
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(cmd.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        TermuxWidgetProvider.sendExecutionIntentToTermuxService(this, sf.getPath(), LOG_TAG);
                    } else if (which == 1) {
                        TermuxButtonWidgetUtils.pinButtonWidget(this, sf.getPath(), sf.getLabel(), sf.mIconId);
                    } else if (which == 2) {
                        showAddEditCommandDialog(cmd);
                    } else if (which == 3) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Delete Command")
                                .setMessage("Are you sure you want to delete '" + cmd.name + "'?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    TermuxCustomCommandPreferences.deleteCustomCommand(this, cmd.id);
                                    populateDashboard();
                                    sendIntentToRefreshAllWidgets(false);
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
        if (IconHelper.isCustomFile(selectedIconId[0])) {
            btnPickIcon.setText("Custom Image");
        } else {
            btnPickIcon.setText("Select Icon: " + selectedIconId[0]);
            btnPickIcon.setIconResource(IconRegistry.getDrawableResId(selectedIconId[0], R.drawable.ic_lucide_terminal));
        }
        btnPickIcon.setOnClickListener(v -> {
            IconPickerBottomSheet picker = IconPickerBottomSheet.newInstance();
            picker.setOnIconSelectedListener(iconId -> {
                selectedIconId[0] = iconId;
                if (IconHelper.isCustomFile(iconId)) {
                    btnPickIcon.setText("Custom Image");
                    btnPickIcon.setIcon(null);
                } else {
                    IconRegistry.IconItem item = IconRegistry.getIcon(iconId);
                    String iconName = item != null ? item.name : iconId;
                    btnPickIcon.setText("Select Icon: " + iconName);
                    btnPickIcon.setIconResource(IconRegistry.getDrawableResId(iconId, R.drawable.ic_lucide_terminal));
                }
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
        sendIntentToRefreshAllWidgets(false);
    }

    public void sendIntentToRefreshAllWidgets(boolean showToast) {
        TermuxWidgetProvider.sendIntentToRefreshAllWidgets(this, LOG_TAG);
        if (showToast) {
            Toast.makeText(this, "Widgets Refreshed", Toast.LENGTH_SHORT).show();
        }
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
