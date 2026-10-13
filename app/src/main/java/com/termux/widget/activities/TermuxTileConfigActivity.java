package com.termux.widget.activities;

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
import com.termux.shared.shell.ShellUtils;
import com.termux.widget.R;
import com.termux.widget.ShortcutFile;
import com.termux.widget.TermuxWidgetProvider;
import com.termux.widget.icons.IconPickerBottomSheet;
import com.termux.widget.icons.IconRegistry;
import com.termux.widget.tiles.TermuxTilePreferences;
import com.termux.widget.utils.ShortcutUtils;

import java.util.ArrayList;
import java.util.List;

public class TermuxTileConfigActivity extends AppCompatActivity {

    public static final String EXTRA_SLOT_INDEX = "tile_slot_index";

    private int mSlotIndex = 1;
    private String mSelectedScriptPath = null;
    private String mSelectedIconId = "lucide_terminal";
    private String mCustomLabel = "";

    private TextView mTxtSelectedScript;
    private ImageView mImgChosenIcon;
    private TextView mTxtChosenIconName;
    private EditText mEdtTileLabel;

    // Preview
    private ImageView mPreviewTileIcon;
    private TextView mPreviewTileLabel;
    private TextView mPreviewTileSublabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tile_config);

        mSlotIndex = getIntent().getIntExtra(EXTRA_SLOT_INDEX, 1);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_tile_config);
        toolbar.setTitle(getString(R.string.quick_tile_config_title, mSlotIndex));
        toolbar.setNavigationOnClickListener(v -> finish());

        mTxtSelectedScript = findViewById(R.id.txt_tile_selected_script);
        mImgChosenIcon = findViewById(R.id.img_tile_chosen_icon);
        mTxtChosenIconName = findViewById(R.id.txt_tile_chosen_icon_name);
        mEdtTileLabel = findViewById(R.id.edt_tile_label);

        mPreviewTileIcon = findViewById(R.id.preview_tile_icon);
        mPreviewTileLabel = findViewById(R.id.preview_tile_label);
        mPreviewTileSublabel = findViewById(R.id.preview_tile_sublabel);

        // Load existing
        String existingScript = TermuxTilePreferences.getScriptPath(this, mSlotIndex);
        if (existingScript != null) {
            mSelectedScriptPath = existingScript;
            mTxtSelectedScript.setText(existingScript);
            mCustomLabel = TermuxTilePreferences.getLabel(this, mSlotIndex);
            mEdtTileLabel.setText(mCustomLabel);
            String existingIcon = TermuxTilePreferences.getIconId(this, mSlotIndex);
            if (existingIcon != null) {
                mSelectedIconId = existingIcon;
            }
        }

        updatePreview();

        findViewById(R.id.card_tile_select_script).setOnClickListener(v -> showScriptSelectionDialog());
        findViewById(R.id.card_tile_select_icon).setOnClickListener(v -> showIconPickerDialog());

        mEdtTileLabel.addTextChangedListener(new TextWatcher() {
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

        findViewById(R.id.btn_save_tile).setOnClickListener(v -> saveAndFinish());
        findViewById(R.id.btn_test_run_tile).setOnClickListener(v -> testRun());
        findViewById(R.id.btn_clear_tile).setOnClickListener(v -> clearAndFinish());
    }

    private void updatePreview() {
        String label = !mCustomLabel.isEmpty() ? mCustomLabel :
                (mSelectedScriptPath != null ? ShellUtils.getExecutableBasename(mSelectedScriptPath) : ("Tile " + mSlotIndex));
        mPreviewTileLabel.setText(label);

        int iconRes = IconRegistry.getDrawableResId(mSelectedIconId, R.drawable.ic_lucide_terminal);
        mPreviewTileIcon.setImageResource(iconRes);
        mImgChosenIcon.setImageResource(iconRes);

        IconRegistry.IconItem item = IconRegistry.getIcon(mSelectedIconId);
        if (item != null) {
            mTxtChosenIconName.setText(item.name + " (" + item.pack + ")");
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
                        mEdtTileLabel.setText(mCustomLabel);
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

    private void testRun() {
        if (mSelectedScriptPath == null) {
            Toast.makeText(this, "Please choose a script first", Toast.LENGTH_SHORT).show();
            return;
        }
        TermuxWidgetProvider.sendExecutionIntentToTermuxService(this, mSelectedScriptPath, "TileTest" + mSlotIndex);
        Toast.makeText(this, "Executed: " + mSelectedScriptPath, Toast.LENGTH_SHORT).show();
    }

    private void saveAndFinish() {
        if (mSelectedScriptPath == null) {
            Toast.makeText(this, "Please choose a script first", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalLabel = !mCustomLabel.isEmpty() ? mCustomLabel : ShellUtils.getExecutableBasename(mSelectedScriptPath);
        TermuxTilePreferences.saveTile(this, mSlotIndex, mSelectedScriptPath, finalLabel, mSelectedIconId);
        Toast.makeText(this, "Tile " + mSlotIndex + " updated!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void clearAndFinish() {
        TermuxTilePreferences.clearTile(this, mSlotIndex);
        Toast.makeText(this, "Tile " + mSlotIndex + " cleared", Toast.LENGTH_SHORT).show();
        finish();
    }
}
