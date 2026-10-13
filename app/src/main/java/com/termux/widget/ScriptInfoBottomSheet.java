package com.termux.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.termux.widget.icons.IconRegistry;
import com.termux.widget.tiles.TermuxTilePreferences;

public class ScriptInfoBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PATH = "arg_path";
    private static final String ARG_LABEL = "arg_label";
    private static final String ARG_UNEXPANDED_PATH = "arg_unexpanded_path";
    private static final String ARG_IS_TASK = "arg_is_task";
    private static final String ARG_IS_CUSTOM = "arg_is_custom";
    private static final String ARG_ICON_ID = "arg_icon_id";
    private static final String ARG_CUSTOM_COMMAND = "arg_custom_command";

    public interface ScriptActionListener {
        void onEditCommand(@NonNull ShortcutFile shortcutFile);
        void onDeleteCommand(@NonNull ShortcutFile shortcutFile);
        void onTileAssigned();
        void onAppearanceChanged();
    }

    private ScriptActionListener mListener;

    public static ScriptInfoBottomSheet newInstance(@NonNull ShortcutFile shortcutFile) {
        ScriptInfoBottomSheet sheet = new ScriptInfoBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PATH, shortcutFile.getPath());
        args.putString(ARG_LABEL, shortcutFile.getLabel());
        args.putString(ARG_UNEXPANDED_PATH, shortcutFile.getUnExpandedPath());
        args.putBoolean(ARG_IS_TASK, shortcutFile.isTask());
        args.putBoolean(ARG_IS_CUSTOM, shortcutFile.mIsCustomCommand);
        args.putString(ARG_ICON_ID, shortcutFile.mIconId);
        args.putString(ARG_CUSTOM_COMMAND, shortcutFile.mCustomCommand);
        sheet.setArguments(args);
        return sheet;
    }

    public void setScriptActionListener(ScriptActionListener listener) {
        mListener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_TermuxWidget_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_script_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args == null) return;

        Context context = requireContext();
        String path = args.getString(ARG_PATH, "");
        String label = args.getString(ARG_LABEL, "");
        String unexpandedPath = args.getString(ARG_UNEXPANDED_PATH, "");
        boolean isTask = args.getBoolean(ARG_IS_TASK, false);
        boolean isCustom = args.getBoolean(ARG_IS_CUSTOM, false);
        String iconId = args.getString(ARG_ICON_ID, null);
        String customCommand = args.getString(ARG_CUSTOM_COMMAND, "");

        TextView txtTitle = view.findViewById(R.id.sheet_title);
        TextView txtPath = view.findViewById(R.id.sheet_path);
        TextView txtBadge = view.findViewById(R.id.sheet_badge);
        ImageView imgIcon = view.findViewById(R.id.sheet_icon);
        MaterialButton btnRun = view.findViewById(R.id.sheet_btn_run);

        txtTitle.setText(label);
        txtPath.setText(unexpandedPath);

        int defaultIcon = isTask ? R.drawable.ic_hero_bolt : R.drawable.ic_lucide_terminal;
        int iconRes = iconId != null ? IconRegistry.getDrawableResId(iconId, defaultIcon) : defaultIcon;
        imgIcon.setImageResource(iconRes);

        if (isCustom) {
            txtBadge.setText("CUSTOM COMMAND");
            txtBadge.setBackgroundResource(R.drawable.widget_pill_badge_bg);
            txtBadge.setTextColor(context.getResources().getColor(R.color.badge_task_text));
        } else if (isTask) {
            txtBadge.setText("BACKGROUND TASK");
            txtBadge.setBackgroundResource(R.drawable.widget_pill_badge_bg);
            txtBadge.setTextColor(context.getResources().getColor(R.color.badge_task_text));
        } else {
            txtBadge.setText("TERMINAL SESSION");
            txtBadge.setBackgroundResource(R.drawable.widget_pill_session_bg);
            txtBadge.setTextColor(context.getResources().getColor(R.color.badge_session_text));
        }

        // Run button
        btnRun.setOnClickListener(v -> {
            TermuxWidgetProvider.sendExecutionIntentToTermuxService(context, path, "ScriptInfoBottomSheet");
            Toast.makeText(context, "Executing: " + label, Toast.LENGTH_SHORT).show();
            dismiss();
        });

        // Appearance customization (Cosmetic in-app & widgets)
        View btnAppearance = view.findViewById(R.id.sheet_action_appearance);
        View.OnClickListener openAppearanceCustomizer = v -> {
            ShortcutFile sf = new ShortcutFile(path, label);
            sf.mIconId = iconId;
            sf.mIsTask = isTask;
            sf.mIsCustomCommand = isCustom;
            sf.mCustomCommand = customCommand;

            ShortcutAppearanceBottomSheet appearanceSheet = ShortcutAppearanceBottomSheet.newInstance(sf);
            appearanceSheet.setOnAppearanceSavedListener((p, newLabel, newIconId) -> {
                if (newLabel != null && !newLabel.isEmpty()) {
                    txtTitle.setText(newLabel);
                } else {
                    txtTitle.setText(label);
                }
                int defIcon = isTask ? R.drawable.ic_hero_bolt : R.drawable.ic_lucide_terminal;
                imgIcon.setImageResource(newIconId != null ? IconRegistry.getDrawableResId(newIconId, defIcon) : defIcon);
                if (mListener != null) {
                    mListener.onAppearanceChanged();
                }
            });
            appearanceSheet.show(getChildFragmentManager(), "shortcut_appearance");
        };

        if (btnAppearance != null) {
            btnAppearance.setOnClickListener(openAppearanceCustomizer);
        }
        imgIcon.setOnClickListener(openAppearanceCustomizer);
        txtTitle.setOnClickListener(openAppearanceCustomizer);

        // Pin to Home Screen
        View btnPin = view.findViewById(R.id.sheet_action_pin);
        btnPin.setOnClickListener(v -> {
            pinToHomeScreen(context, path, label, iconId, isTask, isCustom, customCommand);
            dismiss();
        });

        // Assign to Quick Tile
        View btnTile = view.findViewById(R.id.sheet_action_tile);
        btnTile.setOnClickListener(v -> {
            showAssignToTileDialog(context, path, label, iconId);
        });

        // Copy Path / Command
        View btnCopy = view.findViewById(R.id.sheet_action_copy);
        TextView txtCopyLabel = view.findViewById(R.id.sheet_txt_copy_label);
        if (isCustom) {
            txtCopyLabel.setText("Copy Bash Command");
        }
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                String copyText = (isCustom && customCommand != null && !customCommand.isEmpty()) ? customCommand : path;
                ClipData clip = ClipData.newPlainText("Termux Script", copyText);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
            dismiss();
        });

        // Custom command edit & delete
        View btnEdit = view.findViewById(R.id.sheet_action_edit);
        View btnDelete = view.findViewById(R.id.sheet_action_delete);
        if (isCustom) {
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);

            ShortcutFile sf = new ShortcutFile(path, label);
            sf.mIsCustomCommand = true;
            sf.mCustomCommand = customCommand;
            sf.mIconId = iconId;
            sf.mIsTask = isTask;

            btnEdit.setOnClickListener(v -> {
                dismiss();
                if (mListener != null) {
                    mListener.onEditCommand(sf);
                }
            });

            btnDelete.setOnClickListener(v -> {
                dismiss();
                if (mListener != null) {
                    mListener.onDeleteCommand(sf);
                }
            });
        }
    }

    private void pinToHomeScreen(Context context, String path, String label, String iconId, boolean isTask, boolean isCustom, String customCommand) {
        ShortcutFile sf = new ShortcutFile(path, label);
        sf.mIconId = iconId;
        sf.mIsTask = isTask;
        sf.mIsCustomCommand = isCustom;
        sf.mCustomCommand = customCommand;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                shortcutManager.requestPinShortcut(sf.getShortcutInfo(context, false), null);
                Toast.makeText(context, "Pinned shortcut requested", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Fallback for older Android versions
        Intent staticIntent = sf.getStaticShortcutIntent(context);
        context.sendBroadcast(staticIntent);
        Toast.makeText(context, "Added shortcut to home screen", Toast.LENGTH_SHORT).show();
    }

    private void showAssignToTileDialog(Context context, String path, String label, String iconId) {
        CharSequence[] tileOptions = new CharSequence[8];
        for (int i = 1; i <= 8; i++) {
            String existingLabel = TermuxTilePreferences.getLabel(context, i);
            tileOptions[i - 1] = "Tile " + i + (existingLabel != null && !existingLabel.isEmpty() ? " (" + existingLabel + ")" : " (Empty)");
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle("Select Quick Settings Slot")
                .setItems(tileOptions, (dialog, which) -> {
                    int slot = which + 1;
                    TermuxTilePreferences.saveTile(context, slot, path, label, iconId);
                    Toast.makeText(context, "Assigned to Quick Tile " + slot, Toast.LENGTH_SHORT).show();
                    if (mListener != null) {
                        mListener.onTileAssigned();
                    }
                    dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
