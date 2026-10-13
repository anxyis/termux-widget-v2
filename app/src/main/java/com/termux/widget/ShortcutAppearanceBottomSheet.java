package com.termux.widget;

import android.content.Context;
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
import com.termux.widget.icons.IconHelper;
import com.termux.widget.icons.IconPickerBottomSheet;
import com.termux.widget.icons.IconRegistry;

public class ShortcutAppearanceBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PATH = "arg_path";
    private static final String ARG_CURRENT_LABEL = "arg_current_label";
    private static final String ARG_ORIGINAL_LABEL = "arg_original_label";
    private static final String ARG_ICON_ID = "arg_icon_id";
    private static final String ARG_IS_TASK = "arg_is_task";

    public interface OnAppearanceSavedListener {
        void onAppearanceSaved(@NonNull String path, @Nullable String newLabel, @Nullable String newIconId);
    }

    private String mPath;
    private String mCurrentLabel;
    private String mOriginalLabel;
    private String mSelectedIconId;
    private boolean mIsTask;

    private OnAppearanceSavedListener mListener;

    private ImageView mPreviewIcon;
    private ImageView mPreviewCustomImage;
    private TextView mPreviewTitle;
    private TextView mPreviewPath;
    private EditText mEdtLabel;

    public static ShortcutAppearanceBottomSheet newInstance(@NonNull ShortcutFile shortcutFile) {
        ShortcutAppearanceBottomSheet sheet = new ShortcutAppearanceBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PATH, shortcutFile.getPath());
        args.putString(ARG_CURRENT_LABEL, shortcutFile.getLabel());
        args.putString(ARG_ORIGINAL_LABEL, shortcutFile.getOriginalLabel());
        args.putString(ARG_ICON_ID, shortcutFile.mIconId);
        args.putBoolean(ARG_IS_TASK, shortcutFile.isTask());
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
        mPath = args.getString(ARG_PATH, "");
        mCurrentLabel = args.getString(ARG_CURRENT_LABEL, "");
        mOriginalLabel = args.getString(ARG_ORIGINAL_LABEL, mCurrentLabel);
        mSelectedIconId = args.getString(ARG_ICON_ID, null);
        mIsTask = args.getBoolean(ARG_IS_TASK, false);

        mPreviewIcon = view.findViewById(R.id.appearance_preview_icon);
        mPreviewCustomImage = view.findViewById(R.id.appearance_preview_custom_image);
        mPreviewTitle = view.findViewById(R.id.appearance_preview_title);
        mPreviewPath = view.findViewById(R.id.appearance_preview_path);
        mEdtLabel = view.findViewById(R.id.edt_appearance_label);
        View iconContainer = view.findViewById(R.id.preview_icon_container);
        MaterialButton btnPickIcon = view.findViewById(R.id.btn_appearance_pick_icon);
        MaterialButton btnSave = view.findViewById(R.id.btn_appearance_save);
        MaterialButton btnReset = view.findViewById(R.id.btn_appearance_reset);
        MaterialButton btnCancel = view.findViewById(R.id.btn_appearance_cancel);

        mPreviewTitle.setText(mCurrentLabel);
        mPreviewPath.setText(mPath);
        mEdtLabel.setText(mCurrentLabel);
        updateIconView();

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

        iconContainer.setOnClickListener(iconClickListener);
        btnPickIcon.setOnClickListener(iconClickListener);

        btnSave.setOnClickListener(v -> {
            String entered = mEdtLabel.getText() != null ? mEdtLabel.getText().toString().trim() : "";
            String finalLabel = entered.isEmpty() ? null : entered;
            TermuxShortcutMetadataPreferences.setShortcutMetadata(context, mPath, finalLabel, mSelectedIconId);
            TermuxWidgetProvider.sendIntentToRefreshAllWidgets(context, "ShortcutAppearance");
            Toast.makeText(context, "Appearance updated for " + (finalLabel != null ? finalLabel : mOriginalLabel), Toast.LENGTH_SHORT).show();
            if (mListener != null) {
                mListener.onAppearanceSaved(mPath, finalLabel, mSelectedIconId);
            }
            dismiss();
        });

        btnReset.setOnClickListener(v -> {
            TermuxShortcutMetadataPreferences.clearShortcutMetadata(context, mPath);
            TermuxWidgetProvider.sendIntentToRefreshAllWidgets(context, "ShortcutAppearance");
            Toast.makeText(context, "Appearance reset to default", Toast.LENGTH_SHORT).show();
            if (mListener != null) {
                mListener.onAppearanceSaved(mPath, mOriginalLabel, null);
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void updateIconView() {
        if (mPreviewIcon == null) return;
        int defaultIcon = mIsTask ? R.drawable.ic_hero_bolt : R.drawable.ic_lucide_terminal;
        IconHelper.applyToImageView(mPreviewIcon, mPreviewCustomImage, mSelectedIconId, defaultIcon);
    }
}
