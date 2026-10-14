package com.termux.widget.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.termux.widget.R;
import com.termux.widget.TermuxWidgetProvider;
import com.termux.widget.TermuxWidgetStylePreferences;
import com.termux.widget.theme.ThemeManager;
import com.termux.widget.utils.BouncyAnimationHelper;

public class TermuxWidgetSettingsActivity extends AppCompatActivity {

    private MaterialCardView mCardPreview;

    // Multi-Widget Previews
    private View mLayoutPreviewButton;
    private View mLayoutPreviewList;
    private View mLayoutPreviewGrid;

    // Button Preview Elements
    private View mPreviewIconFrame;
    private ImageView mPreviewWidgetIconBg;
    private ImageView mPreviewIcon;
    private TextView mPreviewTitle;
    private TextView mPreviewSubtitle;

    // List Preview Elements
    private View mPreviewListItem1;
    private View mPreviewListItem1Frame;
    private ImageView mPreviewListItem1BadgeBg;
    private ImageView mPreviewListItem1Icon;
    private TextView mPreviewListItem1Text;
    private View mPreviewListItem2;
    private View mPreviewListItem2Frame;
    private ImageView mPreviewListItem2BadgeBg;
    private ImageView mPreviewListItem2Icon;
    private TextView mPreviewListItem2Text;

    // Grid Preview Elements
    private View mPreviewGridItem1;
    private View mPreviewGridItem1Frame;
    private ImageView mPreviewGridItem1BadgeBg;
    private ImageView mPreviewGridItem1Icon;
    private TextView mPreviewGridItem1Text;
    private View mPreviewGridItem2;
    private View mPreviewGridItem2Frame;
    private ImageView mPreviewGridItem2BadgeBg;
    private ImageView mPreviewGridItem2Icon;
    private TextView mPreviewGridItem2Text;

    private TextView mTxtCornerVal;
    private TextView mTxtTextVal;
    private TextView mTxtIconVal;
    private TextView mTxtGridVal;

