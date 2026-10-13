package com.termux.widget.icons;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.termux.widget.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * High-performance, low-overhead icon renderer and memory cache.
 * Offloads vector XML parsing and disk I/O completely off the UI thread to guarantee 0 ANRs.
 */
public final class IconLoader {

    private static final int CACHE_SIZE = 256;
    private static final LruCache<String, Bitmap> BITMAP_CACHE = new LruCache<>(CACHE_SIZE);

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, new ThreadFactory() {
        private int count = 0;
        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(() -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                r.run();
            }, "IconLoader-" + (++count));
            t.setDaemon(true);
            return t;
        }
    });

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private IconLoader() {}

    /**
     * Loads icon asynchronously into an ImageView with bitmap memory caching.
     */
    public static void loadIconAsync(@NonNull ImageView imageView,
                                    @Nullable String iconId,
                                    @DrawableRes int defaultRes) {
        loadIconAsync(imageView, iconId, defaultRes, 72);
    }

    public static void loadIconAsync(@NonNull ImageView imageView,
                                    @Nullable String iconId,
                                    @DrawableRes int defaultRes,
                                    int targetSizePx) {
        Context context = imageView.getContext().getApplicationContext();
        String key = (iconId != null && !iconId.isEmpty()) ? iconId : ("res:" + defaultRes);
        boolean isCustom = IconHelper.isCustomFile(iconId);

        // Instant cache hit: 0 CPU overhead, 0 XML parsing, 0 allocations
        Bitmap cached = BITMAP_CACHE.get(key);
        if (cached != null && !cached.isRecycled()) {
            imageView.setTag(R.id.img_icon, key);
            if (isCustom) {
                imageView.setImageTintList(null);
            } else {
                ColorStateList tint = ContextCompat.getColorStateList(imageView.getContext(), R.color.m3_on_surface);
                imageView.setImageTintList(tint);
            }
            imageView.setImageBitmap(cached);
            return;
        }

        // Cache miss: clear image to avoid recycled cell flash and submit background task
        imageView.setTag(R.id.img_icon, key);
        imageView.setImageDrawable(null);

        EXECUTOR.execute(() -> {
            Bitmap rendered = null;
            if (isCustom) {
                rendered = IconHelper.loadCustomBitmap(iconId);
            } else {
                int resId = IconRegistry.getDrawableResId(iconId, defaultRes);
                rendered = renderVectorToBitmap(context, resId, targetSizePx);
            }

            if (rendered != null) {
                BITMAP_CACHE.put(key, rendered);
                final Bitmap finalBmp = rendered;
                MAIN_HANDLER.post(() -> {
                    // Only apply if the view hasn't been recycled for another item
                    if (key.equals(imageView.getTag(R.id.img_icon))) {
                        if (isCustom) {
                            imageView.setImageTintList(null);
                        } else {
                            ColorStateList tint = ContextCompat.getColorStateList(imageView.getContext(), R.color.m3_on_surface);
                            imageView.setImageTintList(tint);
                        }
                        imageView.setImageBitmap(finalBmp);
                    }
                });
            }
        });
    }

    @Nullable
    private static Bitmap renderVectorToBitmap(@NonNull Context context, @DrawableRes int resId, int size) {
        try {
            Drawable drawable = AppCompatResources.getDrawable(context, resId);
            if (drawable == null) return null;

            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, size, size);
            drawable.draw(canvas);
            return bitmap;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void prewarmCache(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                java.util.List<IconRegistry.IconItem> all = IconRegistry.getAllIcons();
                int limit = Math.min(24, all.size());
                for (int i = 0; i < limit; i++) {
                    IconRegistry.IconItem item = all.get(i);
                    String key = item.id;
                    if (BITMAP_CACHE.get(key) == null) {
                        Bitmap bmp = renderVectorToBitmap(appContext, item.drawableResId, 72);
                        if (bmp != null) {
                            BITMAP_CACHE.put(key, bmp);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        });
    }

    public static void clearCache() {
        BITMAP_CACHE.evictAll();
    }
}
