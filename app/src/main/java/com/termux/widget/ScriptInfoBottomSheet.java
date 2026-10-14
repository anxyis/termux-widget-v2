package com.termux.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.termux.widget.icons.IconHelper;
import com.termux.widget.theme.ThemeManager;
import com.termux.widget.tiles.TermuxTilePreferences;
import com.termux.widget.utils.BouncyAnimationHelper;

public class ScriptInfoBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PATH = "arg_path";
    private static final String ARG_LABEL = "arg_label";
    private static final String ARG_UNEXPANDED_PATH = "arg_unexpanded_path";
    private static final String ARG_IS_TASK = "arg_is_task";
    private static final String ARG_IS_CUSTOM = "arg_is_custom";
    private static final String ARG_ICON_ID = "arg_icon_id";
    private static final String ARG_CUSTOM_COMMAND = "arg_custom_command";
    private static final String ARG_GROUP = "arg_group";

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
        args.putString(ARG_GROUP, shortcutFile.getGroupName());
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
        boolean isAmoled = ThemeManager.isAmoledMode(context);

        String path = args.getString(ARG_PATH, "");
        String label = args.getString(ARG_LABEL, "");
        String unexpandedPath = args.getString(ARG_UNEXPANDED_PATH, "");
        boolean isTask = args.getBoolean(ARG_IS_TASK, false);
        boolean isCustom = args.getBoolean(ARG_IS_CUSTOM, false);
        String iconId = args.getString(ARG_ICON_ID, null);
        String customCommand = args.getString(ARG_CUSTOM_COMMAND, "");
        String initialGroup = args.getString(ARG_GROUP, "General");

        String savedGroup = TermuxShortcutMetadataPreferences.getCustomGroup(context, path);
        final String[] currentGroup = new String[]{savedGroup != null && !savedGroup.isEmpty() ? savedGroup : initialGroup};

        TextView txtTitle = view.findViewById(R.id.sheet_title);
        TextView txtPath = view.findViewById(R.id.sheet_path);
        TextView txtBadge = view.findViewById(R.id.sheet_badge);
        TextView txtGroupBadge = view.findViewById(R.id.sheet_group_badge);
        ImageView imgIcon = view.findViewById(R.id.sheet_icon);
        View iconFrame = view.findViewById(R.id.sheet_icon_frame);
        MaterialButton btnRun = view.findViewById(R.id.sheet_btn_run);
        MaterialCardView secondaryCard = view.findViewById(R.id.sheet_secondary_card);
        TextView txtGroupSub = view.findViewById(R.id.sheet_txt_group_sub);

        txtTitle.setText(label);
        txtPath.setText(unexpandedPath);
        if (txtGroupBadge != null) {
            txtGroupBadge.setText(currentGroup[0].toUpperCase());
        }
        if (txtGroupSub != null) {
            txtGroupSub.setText("Current: " + currentGroup[0]);
        }

        int defaultIcon = isTask ? R.drawable.ic_hero_bolt : R.drawable.ic_lucide_terminal;
        IconHelper.applyToImageView(imgIcon, null, iconId, defaultIcon);

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

        // AMOLED Theme Application
        if (isAmoled) {
            View root = view.findViewById(R.id.sheet_root);
            if (root != null) root.setBackgroundResource(R.drawable.widget_sheet_amoled_bg);
            View dragHandle = view.findViewById(R.id.sheet_drag_handle);
            if (dragHandle != null) dragHandle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#444444")));

            if (iconFrame != null) iconFrame.setBackgroundResource(R.drawable.widget_icon_badge_amoled_bg);
            imgIcon.setColorFilter(Color.WHITE);
            txtTitle.setTextColor(Color.WHITE);
            txtPath.setTextColor(Color.parseColor("#9E9E9E"));

            txtBadge.setBackgroundResource(R.drawable.widget_pill_badge_amoled_bg);
            txtBadge.setTextColor(Color.WHITE);
            if (txtGroupBadge != null) {
                txtGroupBadge.setBackgroundResource(R.drawable.widget_pill_badge_amoled_bg);
                txtGroupBadge.setTextColor(Color.WHITE);
            }

            btnRun.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnRun.setTextColor(Color.BLACK);
            btnRun.setIconTint(ColorStateList.valueOf(Color.BLACK));

            if (secondaryCard != null) {
                secondaryCard.setCardBackgroundColor(Color.parseColor("#0A0A0A"));
                secondaryCard.setStrokeColor(Color.parseColor("#262626"));
            }

            // Dividers
            int[] dividerIds = new int[]{R.id.sheet_divider_1, R.id.sheet_divider_2, R.id.sheet_divider_3, R.id.sheet_divider_4};
            for (int divId : dividerIds) {
                View div = view.findViewById(divId);
                if (div != null) div.setBackgroundColor(Color.parseColor("#262626"));
            }

            // Icons & Labels
            ImageView iconApp = view.findViewById(R.id.sheet_icon_appearance);
            if (iconApp != null) iconApp.setColorFilter(Color.WHITE);
            TextView titleApp = view.findViewById(R.id.sheet_txt_appearance_title);
            if (titleApp != null) titleApp.setTextColor(Color.WHITE);
            TextView subApp = view.findViewById(R.id.sheet_txt_appearance_sub);
            if (subApp != null) subApp.setTextColor(Color.parseColor("#9E9E9E"));

            ImageView iconGrp = view.findViewById(R.id.sheet_icon_group);
            if (iconGrp != null) iconGrp.setColorFilter(Color.WHITE);
            TextView titleGrp = view.findViewById(R.id.sheet_txt_group_title);
            if (titleGrp != null) titleGrp.setTextColor(Color.WHITE);
            if (txtGroupSub != null) txtGroupSub.setTextColor(Color.parseColor("#9E9E9E"));

            ImageView iconPin = view.findViewById(R.id.sheet_icon_pin);
            if (iconPin != null) iconPin.setColorFilter(Color.WHITE);
            TextView txtPin = view.findViewById(R.id.sheet_txt_pin);
            if (txtPin != null) txtPin.setTextColor(Color.WHITE);

            ImageView iconTile = view.findViewById(R.id.sheet_icon_tile);
            if (iconTile != null) iconTile.setColorFilter(Color.WHITE);
            TextView txtTile = view.findViewById(R.id.sheet_txt_tile);
            if (txtTile != null) txtTile.setTextColor(Color.WHITE);

            ImageView iconCopy = view.findViewById(R.id.sheet_icon_copy);
            if (iconCopy != null) iconCopy.setColorFilter(Color.WHITE);
            TextView txtCopy = view.findViewById(R.id.sheet_txt_copy_label);
            if (txtCopy != null) txtCopy.setTextColor(Color.WHITE);

            ImageView iconEdit = view.findViewById(R.id.sheet_icon_edit);
            if (iconEdit != null) iconEdit.setColorFilter(Color.WHITE);
            TextView txtEdit = view.findViewById(R.id.sheet_txt_edit);
            if (txtEdit != null) txtEdit.setTextColor(Color.WHITE);
        }

        // Bouncy touch feedback
        BouncyAnimationHelper.applySpringPress(btnRun, 0.95f);
        btnRun.setOnClickListener(v -> {
            TermuxWidgetProvider.sendExecutionIntentToTermuxService(context, path, "ScriptInfoBottomSheet");
            Toast.makeText(context, "Executing: " + label, Toast.LENGTH_SHORT).show();
            dismiss();
        });

        // Appearance customization (Cosmetic in-app & widgets)
        View btnAppearance = view.findViewById(R.id.sheet_action_appearance);
        BouncyAnimationHelper.applySpringPress(btnAppearance, 0.97f);
        View.OnClickListener openAppearanceCustomizer = v -> {
            ShortcutFile sf = new ShortcutFile(path, label);
            sf.mIconId = iconId;
            sf.mIsTask = isTask;
            sf.mIsCustomCommand = isCustom;
            sf.mCustomCommand = customCommand;
            sf.mGroup = currentGroup[0];

            ShortcutAppearanceBottomSheet appearanceSheet = ShortcutAppearanceBottomSheet.newInstance(sf);
            appearanceSheet.setOnAppearanceSavedListener((p, newLabel, newIconId) -> {
                if (newLabel != null && !newLabel.isEmpty()) {
                    txtTitle.setText(newLabel);
                } else {
                    txtTitle.setText(label);
                }
                int defIcon = isTask ? R.drawable.ic_hero_bolt : R.drawable.ic_lucide_terminal;
                IconHelper.applyToImageView(imgIcon, null, newIconId, defIcon);
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

        // Change Category / Group Action
        View btnGroup = view.findViewById(R.id.sheet_action_group);
        if (btnGroup != null) {
            BouncyAnimationHelper.applySpringPress(btnGroup, 0.97f);
            btnGroup.setOnClickListener(v -> {
                showChangeGroupDialog(context, path, currentGroup[0], newGroup -> {
                    currentGroup[0] = newGroup;
                    TermuxShortcutMetadataPreferences.setCustomGroup(context, path, newGroup);
                    if (txtGroupBadge != null) txtGroupBadge.setText(newGroup.toUpperCase());
                    if (txtGroupSub != null) txtGroupSub.setText("Current: " + newGroup);
                    Toast.makeText(context, "Category set to: " + newGroup, Toast.LENGTH_SHORT).show();
                    if (mListener != null) {
                        mListener.onAppearanceChanged();
                    }
                });
            });
        }

        // Add to Home Screen (Widget)
        View btnPin = view.findViewById(R.id.sheet_action_pin);
        if (btnPin != null) {
            BouncyAnimationHelper.applySpringPress(btnPin, 0.97f);
            btnPin.setOnClickListener(v -> {
                TermuxButtonWidgetUtils.pinButtonWidget(context, path, label, iconId);
                dismiss();
            });
        }

        // Assign to Quick Tile
        View btnTile = view.findViewById(R.id.sheet_action_tile);
        if (btnTile != null) {
            BouncyAnimationHelper.applySpringPress(btnTile, 0.97f);
            btnTile.setOnClickListener(v -> {
                showAssignToTileDialog(context, path, label, iconId);
            });
        }

        // Copy Path / Command
        View btnCopy = view.findViewById(R.id.sheet_action_copy);
        TextView txtCopyLabel = view.findViewById(R.id.sheet_txt_copy_label);
        if (isCustom && txtCopyLabel != null) {
            txtCopyLabel.setText("Copy Bash Command");
        }
        if (btnCopy != null) {
            BouncyAnimationHelper.applySpringPress(btnCopy, 0.97f);
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
        }

        // Custom command edit & delete
        View btnEdit = view.findViewById(R.id.sheet_action_edit);
        View btnDelete = view.findViewById(R.id.sheet_action_delete);
        if (isCustom) {
            if (btnEdit != null) {
                btnEdit.setVisibility(View.VISIBLE);
                BouncyAnimationHelper.applySpringPress(btnEdit, 0.97f);
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
            }

            if (btnDelete != null) {
                btnDelete.setVisibility(View.VISIBLE);
                BouncyAnimationHelper.applySpringPress(btnDelete, 0.97f);
                ShortcutFile sf = new ShortcutFile(path, label);
                sf.mIsCustomCommand = true;
                sf.mCustomCommand = customCommand;
                sf.mIconId = iconId;
                sf.mIsTask = isTask;
                btnDelete.setOnClickListener(v -> {
                    dismiss();
                    if (mListener != null) {
                        mListener.onDeleteCommand(sf);
                    }
                });
            }
        }
    }

    private interface OnGroupSelectedCallback {
        void onGroupSelected(@NonNull String newGroup);
    }

    private void showChangeGroupDialog(Context context, String path, String currentGroup, OnGroupSelectedCallback callback) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        EditText input = new EditText(context);
        input.setHint("Category / Group Name (e.g. Work, SSH, Media)");
        input.setText(currentGroup);
        layout.addView(input);

        new MaterialAlertDialogBuilder(context)
                .setTitle("Change Category / Group")
                .setMessage("Group this shortcut inside the widget app for quick sorting and filtering.")
                .setView(layout)
                .setPositiveButton("Save", (d, which) -> {
                    String entered = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!entered.isEmpty()) {
                        callback.onGroupSelected(entered);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
