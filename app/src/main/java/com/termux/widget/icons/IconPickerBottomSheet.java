package com.termux.widget.icons;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.termux.widget.R;
import com.termux.widget.icons.IconRegistry.IconItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IconPickerBottomSheet extends BottomSheetDialogFragment {

    private static final int PAGE_SIZE = 24;

    public interface OnIconSelectedListener {
        void onIconSelected(@NonNull String iconId);
    }

    private OnIconSelectedListener mListener;
    private IconAdapter mAdapter;
    private String mSelectedCategory = IconRegistry.CAT_ALL;
    private String mCurrentSearch = "";

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService mExecutor;

    private final List<IconItem> mAllFilteredItems = new ArrayList<>();
    private final List<IconItem> mDisplayedItems = new ArrayList<>();
    private int mCurrentPage = 1;
    private boolean mIsLoadingMore = false;

    private ActivityResultLauncher<String> mImagePickerLauncher;

    public static IconPickerBottomSheet newInstance() {
        return new IconPickerBottomSheet();
    }

    public void setOnIconSelectedListener(OnIconSelectedListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExecutor = Executors.newSingleThreadExecutor();

        mImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleCustomImageSelected(uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_icon_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnPickCustomImage = view.findViewById(R.id.btn_pick_custom_image);
        if (btnPickCustomImage != null) {
            btnPickCustomImage.setOnClickListener(v -> {
                if (mImagePickerLauncher != null) {
                    mImagePickerLauncher.launch("image/*");
                }
            });
        }

        RecyclerView recyclerView = view.findViewById(R.id.recycler_icons);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new IconAdapter(mDisplayedItems, iconItem -> {
            if (mListener != null) {
                mListener.onIconSelected(iconItem.id);
            }
            dismiss();
        });
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy > 0 && !mIsLoadingMore) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItem) >= (totalItemCount - 12)) {
                        loadNextPage();
                    }
                }
            }
        });

        EditText searchEdit = view.findViewById(R.id.edt_search_icons);
        searchEdit.addTextChangedListener(new TextWatcher() {
            private Runnable mFilterDebounce;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCurrentSearch = s != null ? s.toString() : "";
                if (mFilterDebounce != null) {
                    mMainHandler.removeCallbacks(mFilterDebounce);
                }
                mFilterDebounce = () -> filterIconsAsync();
                mMainHandler.postDelayed(mFilterDebounce, 200);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ChipGroup chipGroup = view.findViewById(R.id.chip_group_categories);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_cat_selfhosted) {
                mSelectedCategory = IconRegistry.CAT_SELFHOSTED;
            } else if (checkedId == R.id.chip_cat_study) {
                mSelectedCategory = IconRegistry.CAT_STUDY;
            } else if (checkedId == R.id.chip_cat_dev) {
                mSelectedCategory = IconRegistry.CAT_DEV;
            } else if (checkedId == R.id.chip_cat_servers) {
                mSelectedCategory = IconRegistry.CAT_SERVERS;
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
            filterIconsAsync();
        });

        // Initial async load
        filterIconsAsync();
    }

    private void handleCustomImageSelected(@NonNull Uri uri) {
        Context context = getContext();
        if (context == null) return;

        Toast.makeText(context, "Processing custom icon...", Toast.LENGTH_SHORT).show();

        if (mExecutor != null && !mExecutor.isShutdown()) {
            mExecutor.execute(() -> {
                String customIconToken = IconHelper.saveCustomImageUri(context, uri);
                mMainHandler.post(() -> {
                    if (isAdded()) {
                        if (customIconToken != null) {
                            if (mListener != null) {
                                mListener.onIconSelected(customIconToken);
                            }
                            dismiss();
                        } else {
                            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            });
        }
    }

    private void filterIconsAsync() {
        if (mExecutor == null || mExecutor.isShutdown()) return;

        final String query = mCurrentSearch;
        final String category = mSelectedCategory;

        mExecutor.execute(() -> {
            List<IconItem> results = IconRegistry.searchIcons(query, category);
            mMainHandler.post(() -> {
                if (!isAdded()) return;
                mAllFilteredItems.clear();
                mAllFilteredItems.addAll(results);
                mCurrentPage = 1;

                mDisplayedItems.clear();
                int end = Math.min(PAGE_SIZE, mAllFilteredItems.size());
                mDisplayedItems.addAll(mAllFilteredItems.subList(0, end));

                mAdapter.notifyDataSetChanged();
            });
        });
    }

    private void loadNextPage() {
        if (mDisplayedItems.size() >= mAllFilteredItems.size()) {
            return;
        }

        mIsLoadingMore = true;
        int startIndex = mDisplayedItems.size();
        int endIndex = Math.min(startIndex + PAGE_SIZE, mAllFilteredItems.size());

        if (startIndex < endIndex) {
            List<IconItem> nextBatch = mAllFilteredItems.subList(startIndex, endIndex);
            mDisplayedItems.addAll(nextBatch);
            mAdapter.notifyItemRangeInserted(startIndex, nextBatch.size());
            mCurrentPage++;
        }
        mIsLoadingMore = false;
    }

    @Override
    public void onDestroyView() {
        mMainHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (mExecutor != null) {
            mExecutor.shutdown();
        }
        super.onDestroy();
    }

    private static class IconAdapter extends RecyclerView.Adapter<IconViewHolder> {
        private final List<IconItem> mItems;
        private final OnIconClickListener mClickListener;

        interface OnIconClickListener {
            void onIconClick(IconItem item);
        }

        IconAdapter(List<IconItem> items, OnIconClickListener listener) {
            mItems = items;
            mClickListener = listener;
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
            holder.txtName.setText(item.name);
            holder.txtPack.setText(item.pack);
            IconLoader.loadIconAsync(holder.imgIcon, item.id, item.drawableResId, 72);
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