    private Slider mSliderCorner;
    private Slider mSliderText;
    private Slider mSliderIcon;
    private Slider mSliderGrid;
    private MaterialSwitch mSwitchTransparency;
    private View mLayoutTransparencySlider;
    private Slider mSliderTransparency;
    private TextView mTxtTransparencyVal;
    private MaterialSwitch mSwitchMaterialYou;
    private View mLayoutCustomColors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_termux_widget_settings);

        MaterialToolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        mCardPreview = findViewById(R.id.card_widget_live_preview);

        mLayoutPreviewButton = findViewById(R.id.layout_preview_button);
        mLayoutPreviewList = findViewById(R.id.layout_preview_list);
        mLayoutPreviewGrid = findViewById(R.id.layout_preview_grid);

        // Button Preview
        mPreviewIconFrame = findViewById(R.id.preview_icon_frame);
        mPreviewWidgetIconBg = findViewById(R.id.preview_widget_icon_bg);
        mPreviewIcon = findViewById(R.id.preview_widget_icon);
        mPreviewTitle = findViewById(R.id.preview_widget_title);
        mPreviewSubtitle = findViewById(R.id.preview_widget_subtitle);

        // List Preview
        mPreviewListItem1 = findViewById(R.id.preview_list_item1);
        mPreviewListItem1Frame = findViewById(R.id.preview_list_item1_frame);
        mPreviewListItem1BadgeBg = findViewById(R.id.preview_list_item1_badge_bg);
        mPreviewListItem1Icon = findViewById(R.id.preview_list_item1_icon);
        mPreviewListItem1Text = findViewById(R.id.preview_list_item1_text);
        mPreviewListItem2 = findViewById(R.id.preview_list_item2);
        mPreviewListItem2Frame = findViewById(R.id.preview_list_item2_frame);
        mPreviewListItem2BadgeBg = findViewById(R.id.preview_list_item2_badge_bg);
        mPreviewListItem2Icon = findViewById(R.id.preview_list_item2_icon);
        mPreviewListItem2Text = findViewById(R.id.preview_list_item2_text);

        // Grid Preview
        mPreviewGridItem1 = findViewById(R.id.preview_grid_item1);
        mPreviewGridItem1Frame = findViewById(R.id.preview_grid_item1_frame);
        mPreviewGridItem1BadgeBg = findViewById(R.id.preview_grid_item1_badge_bg);
        mPreviewGridItem1Icon = findViewById(R.id.preview_grid_item1_icon);
        mPreviewGridItem1Text = findViewById(R.id.preview_grid_item1_text);
        mPreviewGridItem2 = findViewById(R.id.preview_grid_item2);
        mPreviewGridItem2Frame = findViewById(R.id.preview_grid_item2_frame);
        mPreviewGridItem2BadgeBg = findViewById(R.id.preview_grid_item2_badge_bg);
        mPreviewGridItem2Icon = findViewById(R.id.preview_grid_item2_icon);
        mPreviewGridItem2Text = findViewById(R.id.preview_grid_item2_text);

        mTxtCornerVal = findViewById(R.id.txt_val_corner_radius);
        mTxtTextVal = findViewById(R.id.txt_val_text_size);
        mTxtIconVal = findViewById(R.id.txt_val_icon_size);
        mTxtGridVal = findViewById(R.id.txt_val_grid_spacing);

        mSliderCorner = findViewById(R.id.slider_corner_radius);
        mSliderText = findViewById(R.id.slider_text_size);
        mSliderIcon = findViewById(R.id.slider_icon_size);
        mSliderGrid = findViewById(R.id.slider_grid_spacing);

        // Preview Type Selector
        MaterialButtonToggleGroup togglePreviewType = findViewById(R.id.toggle_group_preview_type);
        if (togglePreviewType != null) {
            togglePreviewType.check(R.id.btn_preview_button);
            togglePreviewType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                if (checkedId == R.id.btn_preview_button) {
                    if (mLayoutPreviewButton != null) mLayoutPreviewButton.setVisibility(View.VISIBLE);
                    if (mLayoutPreviewList != null) mLayoutPreviewList.setVisibility(View.GONE);
                    if (mLayoutPreviewGrid != null) mLayoutPreviewGrid.setVisibility(View.GONE);
                } else if (checkedId == R.id.btn_preview_list) {
                    if (mLayoutPreviewButton != null) mLayoutPreviewButton.setVisibility(View.GONE);
                    if (mLayoutPreviewList != null) mLayoutPreviewList.setVisibility(View.VISIBLE);
                    if (mLayoutPreviewGrid != null) mLayoutPreviewGrid.setVisibility(View.GONE);
                } else if (checkedId == R.id.btn_preview_grid) {
                    if (mLayoutPreviewButton != null) mLayoutPreviewButton.setVisibility(View.GONE);
                    if (mLayoutPreviewList != null) mLayoutPreviewList.setVisibility(View.GONE);
                    if (mLayoutPreviewGrid != null) mLayoutPreviewGrid.setVisibility(View.VISIBLE);
                }
            });
        }

        // AMOLED Switch
        MaterialSwitch switchAmoled = findViewById(R.id.switch_settings_amoled);
        if (switchAmoled != null) {
            switchAmoled.setChecked(ThemeManager.isAmoledMode(this));
            switchAmoled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked == ThemeManager.isAmoledMode(this)) return;
                ThemeManager.setAmoledMode(this, isChecked);
                TermuxWidgetProvider.sendIntentToRefreshAllWidgets(this, "SettingsAmoledChange");
                buttonView.postDelayed(this::recreate, 220);
            });
        }

        // Widget Transparency Switch & Slider
        mSwitchTransparency = findViewById(R.id.switch_settings_transparency);
        mLayoutTransparencySlider = findViewById(R.id.layout_transparency_slider);
        mSliderTransparency = findViewById(R.id.slider_transparency);
        mTxtTransparencyVal = findViewById(R.id.txt_val_transparency);

        boolean isTransEnabled = TermuxWidgetStylePreferences.isTransparencyEnabled(this);
        int currentTransPercent = TermuxWidgetStylePreferences.getTransparencyPercent(this);

        if (mSwitchTransparency != null) {
            mSwitchTransparency.setChecked(isTransEnabled);
            if (mLayoutTransparencySlider != null) {
                mLayoutTransparencySlider.setVisibility(isTransEnabled ? View.VISIBLE : View.GONE);
            }
            mSwitchTransparency.setOnCheckedChangeListener((buttonView, isChecked) -> {
                TermuxWidgetStylePreferences.setTransparencyEnabled(this, isChecked);
                if (mLayoutTransparencySlider != null) {
                    mLayoutTransparencySlider.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
                updatePreviewBlurAndTheme();
                TermuxWidgetProvider.sendIntentToRefreshAllWidgets(this, "TransparencyToggleChange");
            });
        }

        if (mSliderTransparency != null) {
            mSliderTransparency.setValue(Math.max(10, Math.min(90, currentTransPercent)));
            if (mTxtTransparencyVal != null) {
                mTxtTransparencyVal.setText(currentTransPercent + "%");
            }
            mSliderTransparency.addOnChangeListener((slider, value, fromUser) -> {
                int val = (int) value;
                if (mTxtTransparencyVal != null) {
                    mTxtTransparencyVal.setText(val + "%");
                }
                updatePreviewBlurAndTheme();
            });
            mSliderTransparency.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(Slider slider) {}

                @Override
                public void onStopTrackingTouch(Slider slider) {
                    int val = (int) slider.getValue();
                    TermuxWidgetStylePreferences.setTransparencyPercent(TermuxWidgetSettingsActivity.this, val);
                    TermuxWidgetProvider.sendIntentToRefreshAllWidgets(TermuxWidgetSettingsActivity.this, "TransparencyPercentChange");
                }
            });
        }

        // Material You Dynamic Colors Switch & Custom Colors
        mSwitchMaterialYou = findViewById(R.id.switch_settings_material_you);
        mLayoutCustomColors = findViewById(R.id.layout_custom_colors);
        boolean isCustomColors = TermuxWidgetStylePreferences.isCustomColorsEnabled(this);

        if (mSwitchMaterialYou != null) {
            mSwitchMaterialYou.setChecked(!isCustomColors);
            if (mLayoutCustomColors != null) {
                mLayoutCustomColors.setVisibility(isCustomColors ? View.VISIBLE : View.GONE);
            }
            mSwitchMaterialYou.setOnCheckedChangeListener((buttonView, isChecked) -> {
                boolean useCustom = !isChecked;
                TermuxWidgetStylePreferences.setCustomColorsEnabled(this, useCustom);
                if (mLayoutCustomColors != null) {
                    mLayoutCustomColors.setVisibility(useCustom ? View.VISIBLE : View.GONE);
                }
                updatePreviewBlurAndTheme();
                TermuxWidgetProvider.sendIntentToRefreshAllWidgets(this, "MaterialYouToggle");
            });
        }

        // Rainbow Color Swatches
        setupRainbowCircle(R.id.circle_color_red, 0xFFEF4444);
        setupRainbowCircle(R.id.circle_color_orange, 0xFFF97316);
        setupRainbowCircle(R.id.circle_color_amber, 0xFFF59E0B);
        setupRainbowCircle(R.id.circle_color_yellow, 0xFFEAB308);
        setupRainbowCircle(R.id.circle_color_lime, 0xFF84CC16);
        setupRainbowCircle(R.id.circle_color_green, 0xFF10B981);
        setupRainbowCircle(R.id.circle_color_teal, 0xFF14B8A6);
        setupRainbowCircle(R.id.circle_color_cyan, 0xFF06B6D4);
        setupRainbowCircle(R.id.circle_color_blue, 0xFF3B82F6);
        setupRainbowCircle(R.id.circle_color_indigo, 0xFF6366F1);
        setupRainbowCircle(R.id.circle_color_purple, 0xFF8B5CF6);
        setupRainbowCircle(R.id.circle_color_pink, 0xFFEC4899);
        setupRainbowCircle(R.id.circle_color_white, 0xFFFFFFFF);

        // Add Custom Color Button (Grey circle with +)
        View circleAdd = findViewById(R.id.circle_color_add);
        if (circleAdd != null) {
            BouncyAnimationHelper.applySpringPress(circleAdd, 0.92f);
            circleAdd.setOnClickListener(v -> showColorPickerDialog());
        }

        // Sliders Setup
        int currentCorner = TermuxWidgetStylePreferences.getCornerRadiusDp(this);
        int currentText = TermuxWidgetStylePreferences.getTextSizeSp(this);
        int currentIcon = TermuxWidgetStylePreferences.getIconSizeDp(this);
        int currentGridSpacing = TermuxWidgetStylePreferences.getGridSpacingDp(this);

        if (mSliderCorner != null) {
            mSliderCorner.setValue(Math.max(0, Math.min(32, currentCorner)));
            mTxtCornerVal.setText(currentCorner + " dp");
            mSliderCorner.addOnChangeListener((slider, value, fromUser) -> {
                int val = (int) value;
                mTxtCornerVal.setText(val + " dp");
                updatePreviewCornerRadius(val);
            });
            mSliderCorner.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(Slider slider) {}

                @Override
                public void onStopTrackingTouch(Slider slider) {
                    int val = (int) slider.getValue();
                    TermuxWidgetStylePreferences.setCornerRadiusDp(TermuxWidgetSettingsActivity.this, val);
                    TermuxWidgetProvider.sendIntentToRefreshAllWidgets(TermuxWidgetSettingsActivity.this, "CornerRadiusChange");
                }
            });
        }

        if (mSliderText != null) {
            mSliderText.setValue(Math.max(10, Math.min(20, currentText)));
            mTxtTextVal.setText(currentText + " sp");
            mSliderText.addOnChangeListener((slider, value, fromUser) -> {
                int val = (int) value;
                mTxtTextVal.setText(val + " sp");
                updatePreviewTextSize(val);
            });
            mSliderText.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(Slider slider) {}

                @Override
                public void onStopTrackingTouch(Slider slider) {
                    int val = (int) slider.getValue();
                    TermuxWidgetStylePreferences.setTextSizeSp(TermuxWidgetSettingsActivity.this, val);
                    TermuxWidgetProvider.sendIntentToRefreshAllWidgets(TermuxWidgetSettingsActivity.this, "TextSizeChange");
                }
            });
        }

        if (mSliderIcon != null) {
            mSliderIcon.setValue(Math.max(16, Math.min(36, currentIcon)));
            mTxtIconVal.setText(currentIcon + " dp");
            mSliderIcon.addOnChangeListener((slider, value, fromUser) -> {
                int val = (int) value;
                mTxtIconVal.setText(val + " dp");
                updatePreviewIconSize(val);
            });
            mSliderIcon.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(Slider slider) {}

                @Override
                public void onStopTrackingTouch(Slider slider) {
                    int val = (int) slider.getValue();
                    TermuxWidgetStylePreferences.setIconSizeDp(TermuxWidgetSettingsActivity.this, val);
                    TermuxWidgetProvider.sendIntentToRefreshAllWidgets(TermuxWidgetSettingsActivity.this, "IconSizeChange");
                }
            });
        }

        if (mSliderGrid != null) {
            mSliderGrid.setValue(Math.max(0, Math.min(48, currentGridSpacing)));
            if (mTxtGridVal != null) mTxtGridVal.setText(currentGridSpacing + " dp");
            mSliderGrid.addOnChangeListener((slider, value, fromUser) -> {
                int val = (int) value;
                if (mTxtGridVal != null) mTxtGridVal.setText(val + " dp");
                updatePreviewGridSpacing(val);
            });
            mSliderGrid.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(Slider slider) {}

                @Override
                public void onStopTrackingTouch(Slider slider) {
                    int val = (int) slider.getValue();
                    TermuxWidgetStylePreferences.setGridSpacingDp(TermuxWidgetSettingsActivity.this, val);
                    TermuxWidgetProvider.sendIntentToRefreshAllWidgets(TermuxWidgetSettingsActivity.this, "GridSpacingChange");
                }
            });
        }

        // Reset to Defaults Button
        MaterialButton btnReset = findViewById(R.id.btn_reset_widget_styling);
        if (btnReset != null) {
            BouncyAnimationHelper.applySpringPress(btnReset, 0.95f);
            btnReset.setOnClickListener(v -> {
                TermuxWidgetStylePreferences.resetToDefaults(this);
                if (mSliderCorner != null) mSliderCorner.setValue(TermuxWidgetStylePreferences.DEFAULT_CORNER_RADIUS_DP);
                if (mSliderText != null) mSliderText.setValue(TermuxWidgetStylePreferences.DEFAULT_TEXT_SIZE_SP);
                if (mSliderIcon != null) mSliderIcon.setValue(TermuxWidgetStylePreferences.DEFAULT_ICON_SIZE_DP);
                if (mSliderGrid != null) mSliderGrid.setValue(TermuxWidgetStylePreferences.DEFAULT_GRID_SPACING_DP);
                if (mSwitchTransparency != null) mSwitchTransparency.setChecked(TermuxWidgetStylePreferences.DEFAULT_TRANSPARENCY_ENABLED);
                if (mLayoutTransparencySlider != null) mLayoutTransparencySlider.setVisibility(View.GONE);
                if (mSliderTransparency != null) mSliderTransparency.setValue(TermuxWidgetStylePreferences.DEFAULT_TRANSPARENCY_PERCENT);
                if (mTxtTransparencyVal != null) mTxtTransparencyVal.setText(TermuxWidgetStylePreferences.DEFAULT_TRANSPARENCY_PERCENT + "%");
                if (mSwitchMaterialYou != null) mSwitchMaterialYou.setChecked(true);
                if (mLayoutCustomColors != null) mLayoutCustomColors.setVisibility(View.GONE);

                updatePreviewCornerRadius(TermuxWidgetStylePreferences.DEFAULT_CORNER_RADIUS_DP);
                updatePreviewTextSize(TermuxWidgetStylePreferences.DEFAULT_TEXT_SIZE_SP);
                updatePreviewIconSize(TermuxWidgetStylePreferences.DEFAULT_ICON_SIZE_DP);
                updatePreviewGridSpacing(TermuxWidgetStylePreferences.DEFAULT_GRID_SPACING_DP);
                updatePreviewBlurAndTheme();

                TermuxWidgetProvider.sendIntentToRefreshAllWidgets(this, "ResetStyles");
                Toast.makeText(this, "Widget styling reset to defaults", Toast.LENGTH_SHORT).show();
            });
        }

        // Permissions & Setup Buttons
        MaterialButton btnPerms = findViewById(R.id.btn_settings_permissions);
        if (btnPerms != null) {
            BouncyAnimationHelper.applySpringPress(btnPerms, 0.96f);
            btnPerms.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        }

        MaterialButton btnCopySetup = findViewById(R.id.btn_settings_copy_setup);
        if (btnCopySetup != null) {
            BouncyAnimationHelper.applySpringPress(btnCopySetup, 0.96f);
            btnCopySetup.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Termux Setup", "echo \"allow-external-apps = true\" >> ~/.termux/termux.properties && termux-reload-settings");
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Setup command copied! Run in Termux.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Initial preview rendering
        updatePreviewCornerRadius(currentCorner);
        updatePreviewTextSize(currentText);
        updatePreviewIconSize(currentIcon);
        updatePreviewGridSpacing(currentGridSpacing);

        applyAmoledThemeIfNeeded();
        updatePreviewBlurAndTheme();
    }

    private void setupRainbowCircle(int resId, int primaryColor) {
        View circle = findViewById(resId);
        if (circle != null) {
            BouncyAnimationHelper.applySpringPress(circle, 0.92f);
            circle.setOnClickListener(v -> {
                TermuxWidgetStylePreferences.setCustomColorsEnabled(this, true);
                TermuxWidgetStylePreferences.setCustomPrimaryColor(this, primaryColor);
                int textColor = androidx.core.graphics.ColorUtils.calculateLuminance(primaryColor) > 0.6 ? Color.BLACK : Color.WHITE;
                TermuxWidgetStylePreferences.setCustomTextColor(this, textColor);
                if (mSwitchMaterialYou != null) mSwitchMaterialYou.setChecked(false);
                if (mLayoutCustomColors != null) mLayoutCustomColors.setVisibility(View.VISIBLE);
                updatePreviewBlurAndTheme();
                TermuxWidgetProvider.sendIntentToRefreshAllWidgets(this, "CustomColorRainbow");
                Toast.makeText(this, "Accent color applied", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showColorPickerDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_color_picker, null);
        MaterialCardView cardPreview = dialogView.findViewById(R.id.card_color_picker_preview);
        TextView txtHex = dialogView.findViewById(R.id.txt_color_picker_hex);
        Slider sliderHue = dialogView.findViewById(R.id.slider_color_picker_hue);
        com.google.android.material.textfield.TextInputEditText etHex = dialogView.findViewById(R.id.et_color_picker_hex);
        GridLayout gridSwatches = dialogView.findViewById(R.id.grid_color_swatches);

        int initialColor = TermuxWidgetStylePreferences.getCustomPrimaryColor(this);
        final int[] currentColor = new int[]{initialColor};

        Runnable updateViews = () -> {
            int c = currentColor[0];
            String hexStr = String.format("#%06X", (0xFFFFFF & c));
            if (cardPreview != null) cardPreview.setCardBackgroundColor(c);
            if (txtHex != null) {
                txtHex.setText(hexStr);
                txtHex.setTextColor(androidx.core.graphics.ColorUtils.calculateLuminance(c) > 0.5 ? Color.BLACK : Color.WHITE);
            }
            if (etHex != null && !etHex.getText().toString().equalsIgnoreCase(hexStr)) {
                etHex.setText(hexStr);
            }
        };

        float[] hsv = new float[3];
        Color.colorToHSV(initialColor, hsv);
        if (sliderHue != null) {
            sliderHue.setValue(Math.max(0, Math.min(360, hsv[0])));
            sliderHue.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser) {
                    float[] currentHsv = new float[]{value, 0.85f, 0.95f};
                    currentColor[0] = Color.HSVToColor(currentHsv);
                    updateViews.run();
                }
            });
        }

        if (etHex != null) {
            etHex.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    String str = s.toString().trim();
                    if (!str.startsWith("#")) str = "#" + str;
                    if (str.length() == 7) {
                        try {
                            int parsed = Color.parseColor(str);
                            currentColor[0] = parsed;
                            float[] newHsv = new float[3];
                            Color.colorToHSV(parsed, newHsv);
                            if (sliderHue != null) sliderHue.setValue(Math.max(0, Math.min(360, newHsv[0])));
                            if (cardPreview != null) cardPreview.setCardBackgroundColor(parsed);
                            if (txtHex != null) {
                                txtHex.setText(str.toUpperCase());
                                txtHex.setTextColor(androidx.core.graphics.ColorUtils.calculateLuminance(parsed) > 0.5 ? Color.BLACK : Color.WHITE);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            });
        }

        int[] swatches = {
                0xFFEF4444, 0xFFF97316, 0xFFF59E0B, 0xFFEAB308, 0xFF84CC16, 0xFF10B981,
                0xFF14B8A6, 0xFF06B6D4, 0xFF3B82F6, 0xFF6366F1, 0xFF8B5CF6, 0xFFEC4899,
                0xFFF43F5E, 0xFFD946EF, 0xFF64748B, 0xFF0EA5E9, 0xFF22C55E, 0xFFFFFFFF
        };

        if (gridSwatches != null) {
            int swatchSize = dpToPx(38);
            int margin = dpToPx(4);
            for (int swColor : swatches) {
                MaterialCardView swatchCard = new MaterialCardView(this);
                GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
                glp.width = swatchSize;
                glp.height = swatchSize;
                glp.setMargins(margin, margin, margin, margin);
                swatchCard.setLayoutParams(glp);
                swatchCard.setRadius(swatchSize / 2f);
                swatchCard.setCardBackgroundColor(swColor);
                swatchCard.setStrokeWidth(dpToPx(2));
                swatchCard.setStrokeColor(Color.parseColor("#33FFFFFF"));
                BouncyAnimationHelper.applySpringPress(swatchCard, 0.90f);
                swatchCard.setOnClickListener(v -> {
                    currentColor[0] = swColor;
                    float[] newHsv = new float[3];
                    Color.colorToHSV(swColor, newHsv);
                    if (sliderHue != null) sliderHue.setValue(Math.max(0, Math.min(360, newHsv[0])));
                    updateViews.run();
                });
                gridSwatches.addView(swatchCard);
            }
        }

        updateViews.run();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Accent Color")
                .setView(dialogView)
                .setPositiveButton("Apply", (dialog, which) -> {
                    int chosen = currentColor[0];
                    TermuxWidgetStylePreferences.setCustomColorsEnabled(this, true);
                    TermuxWidgetStylePreferences.setCustomPrimaryColor(this, chosen);
                    int textColor = androidx.core.graphics.ColorUtils.calculateLuminance(chosen) > 0.6 ? Color.BLACK : Color.WHITE;
                    TermuxWidgetStylePreferences.setCustomTextColor(this, textColor);
                    if (mSwitchMaterialYou != null) mSwitchMaterialYou.setChecked(false);
                    if (mLayoutCustomColors != null) mLayoutCustomColors.setVisibility(View.VISIBLE);
                    updatePreviewBlurAndTheme();
                    TermuxWidgetProvider.sendIntentToRefreshAllWidgets(this, "CustomColorPickerApply");
                    Toast.makeText(this, "Custom color applied", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePreviewCornerRadius(int radiusDp) {
        if (mCardPreview != null) {
            mCardPreview.setRadius(dpToPx(radiusDp));
        }
    }

    private void updatePreviewTextSize(int sizeSp) {
        if (mPreviewTitle != null) mPreviewTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        if (mPreviewSubtitle != null) mPreviewSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(9, sizeSp - 2));

        if (mPreviewListItem1Text != null) mPreviewListItem1Text.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        if (mPreviewListItem2Text != null) mPreviewListItem2Text.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);

        if (mPreviewGridItem1Text != null) mPreviewGridItem1Text.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(9, sizeSp - 1));
        if (mPreviewGridItem2Text != null) mPreviewGridItem2Text.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(9, sizeSp - 1));
    }

    private void updatePreviewIconSize(int sizeDp) {
        int badgeSizeDp = Math.round(sizeDp * 1.6f);

        // Button Preview
        updateViewSize(mPreviewIconFrame, badgeSizeDp, badgeSizeDp);
        updateViewSize(mPreviewIcon, sizeDp, sizeDp);

        // List Preview
        updateViewSize(mPreviewListItem1Frame, badgeSizeDp, badgeSizeDp);
        updateViewSize(mPreviewListItem1Icon, sizeDp, sizeDp);
        updateViewSize(mPreviewListItem2Frame, badgeSizeDp, badgeSizeDp);
        updateViewSize(mPreviewListItem2Icon, sizeDp, sizeDp);

        // Grid Preview
        updateViewSize(mPreviewGridItem1Frame, badgeSizeDp, badgeSizeDp);
        updateViewSize(mPreviewGridItem1Icon, sizeDp, sizeDp);
        updateViewSize(mPreviewGridItem2Frame, badgeSizeDp, badgeSizeDp);
        updateViewSize(mPreviewGridItem2Icon, sizeDp, sizeDp);
    }

    private void updatePreviewGridSpacing(int spacingDp) {
        if (mPreviewGridItem1 != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mPreviewGridItem1.getLayoutParams();
            if (lp != null) {
                lp.rightMargin = dpToPx(spacingDp / 2);
                mPreviewGridItem1.setLayoutParams(lp);
            }
        }
        if (mPreviewGridItem2 != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mPreviewGridItem2.getLayoutParams();
            if (lp != null) {
                lp.leftMargin = dpToPx(spacingDp / 2);
                mPreviewGridItem2.setLayoutParams(lp);
            }
        }
    }

    private void updateViewSize(View view, int widthDp, int heightDp) {
        if (view == null) return;
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp != null) {
            lp.width = dpToPx(widthDp);
            lp.height = dpToPx(heightDp);
            view.setLayoutParams(lp);
        }
    }

    private void updatePreviewBlurAndTheme() {
        boolean isTrans = TermuxWidgetStylePreferences.isTransparencyEnabled(this);
        int transPercent = TermuxWidgetStylePreferences.getTransparencyPercent(this);
        float cardAlpha = isTrans ? Math.max(0.1f, Math.min(1.0f, (100 - transPercent) / 100f)) : 1.0f;

        if (mCardPreview != null) mCardPreview.setAlpha(cardAlpha);
        if (mPreviewListItem1 != null) mPreviewListItem1.setAlpha(cardAlpha);
        if (mPreviewListItem2 != null) mPreviewListItem2.setAlpha(cardAlpha);
        if (mPreviewGridItem1 != null) mPreviewGridItem1.setAlpha(cardAlpha);
        if (mPreviewGridItem2 != null) mPreviewGridItem2.setAlpha(cardAlpha);

        boolean isAmoled = ThemeManager.isAmoledMode(this);
        boolean isCustom = TermuxWidgetStylePreferences.isCustomColorsEnabled(this);
        int customPrimary = TermuxWidgetStylePreferences.getCustomPrimaryColor(this);
        int customText = TermuxWidgetStylePreferences.getCustomTextColor(this);

        if (isAmoled) {
            if (mCardPreview != null) {
                mCardPreview.setCardBackgroundColor(Color.parseColor("#0A0A0A"));
                mCardPreview.setStrokeColor(Color.parseColor("#262626"));
            }
            if (mPreviewIconFrame != null) mPreviewIconFrame.setBackgroundResource(R.drawable.widget_icon_badge_amoled_bg);
            if (mPreviewWidgetIconBg != null) mPreviewWidgetIconBg.setColorFilter(Color.parseColor("#262626"));
            if (mPreviewTitle != null) mPreviewTitle.setTextColor(Color.WHITE);
            if (mPreviewSubtitle != null) mPreviewSubtitle.setTextColor(Color.parseColor("#9E9E9E"));
            if (mPreviewIcon != null) mPreviewIcon.setColorFilter(Color.WHITE);

            // List
            if (mPreviewListItem1 != null) mPreviewListItem1.setBackgroundResource(R.drawable.widget_button_single_amoled_bg);
            if (mPreviewListItem1Frame != null) mPreviewListItem1Frame.setBackgroundResource(R.drawable.widget_icon_badge_amoled_bg);
            if (mPreviewListItem1BadgeBg != null) mPreviewListItem1BadgeBg.setColorFilter(Color.parseColor("#262626"));
            if (mPreviewListItem1Text != null) mPreviewListItem1Text.setTextColor(Color.WHITE);
            if (mPreviewListItem1Icon != null) mPreviewListItem1Icon.setColorFilter(Color.WHITE);

            // High-contrast AMOLED toggle on: dark container, white outline, white circle badge, black icon, white text
            if (mPreviewListItem2 != null) mPreviewListItem2.setBackgroundResource(R.drawable.widget_toggle_card_amoled_on_bg);
            if (mPreviewListItem2Frame != null) mPreviewListItem2Frame.setBackgroundResource(R.drawable.widget_toggle_badge_amoled_on_bg);
            if (mPreviewListItem2BadgeBg != null) mPreviewListItem2BadgeBg.setColorFilter(Color.WHITE);
            if (mPreviewListItem2Text != null) mPreviewListItem2Text.setTextColor(Color.WHITE);
            if (mPreviewListItem2Icon != null) mPreviewListItem2Icon.setColorFilter(Color.BLACK);

            // Grid
            if (mPreviewGridItem1 != null) mPreviewGridItem1.setBackgroundResource(R.drawable.widget_grid_card_amoled_bg);
            if (mPreviewGridItem1Frame != null) mPreviewGridItem1Frame.setBackgroundResource(R.drawable.widget_icon_badge_amoled_bg);
            if (mPreviewGridItem1BadgeBg != null) mPreviewGridItem1BadgeBg.setColorFilter(Color.parseColor("#262626"));
            if (mPreviewGridItem1Text != null) mPreviewGridItem1Text.setTextColor(Color.WHITE);
            if (mPreviewGridItem1Icon != null) mPreviewGridItem1Icon.setColorFilter(Color.WHITE);

            if (mPreviewGridItem2 != null) mPreviewGridItem2.setBackgroundResource(R.drawable.widget_toggle_grid_card_amoled_on_bg);
            if (mPreviewGridItem2Frame != null) mPreviewGridItem2Frame.setBackgroundResource(R.drawable.widget_toggle_badge_amoled_on_bg);
            if (mPreviewGridItem2BadgeBg != null) mPreviewGridItem2BadgeBg.setColorFilter(Color.WHITE);
            if (mPreviewGridItem2Text != null) mPreviewGridItem2Text.setTextColor(Color.WHITE);
            if (mPreviewGridItem2Icon != null) mPreviewGridItem2Icon.setColorFilter(Color.BLACK);

        } else if (isCustom) {
            int iconContrast = androidx.core.graphics.ColorUtils.calculateLuminance(customPrimary) > 0.55 ? Color.BLACK : Color.WHITE;

            if (mCardPreview != null) {
                mCardPreview.setCardBackgroundColor(Color.parseColor("#18181B"));
                mCardPreview.setStrokeColor(Color.parseColor("#27272A"));
            }
            if (mPreviewIconFrame != null) mPreviewIconFrame.setBackgroundResource(R.drawable.widget_icon_badge_bg);
            if (mPreviewWidgetIconBg != null) mPreviewWidgetIconBg.setColorFilter(customPrimary);
            if (mPreviewTitle != null) mPreviewTitle.setTextColor(customText);
            if (mPreviewSubtitle != null) mPreviewSubtitle.setTextColor(Color.parseColor("#A1A1AA"));
            if (mPreviewIcon != null) mPreviewIcon.setColorFilter(iconContrast);

            // List
            if (mPreviewListItem1 != null) mPreviewListItem1.setBackgroundResource(R.drawable.widget_button_single_bg);
            if (mPreviewListItem1Frame != null) mPreviewListItem1Frame.setBackgroundResource(R.drawable.widget_icon_badge_bg);
            if (mPreviewListItem1BadgeBg != null) mPreviewListItem1BadgeBg.setColorFilter(customPrimary);
            if (mPreviewListItem1Text != null) mPreviewListItem1Text.setTextColor(customText);
            if (mPreviewListItem1Icon != null) mPreviewListItem1Icon.setColorFilter(iconContrast);

            if (mPreviewListItem2 != null) mPreviewListItem2.setBackgroundResource(R.drawable.widget_toggle_card_on_bg);
            if (mPreviewListItem2Frame != null) mPreviewListItem2Frame.setBackgroundResource(R.drawable.widget_toggle_badge_on_bg);
            if (mPreviewListItem2BadgeBg != null) mPreviewListItem2BadgeBg.setColorFilter(Color.WHITE);
            if (mPreviewListItem2Text != null) mPreviewListItem2Text.setTextColor(Color.BLACK);
            if (mPreviewListItem2Icon != null) mPreviewListItem2Icon.setColorFilter(Color.BLACK);

            // Grid
            if (mPreviewGridItem1 != null) mPreviewGridItem1.setBackgroundResource(R.drawable.widget_grid_item_bg);
            if (mPreviewGridItem1Frame != null) mPreviewGridItem1Frame.setBackgroundResource(R.drawable.widget_icon_badge_bg);
            if (mPreviewGridItem1BadgeBg != null) mPreviewGridItem1BadgeBg.setColorFilter(customPrimary);
            if (mPreviewGridItem1Text != null) mPreviewGridItem1Text.setTextColor(customText);
            if (mPreviewGridItem1Icon != null) mPreviewGridItem1Icon.setColorFilter(iconContrast);

            if (mPreviewGridItem2 != null) mPreviewGridItem2.setBackgroundResource(R.drawable.widget_toggle_grid_card_on_bg);
            if (mPreviewGridItem2Frame != null) mPreviewGridItem2Frame.setBackgroundResource(R.drawable.widget_toggle_badge_on_bg);
            if (mPreviewGridItem2BadgeBg != null) mPreviewGridItem2BadgeBg.setColorFilter(Color.WHITE);
            if (mPreviewGridItem2Text != null) mPreviewGridItem2Text.setTextColor(Color.BLACK);
            if (mPreviewGridItem2Icon != null) mPreviewGridItem2Icon.setColorFilter(Color.BLACK);

        } else {
            // Material You Dynamic Colors
            if (mCardPreview != null) {
                TypedValue tvBg = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainer, tvBg, true);
                mCardPreview.setCardBackgroundColor(tvBg.data);
                TypedValue tvStroke = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, tvStroke, true);
                mCardPreview.setStrokeColor(tvStroke.data);
            }
            if (mPreviewIconFrame != null) mPreviewIconFrame.setBackgroundResource(R.drawable.widget_icon_badge_bg);
            if (mPreviewWidgetIconBg != null) mPreviewWidgetIconBg.setColorFilter(getColor(R.color.widget_accent_circle_bg));
            if (mPreviewTitle != null) {
                TypedValue tvOnSurface = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tvOnSurface, true);
                mPreviewTitle.setTextColor(tvOnSurface.data);
            }
            if (mPreviewSubtitle != null) {
                TypedValue tvSub = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, tvSub, true);
                mPreviewSubtitle.setTextColor(tvSub.data);
            }
            if (mPreviewIcon != null) {
                mPreviewIcon.setColorFilter(getColor(R.color.widget_accent_circle_icon));
            }

            // List
            if (mPreviewListItem1 != null) mPreviewListItem1.setBackgroundResource(R.drawable.widget_button_single_bg);
            if (mPreviewListItem1Frame != null) mPreviewListItem1Frame.setBackgroundResource(R.drawable.widget_icon_badge_bg);
            if (mPreviewListItem1BadgeBg != null) mPreviewListItem1BadgeBg.setColorFilter(getColor(R.color.widget_accent_circle_bg));
            if (mPreviewListItem1Text != null) {
                TypedValue tvOnSurface = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tvOnSurface, true);
                mPreviewListItem1Text.setTextColor(tvOnSurface.data);
            }
            if (mPreviewListItem1Icon != null) {
                mPreviewListItem1Icon.setColorFilter(getColor(R.color.widget_accent_circle_icon));
            }

            if (mPreviewListItem2 != null) mPreviewListItem2.setBackgroundResource(R.drawable.widget_toggle_card_on_bg);
            if (mPreviewListItem2Frame != null) mPreviewListItem2Frame.setBackgroundResource(R.drawable.widget_toggle_badge_on_bg);
            if (mPreviewListItem2BadgeBg != null) mPreviewListItem2BadgeBg.setColorFilter(getColor(R.color.widget_toggle_badge_on));
            if (mPreviewListItem2Text != null) {
                mPreviewListItem2Text.setTextColor(getColor(R.color.widget_toggle_text_on));
            }
            if (mPreviewListItem2Icon != null) {
                mPreviewListItem2Icon.setColorFilter(getColor(R.color.widget_toggle_icon_on));
            }

            // Grid
            if (mPreviewGridItem1 != null) mPreviewGridItem1.setBackgroundResource(R.drawable.widget_grid_item_bg);
            if (mPreviewGridItem1Frame != null) mPreviewGridItem1Frame.setBackgroundResource(R.drawable.widget_icon_badge_bg);
            if (mPreviewGridItem1BadgeBg != null) mPreviewGridItem1BadgeBg.setColorFilter(getColor(R.color.widget_accent_circle_bg));
            if (mPreviewGridItem1Text != null) {
                TypedValue tvOnSurface = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tvOnSurface, true);
                mPreviewGridItem1Text.setTextColor(tvOnSurface.data);
            }
            if (mPreviewGridItem1Icon != null) {
                mPreviewGridItem1Icon.setColorFilter(getColor(R.color.widget_accent_circle_icon));
            }

            if (mPreviewGridItem2 != null) mPreviewGridItem2.setBackgroundResource(R.drawable.widget_toggle_grid_card_on_bg);
            if (mPreviewGridItem2Frame != null) mPreviewGridItem2Frame.setBackgroundResource(R.drawable.widget_toggle_badge_on_bg);
            if (mPreviewGridItem2BadgeBg != null) mPreviewGridItem2BadgeBg.setColorFilter(getColor(R.color.widget_toggle_badge_on));
            if (mPreviewGridItem2Text != null) {
                mPreviewGridItem2Text.setTextColor(getColor(R.color.widget_toggle_text_on));
            }
            if (mPreviewGridItem2Icon != null) {
                mPreviewGridItem2Icon.setColorFilter(getColor(R.color.widget_toggle_icon_on));
            }
        }
    }

    private void applyAmoledThemeIfNeeded() {
        if (!ThemeManager.isAmoledMode(this)) return;

        View root = findViewById(R.id.settings_root_layout);
        if (root != null) root.setBackgroundColor(Color.BLACK);

        MaterialToolbar toolbar = findViewById(R.id.settings_toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(Color.BLACK);
            toolbar.setTitleTextColor(Color.WHITE);
            toolbar.setNavigationIconTint(Color.WHITE);
        }

        // AMOLED Headers
        int[] headerIds = {R.id.txt_preview_header, R.id.txt_theme_header, R.id.txt_styling_header, R.id.txt_system_header};
        for (int hid : headerIds) {
            TextView h = findViewById(hid);
            if (h != null) h.setTextColor(Color.WHITE);
        }

        // AMOLED Cards
        int[] cardIds = {R.id.card_widget_live_preview, R.id.card_settings_theme, R.id.card_settings_sliders, R.id.card_settings_system};
        for (int cid : cardIds) {
            MaterialCardView c = findViewById(cid);
            if (c != null) {
                c.setCardBackgroundColor(Color.parseColor("#0A0A0A"));
                c.setStrokeColor(Color.parseColor("#262626"));
            }
        }

        // Labels
        TextView txtAmoledTitle = findViewById(R.id.txt_settings_amoled_title);
        if (txtAmoledTitle != null) txtAmoledTitle.setTextColor(Color.WHITE);
        ImageView imgMoon = findViewById(R.id.img_settings_moon);
        if (imgMoon != null) imgMoon.setColorFilter(Color.WHITE);

        TextView txtTransTitle = findViewById(R.id.txt_settings_transparency_title);
        if (txtTransTitle != null) txtTransTitle.setTextColor(Color.WHITE);
        TextView txtTransSub = findViewById(R.id.txt_settings_transparency_subtitle);
        if (txtTransSub != null) txtTransSub.setTextColor(Color.parseColor("#9E9E9E"));
        ImageView imgTrans = findViewById(R.id.img_settings_transparency);
        if (imgTrans != null) imgTrans.setColorFilter(Color.WHITE);
        if (mTxtTransparencyVal != null) mTxtTransparencyVal.setTextColor(Color.WHITE);

        TextView txtMatYouTitle = findViewById(R.id.txt_settings_material_you_title);
        if (txtMatYouTitle != null) txtMatYouTitle.setTextColor(Color.WHITE);
        TextView txtMatYouSub = findViewById(R.id.txt_settings_material_you_subtitle);
        if (txtMatYouSub != null) txtMatYouSub.setTextColor(Color.parseColor("#9E9E9E"));
        ImageView imgMatYou = findViewById(R.id.img_settings_palette);
        if (imgMatYou != null) imgMatYou.setColorFilter(Color.WHITE);

        TextView labelCorner = findViewById(R.id.label_corner_radius);
        if (labelCorner != null) labelCorner.setTextColor(Color.WHITE);
        TextView labelText = findViewById(R.id.label_text_size);
        if (labelText != null) labelText.setTextColor(Color.WHITE);
        TextView labelIcon = findViewById(R.id.label_icon_size);
        if (labelIcon != null) labelIcon.setTextColor(Color.WHITE);
        TextView labelGrid = findViewById(R.id.label_grid_spacing);
        if (labelGrid != null) labelGrid.setTextColor(Color.WHITE);

        if (mTxtCornerVal != null) mTxtCornerVal.setTextColor(Color.WHITE);
        if (mTxtTextVal != null) mTxtTextVal.setTextColor(Color.WHITE);
        if (mTxtIconVal != null) mTxtIconVal.setTextColor(Color.WHITE);
        if (mTxtGridVal != null) mTxtGridVal.setTextColor(Color.WHITE);

        // AMOLED Buttons
        int[] btnIds = {R.id.btn_reset_widget_styling, R.id.btn_settings_permissions, R.id.btn_settings_copy_setup};
        for (int bid : btnIds) {
            MaterialButton btn = findViewById(bid);
            if (btn != null) {
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#141416")));
                btn.setTextColor(Color.WHITE);
                btn.setIconTint(ColorStateList.valueOf(Color.WHITE));
                btn.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2E2E32")));
                btn.setStrokeWidth(3);
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
