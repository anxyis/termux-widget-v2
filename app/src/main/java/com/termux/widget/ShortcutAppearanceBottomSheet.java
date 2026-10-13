package com.termux.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.termux.widget.icons.IconHelper;
import com.termux.widget.icons.IconPickerBottomSheet;
import com.termux.widget.theme.ThemeManager;
import com.termux.widget.utils.BouncyAnimationHelper;

public class ShortcutAppearanceBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PATH = "arg_path";
    private static final String ARG_CURRENT_LABEL = "arg_current_label";
    private static final String ARG_ORIGINAL_LABEL = "arg_original_label";
    private static final String ARG_ICON_ID = "arg_icon_id";
    private static final String ARG_IS_TASK = "arg_is_task";
    private static final String ARG_GROUP = "arg_group";

    public interface OnAppearanceSavedListener {
        void onAppearanceSaved(@NonNull String path, @Nullable String newLabel, @Nullable String newIconId);
    }

    private String mPath;
    private String mCurrentLabel;
    private String mOriginalLabel;
    private String mSelectedIconId;
    private boolean mIsTask;
    private String mCurrentGroup;

    private OnAppearanceSavedListener mListener;

    private ImageView mPreviewIcon;
    private ImageView mPreviewCustomImage;
    private TextView mPreviewTitle;
    private TextView mPreviewPath;
    private EditText mEdtLabel;
    private EditText mEdtGroup;

    public static ShortcutAppearanceBottomSheet newInstance(@NonNull ShortcutFile shortcutFile) {
        ShortcutAppearanceBottomSheet sheet = new ShortcutAppearanceBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PATH, shortcutFile.getPath());
        args.putString(ARG_CURRENT_LABEL, shortcutFile.getLabel());
        args.putString(ARG_ORIGINAL_LABEL, shortcutFile.getOriginalLabel());
        args.putString(ARG_ICON_ID, shortcutFile.mIconId);
        args.putBoolean(ARG_IS_TASK, shortcutFile.isTask());
        args.putString(ARG_GROUP, shortcutFile.getGroupName());
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnAppearanceSavedListener(OnAppearanceSavedListener listener) {
        mListener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_TermuxWidget_BottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_shortcut_appearance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args == null) return;

        Context context = requireContext();
        boolean isAmoled = ThemeManager.isAmoledMode(context);

        mPath = args.getString(ARG_PATH, "");
        mCurrentLabel = args.getString(ARG_CURRENT_LABEL, "");
        mOriginalLabel = args.getString(ARG_ORIGINAL_LABEL, mCurrentLabel);
        mSelectedIconId = args.getString(ARG_ICON_ID, null);
        mIsTask = args.getBoolean(ARG_IS_TASK, false);

        String savedGroup = TermuxShortcutMetadataPreferences.getCustomGroup(context, mPath);
        mCurrentGroup = savedGroup != null ? savedGroup : args.getString(ARG_GROUP, "");

        mPreviewIcon = view.findViewById(R.id.appearance_preview_icon);
        mPreviewCustomImage = view.findViewById(R.id.appearance_preview_custom_image);
        mPreviewTitle = view.findViewById(R.id.appearance_preview_title);
        mPreviewPath = view.findViewById(R.id.appearance_preview_path);
        mEdtLabel = view.findViewById(R.id.edt_appearance_label);
        mEdtGroup = view.findViewById(R.id.edt_appearance_group);
        View iconContainer = view.findViewById(R.id.preview_icon_container);
        MaterialButton btnPickIcon = view.findViewById(R.id.btn_appearance_pick_icon);
        MaterialButton btnSave = view.findViewById(R.id.btn_appearance_save);
        MaterialButton btnReset = view.findViewById(R.id.btn_appearance_reset);
        MaterialButton btnCancel = view.findViewById(R.id.btn_appearance_cancel);
        MaterialCardView previewCard = view.findViewById(R.id.appearance_preview_card);

        mPreviewTitle.setText(mCurrentLabel);
        mPreviewPath.setText(mPath);
        mEdtLabel.setText(mCurrentLabel);
        if (mEdtGroup != null && mCurrentGroup != null && !mCurrentGroup.isEmpty()) {
            mEdtGroup.setText(mCurrentGroup);
        }
        updateIconView();

        // AMOLED Theme Styling
        if (isAmoled) {
            View root = view.findViewById(R.id.appearance_root);
            if (root != null) root.setBackgroundResource(R.drawable.widget_sheet_amoled_bg);
            View dragHandle = view.findViewById(R.id.appearance_drag_handle);
            if (dragHandle != null) dragHandle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#444444")));

            TextView headerTitle = view.findViewById(R.id.appearance_header_title);
            if (headerTitle != null) headerTitle.setTextColor(Color.WHITE);
            TextView headerSub = view.findViewById(R.id.appearance_header_sub);
            if (headerSub != null) headerSub.setTextColor(Color.parseColor("#9E9E9E"));

            if (previewCard != null) {
                previewCard.setCardBackgroundColor(Color.parseColor("#0A0A0A"));
                previewCard.setStrokeColor(Color.parseColor("#262626"));
            }
            if (iconContainer != null) iconContainer.setBackgroundResource(R.drawable.widget_icon_badge_amoled_bg);
            if (mPreviewIcon != null) mPreviewIcon.setColorFilter(Color.WHITE);
            if (mPreviewTitle != null) mPreviewTitle.setTextColor(Color.WHITE);
            if (mPreviewPath != null) mPreviewPath.setTextColor(Color.parseColor("#9E9E9E"));

            if (mEdtLabel != null) mEdtLabel.setTextColor(Color.WHITE);
            if (mEdtGroup != null) mEdtGroup.setTextColor(Color.WHITE);

            TextInputLayout tilLabel = view.findViewById(R.id.til_appearance_label);
            if (tilLabel != null) {
                tilLabel.setBoxStrokeColor(Color.WHITE);
                tilLabel.setHintTextColor(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                tilLabel.setHelperTextColor(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            }
            TextInputLayout tilGroup = view.findViewById(R.id.til_appearance_group);
            if (tilGroup != null) {
                tilGroup.setBoxStrokeColor(Color.WHITE);
                tilGroup.setHintTextColor(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                tilGroup.setHelperTextColor(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            }

            btnPickIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#141416")));
            btnPickIcon.setTextColor(Color.WHITE);
            btnPickIcon.setIconTint(ColorStateList.valueOf(Color.WHITE));
            btnPickIcon.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2E2E32")));
            btnPickIcon.setStrokeWidth(3);

            btnSave.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            btnSave.setTextColor(Color.BLACK);
            btnSave.setIconTint(ColorStateList.valueOf(Color.BLACK));

            btnReset.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#141416")));
            btnReset.setTextColor(Color.WHITE);
            btnReset.setIconTint(ColorStateList.valueOf(Color.WHITE));
            btnReset.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2E2E32")));
            btnReset.setStrokeWidth(3);

            btnCancel.setTextColor(Color.parseColor("#9E9E9E"));
        }

        mEdtLabel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s != null ? s.toString().trim() : "";
                mPreviewTitle.setText(!text.isEmpty() ? text : mOriginalLabel);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        View.OnClickListener iconClickListener = v -> {
            IconPickerBottomSheet picker = IconPickerBottomSheet.newInstance();
            picker.setOnIconSelectedListener(iconId -> {
                mSelectedIconId = iconId;
                updateIconView();
            });
            picker.show(getChildFragmentManager(), "appearance_icon_picker");
        };

        if (iconContainer != null) {
            BouncyAnimationHelper.applySpringPress(iconContainer, 0.90f);
            iconContainer.setOnClickListener(iconClickListener);
        }
        BouncyAnimationHelper.applySpringPress(btnPickIcon, 0.96f);
        btnPickIcon.setOnClickListener(iconClickListener);

        BouncyAnimationHelper.applySpringPress(btnSave, 0.95f);
        btnSave.setOnClickListener(v -> {
            String entered = mEdtLabel.getText() != null ? mEdtLabel.getText().toString().trim() : "";
            String finalLabel = entered.isEmpty() ? null : entered;
            String enteredGroup = mEdtGroup != null && mEdtGroup.getText() != null ? mEdtGroup.getText().toString().trim() : "";
            String finalGroup = enteredGroup.isEmpty() ? null : enteredGroup;

            TermuxShortcutMetadataPreferences.setShortcutMetadata(context, mPath, finalLabel, mSelectedIconId, finalGroup);
            TermuxWidgetProvider.sendIntentToRefreshAllWidgets(context, "ShortcutAppearance");
            Toast.makeText(context, "Appearance updated for " + (finalLabel != null ? finalLabel : mOriginalLabel), Toast.LENGTH_SHORT).show();
            if (mListener != null) {
                mListener.onAppearanceSaved(mPath, finalLabel, mSelectedIconId);
            }
            dismiss();
        });

        BouncyAnimationHelper.applySpringPress(btnReset, 0.96f);
        btnReset.setOnClickListener(v -> {
            TermuxShortcutMetadataPreferences.clearShortcutMetadata(context, mPath);
            TermuxWidgetProvider.sendIntentToRefreshAllWidgets(context, "ShortcutAppearance");
            Toast.makeText(context, "Appearance reset to default", Toast.LENGTH_SHORT).show();
            if (mListener != null) {
                mListener.onAppearanceSaved(mPath, mOriginalLabel, null);
            }
            dismiss();
        });

        BouncyAnimationHelper.applySpringPress(btnCancel, 0.96f);
        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void updateIconView() {
        if (mPreviewIcon == null) return;
        int defaultIcon = mIsTask ? R.drawable.ic_hero_bolt : R.drawable.ic_lucide_terminal;
        IconHelper.applyToImageView(mPreviewIcon, mPreviewCustomImage, mSelectedIconId, defaultIcon);
    }
}
