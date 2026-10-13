package com.termux.widget.icons;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.termux.widget.R;
import com.termux.widget.icons.IconRegistry.IconItem;

import java.util.ArrayList;
import java.util.List;

public class IconPickerBottomSheet extends BottomSheetDialogFragment {

    public interface OnIconSelectedListener {
        void onIconSelected(@NonNull String iconId);
    }

    private OnIconSelectedListener mListener;
    private IconAdapter mAdapter;
    private String mSelectedCategory = IconRegistry.CAT_ALL;
    private String mCurrentSearch = "";

    public static IconPickerBottomSheet newInstance() {
        return new IconPickerBottomSheet();
    }

    public void setOnIconSelectedListener(OnIconSelectedListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_icon_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_icons);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        mAdapter = new IconAdapter(IconRegistry.getAllIcons(), iconItem -> {
            if (mListener != null) {
                mListener.onIconSelected(iconItem.id);
            }
            dismiss();
        });
        recyclerView.setAdapter(mAdapter);

        EditText searchEdit = view.findViewById(R.id.edt_search_icons);
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCurrentSearch = s != null ? s.toString() : "";
                filterIcons();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ChipGroup chipGroup = view.findViewById(R.id.chip_group_categories);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_cat_dev) {
                mSelectedCategory = IconRegistry.CAT_DEV;
            } else if (checkedId == R.id.chip_cat_servers) {
                mSelectedCategory = IconRegistry.CAT_SERVERS;
            } else if (checkedId == R.id.chip_cat_selfhosted) {
                mSelectedCategory = IconRegistry.CAT_SELFHOSTED;
            } else if (checkedId == R.id.chip_cat_power) {
                mSelectedCategory = IconRegistry.CAT_POWER;
            } else if (checkedId == R.id.chip_cat_system) {
                mSelectedCategory = IconRegistry.CAT_SYSTEM;
            } else if (checkedId == R.id.chip_cat_files) {
                mSelectedCategory = IconRegistry.CAT_FILES;
            } else if (checkedId == R.id.chip_cat_hardware) {
                mSelectedCategory = IconRegistry.CAT_HARDWARE;
            } else if (checkedId == R.id.chip_cat_media) {
                mSelectedCategory = IconRegistry.CAT_MEDIA;
            } else if (checkedId == R.id.chip_cat_security) {
                mSelectedCategory = IconRegistry.CAT_SECURITY;
            } else if (checkedId == R.id.chip_cat_tools) {
                mSelectedCategory = IconRegistry.CAT_TOOLS;
            } else {
                mSelectedCategory = IconRegistry.CAT_ALL;
            }
            filterIcons();
        });
    }

    private void filterIcons() {
        List<IconItem> filtered = IconRegistry.searchIcons(mCurrentSearch, mSelectedCategory);
        mAdapter.updateList(filtered);
    }

    private static class IconAdapter extends RecyclerView.Adapter<IconViewHolder> {
        private final List<IconItem> mItems = new ArrayList<>();
        private final OnIconClickListener mClickListener;

        interface OnIconClickListener {
            void onIconClick(IconItem item);
        }

        IconAdapter(List<IconItem> items, OnIconClickListener listener) {
            mItems.addAll(items);
            mClickListener = listener;
        }

        void updateList(List<IconItem> newItems) {
            mItems.clear();
            mItems.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon_picker_cell, parent, false);
            return new IconViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
            IconItem item = mItems.get(position);
            holder.imgIcon.setImageResource(item.drawableResId);
            holder.txtName.setText(item.name);
            holder.txtPack.setText(item.pack);
            holder.card.setOnClickListener(v -> mClickListener.onIconClick(item));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    private static class IconViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView imgIcon;
        final TextView txtName;
        final TextView txtPack;

        IconViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_icon_cell);
            imgIcon = itemView.findViewById(R.id.img_icon);
            txtName = itemView.findViewById(R.id.txt_icon_name);
            txtPack = itemView.findViewById(R.id.txt_icon_pack);
        }
    }
}
