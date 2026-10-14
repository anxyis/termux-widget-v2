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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.termux.widget.R;
import com.termux.widget.TermuxWidgetProvider;
import com.termux.widget.TermuxWidgetStylePreferences;
import com.termux.widget.theme.ThemeManager;
import com.termux.widget.utils.BouncyAnimationHelper;

public class TermuxWidgetSettingsActivity extends AppCompatActivity {

    private MaterialCardView mCardPreview;
    private ImageView mPreviewIcon;
    private TextView mPreviewTitle;
    private TextView mPreviewSubtitle;
    private TextView mTxtCornerVal;
    private TextView mTxtTextVal;
    private TextView mTxtIconVal;

    private Slider mSliderCorner;
    private Slider mSliderText;
    private Slider mSliderIcon;

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
        mPreviewIcon = findViewById(R.id.preview_widget_icon);
        mPreviewTitle = findViewById(R.id.preview_widget_title);
        mPreviewSubtitle = findViewById(R.id.preview_widget_subtitle);

        mTxtCornerVal = findViewById(R.id.txt_val_corner_radius);
        mTxtTextVal = findViewById(R.id.txt_val_text_size);
        mTxtIconVal = findViewById(R.id.txt_val_icon_size);

        mSliderCorner = findViewById(R.id.slider_corner_radius);
        mSliderText = findViewById(R.id.slider_text_size);
        mSliderIcon = findViewById(R.id.slider_icon_size);

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

        // Sliders Setup
        int currentCorner = TermuxWidgetStylePreferences.getCornerRadiusDp(this);
        int currentText = TermuxWidgetStylePreferences.getTextSizeSp(this);
        int currentIcon = TermuxWidgetStylePreferences.getIconSizeDp(this);

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

        // Reset to Defaults Button
        MaterialButton btnReset = findViewById(R.id.btn_reset_widget_styling);
        if (btnReset != null) {
            BouncyAnimationHelper.applySpringPress(btnReset, 0.95f);
            btnReset.setOnClickListener(v -> {
                TermuxWidgetStylePreferences.resetToDefaults(this);
                if (mSliderCorner != null) mSliderCorner.setValue(TermuxWidgetStylePreferences.DEFAULT_CORNER_RADIUS_DP);
                if (mSliderText != null) mSliderText.setValue(TermuxWidgetStylePreferences.DEFAULT_TEXT_SIZE_SP);
                if (mSliderIcon != null) mSliderIcon.setValue(TermuxWidgetStylePreferences.DEFAULT_ICON_SIZE_DP);
                updatePreviewCornerRadius(TermuxWidgetStylePreferences.DEFAULT_CORNER_RADIUS_DP);
                updatePreviewTextSize(TermuxWidgetStylePreferences.DEFAULT_TEXT_SIZE_SP);
                updatePreviewIconSize(TermuxWidgetStylePreferences.DEFAULT_ICON_SIZE_DP);
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

        applyAmoledThemeIfNeeded();
    }

    private void updatePreviewCornerRadius(int radiusDp) {
        if (mCardPreview != null) {
            mCardPreview.setRadius(dpToPx(radiusDp));
        }
    }

    private void updatePreviewTextSize(int sizeSp) {
        if (mPreviewTitle != null) {
            mPreviewTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        }
        if (mPreviewSubtitle != null) {
            mPreviewSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(9, sizeSp - 2));
        }
    }

    private void updatePreviewIconSize(int sizeDp) {
        if (mPreviewIcon != null) {
            ViewGroup.LayoutParams lp = mPreviewIcon.getLayoutParams();
            if (lp != null) {
                lp.width = dpToPx(sizeDp);
                lp.height = dpToPx(sizeDp);
                mPreviewIcon.setLayoutParams(lp);
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

        if (mPreviewTitle != null) mPreviewTitle.setTextColor(Color.WHITE);
        if (mPreviewSubtitle != null) mPreviewSubtitle.setTextColor(Color.parseColor("#9E9E9E"));
        if (mPreviewIcon != null) mPreviewIcon.setColorFilter(Color.WHITE);

        TextView labelCorner = findViewById(R.id.label_corner_radius);
        if (labelCorner != null) labelCorner.setTextColor(Color.WHITE);
        TextView labelText = findViewById(R.id.label_text_size);
        if (labelText != null) labelText.setTextColor(Color.WHITE);
        TextView labelIcon = findViewById(R.id.label_icon_size);
        if (labelIcon != null) labelIcon.setTextColor(Color.WHITE);

        if (mTxtCornerVal != null) mTxtCornerVal.setTextColor(Color.WHITE);
        if (mTxtTextVal != null) mTxtTextVal.setTextColor(Color.WHITE);
        if (mTxtIconVal != null) mTxtIconVal.setTextColor(Color.WHITE);

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
