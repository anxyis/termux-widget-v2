package com.termux.widget.activities;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.termux.shared.shell.ShellUtils;
import com.termux.widget.R;
import com.termux.widget.ShortcutFile;
import com.termux.widget.TermuxButtonWidgetPreferences;
import com.termux.widget.TermuxButtonWidgetProvider;
import com.termux.widget.icons.IconHelper;
import com.termux.widget.icons.IconPickerBottomSheet;
import com.termux.widget.icons.IconRegistry;
import com.termux.widget.utils.ShortcutUtils;

import java.util.ArrayList;
import java.util.List;

public class TermuxButtonWidgetConfigureActivity extends AppCompatActivity {

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private String mSelectedScriptPath = null;
    private String mSelectedIconId = "lucide_terminal";
    private String mCustomLabel = "";

    private TextView mTxtSelectedScript;
    private ImageView mImgChosenIcon;
    private TextView mTxtChosenIconName;
    private EditText mEdtButtonLabel;

    // Preview elements
    private ImageView mPreviewIcon;
    private TextView mPreviewLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.termux.widget.theme.ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        setContentView(R.layout.activity_button_widget_configure);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_button_config);
        toolbar.setNavigationOnClickListener(v -> finish());

        mTxtSelectedScript = findViewById(R.id.txt_selected_script);
        mImgChosenIcon = findViewById(R.id.img_chosen_icon);
        mTxtChosenIconName = findViewById(R.id.txt_chosen_icon_name);
        mEdtButtonLabel = findViewById(R.id.edt_button_label);

        mPreviewIcon = findViewById(R.id.widget_button_icon);
        mPreviewLabel = findViewById(R.id.widget_button_label);

        // Pre-fill existing configuration if re-configuring
        String existingPath = TermuxButtonWidgetPreferences.getScriptPath(this, mAppWidgetId);
        if (existingPath != null) {
            mSelectedScriptPath = existingPath;
            mTxtSelectedScript.setText(existingPath);
            mCustomLabel = TermuxButtonWidgetPreferences.getLabel(this, mAppWidgetId);
            mEdtButtonLabel.setText(mCustomLabel);
            String existingIcon = TermuxButtonWidgetPreferences.getIconId(this, mAppWidgetId);
            if (existingIcon != null) {
                mSelectedIconId = existingIcon;
            }
        }

        updatePreview();

        findViewById(R.id.card_select_script).setOnClickListener(v -> showScriptSelectionDialog());
        findViewById(R.id.card_select_icon).setOnClickListener(v -> showIconPickerDialog());

        mEdtButtonLabel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCustomLabel = s != null ? s.toString().trim() : "";
                updatePreview();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btn_save_widget).setOnClickListener(v -> saveAndFinish());
    }

    private void updatePreview() {
        String label = !mCustomLabel.isEmpty() ? mCustomLabel :
                (mSelectedScriptPath != null ? ShellUtils.getExecutableBasename(mSelectedScriptPath) : "Shortcut Button");
        if (mPreviewLabel != null) mPreviewLabel.setText(label);

        int defaultIcon = R.drawable.ic_lucide_terminal;
        if (mPreviewIcon != null) IconHelper.applyToImageView(mPreviewIcon, null, mSelectedIconId, defaultIcon);
        if (mImgChosenIcon != null) IconHelper.applyToImageView(mImgChosenIcon, null, mSelectedIconId, defaultIcon);

        if (IconHelper.isCustomFile(mSelectedIconId)) {
            if (mTxtChosenIconName != null) mTxtChosenIconName.setText("Custom Image");
        } else {
            IconRegistry.IconItem item = IconRegistry.getIcon(mSelectedIconId);
            if (item != null && mTxtChosenIconName != null) {
                mTxtChosenIconName.setText(item.name + " (" + item.pack + ")");
            }
        }
    }

    private void showScriptSelectionDialog() {
        List<ShortcutFile> files = new ArrayList<>();
        ShortcutUtils.enumerateShortcutFiles(this, files, true);

        if (files.isEmpty()) {
            Toast.makeText(this, R.string.msg_no_shortcut_scripts, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            names[i] = files.get(i).getLabel() + (files.get(i).isTask() ? " [TASK]" : "");
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.quick_tile_select_shortcut)
                .setItems(names, (dialog, which) -> {
                    ShortcutFile chosen = files.get(which);
                    mSelectedScriptPath = chosen.getPath();
                    mTxtSelectedScript.setText(chosen.getLabel());
                    if (mCustomLabel.isEmpty()) {
                        mCustomLabel = chosen.getLabel();
                        mEdtButtonLabel.setText(mCustomLabel);
                    }
                    updatePreview();
                })
                .show();
    }

    private void showIconPickerDialog() {
        IconPickerBottomSheet sheet = IconPickerBottomSheet.newInstance();
        sheet.setOnIconSelectedListener(iconId -> {
            mSelectedIconId = iconId;
            updatePreview();
        });
        sheet.show(getSupportFragmentManager(), "icon_picker");
    }

    private void saveAndFinish() {
        if (mSelectedScriptPath == null) {
            Toast.makeText(this, "Please choose a script first", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalLabel = !mCustomLabel.isEmpty() ? mCustomLabel : ShellUtils.getExecutableBasename(mSelectedScriptPath);
        TermuxButtonWidgetPreferences.saveButtonWidgetConfig(
                this, mAppWidgetId, mSelectedScriptPath, finalLabel, mSelectedIconId);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        TermuxButtonWidgetProvider.updateButtonWidget(this, appWidgetManager, mAppWidgetId);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
