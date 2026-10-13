package com.termux.widget.icons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class IconHelper {

    public static final String PREFIX_CUSTOM_FILE = "custom_file:";
    private static final int MAX_ICON_DIMENSION = 256;

    // Cache up to 50 custom bitmap instances in memory
    private static final LruCache<String, Bitmap> BITMAP_CACHE = new LruCache<>(50);

    private IconHelper() {}

    public static boolean isCustomFile(@Nullable String iconId) {
        return iconId != null && iconId.startsWith(PREFIX_CUSTOM_FILE);
    }

    @Nullable
    public static String getCustomFilePath(@Nullable String iconId) {
        if (isCustomFile(iconId)) {
            return iconId.substring(PREFIX_CUSTOM_FILE.length());
        }
        return null;
    }

    @Nullable
    public static Bitmap loadCustomBitmap(@Nullable String iconId) {
        String path = getCustomFilePath(iconId);
        if (path == null) return null;

        Bitmap cached = BITMAP_CACHE.get(path);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }

        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            return null;
        }

        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);

            int width = opts.outWidth;
            int height = opts.outHeight;
            if (width <= 0 || height <= 0) return null;

            int sampleSize = 1;
            while (width / sampleSize > MAX_ICON_DIMENSION || height / sampleSize > MAX_ICON_DIMENSION) {
                sampleSize *= 2;
            }

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sampleSize;
            Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
            if (bitmap != null) {
                BITMAP_CACHE.put(path, bitmap);
            }
            return bitmap;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Imports an image picked from gallery/files, compresses/scales it to 256x256 max PNG,
     * persists it in app internal storage, and returns the custom_file: path token.
     */
    @Nullable
    public static String saveCustomImageUri(@NonNull Context context, @NonNull Uri uri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;

            Bitmap original = BitmapFactory.decodeStream(is);
            is.close();
            if (original == null) return null;

            int width = original.getWidth();
            int height = original.getHeight();
            Bitmap scaled = original;
            if (width > MAX_ICON_DIMENSION || height > MAX_ICON_DIMENSION) {
                float ratio = Math.min((float) MAX_ICON_DIMENSION / width, (float) MAX_ICON_DIMENSION / height);
                int targetW = Math.max(1, Math.round(width * ratio));
                int targetH = Math.max(1, Math.round(height * ratio));
                scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true);
            }

            File dir = new File(context.getFilesDir(), "custom_icons");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = "icon_" + System.currentTimeMillis() + ".png";
            File dest = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(dest);
            scaled.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            String fullPath = dest.getAbsolutePath();
            BITMAP_CACHE.put(fullPath, scaled);
            return PREFIX_CUSTOM_FILE + fullPath;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Applies icon to standard UI ImageView.
     */
    public static void applyToImageView(@NonNull ImageView iconView,
                                        @Nullable ImageView customImageView,
                                        @Nullable String iconId,
                                        @DrawableRes int defaultRes) {
        if (isCustomFile(iconId)) {
            Bitmap bmp = loadCustomBitmap(iconId);
            if (bmp != null) {
                if (customImageView != null) {
                    iconView.setVisibility(View.GONE);
                    customImageView.setVisibility(View.VISIBLE);
                    customImageView.setImageBitmap(bmp);
                } else {
                    iconView.setVisibility(View.VISIBLE);
                    iconView.setImageTintList(null);
                    iconView.setImageBitmap(bmp);
                }
                return;
            }
        }

        if (customImageView != null) {
            customImageView.setVisibility(View.GONE);
        }
        iconView.setVisibility(View.VISIBLE);
        int resId = IconRegistry.getDrawableResId(iconId, defaultRes);
        iconView.setImageResource(resId);
    }

    /**
     * Applies icon to RemoteViews for Widgets.
     */
    public static void applyToRemoteViews(@NonNull RemoteViews views,
                                          int iconViewId,
                                          int customImageViewId,
                                          @Nullable String iconId,
                                          @DrawableRes int defaultRes) {
        if (isCustomFile(iconId)) {
            Bitmap bmp = loadCustomBitmap(iconId);
            if (bmp != null) {
                if (customImageViewId != 0) {
                    views.setViewVisibility(iconViewId, View.GONE);
                    views.setViewVisibility(customImageViewId, View.VISIBLE);
                    views.setImageViewBitmap(customImageViewId, bmp);
                } else {
                    views.setImageViewBitmap(iconViewId, bmp);
                }
                return;
            }
        }

        if (customImageViewId != 0) {
            views.setViewVisibility(customImageViewId, View.GONE);
        }
        views.setViewVisibility(iconViewId, View.VISIBLE);
        int res = IconRegistry.getDrawableResId(iconId, defaultRes);
        views.setImageViewResource(iconViewId, res);
    }
}
