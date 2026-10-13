package com.termux.widget.icons;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.widget.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registry of curated high-quality vector icons from
 * Lucide, Heroicons, Phosphor Icons, and Ionicons.
 */
public final class IconRegistry {

    public static final String CAT_ALL = "All";
    public static final String CAT_DEV = "Developer";
    public static final String CAT_SERVERS = "Servers & Cloud";
    public static final String CAT_POWER = "Actions & Power";
    public static final String CAT_SYSTEM = "System & Network";

    public static class IconItem {
        public final String id;
        public final String name;
        @DrawableRes public final int drawableResId;
        public final String category;
        public final String pack;

        public IconItem(String id, String name, @DrawableRes int drawableResId, String category, String pack) {
            this.id = id;
            this.name = name;
            this.drawableResId = drawableResId;
            this.category = category;
            this.pack = pack;
        }
    }

    private static final Map<String, IconItem> ICONS = new LinkedHashMap<>();

    static {
        // Developer & Terminal (Lucide / Ionicons)
        register(new IconItem("lucide_terminal", "Terminal", R.drawable.ic_lucide_terminal, CAT_DEV, "Lucide"));
        register(new IconItem("lucide_code", "Code / Script", R.drawable.ic_lucide_code, CAT_DEV, "Lucide"));
        register(new IconItem("lucide_git", "Git Branch", R.drawable.ic_lucide_git_branch, CAT_DEV, "Lucide"));
        register(new IconItem("lucide_cpu", "CPU / Processor", R.drawable.ic_lucide_cpu, CAT_DEV, "Lucide"));
        register(new IconItem("ion_cube", "Container / Pod", R.drawable.ic_ion_cube, CAT_DEV, "Ionicons"));

        // Servers & Cloud (Heroicons / Phosphor / Lucide)
        register(new IconItem("hero_server", "Server Host", R.drawable.ic_hero_server, CAT_SERVERS, "Heroicons"));
        register(new IconItem("lucide_database", "Database", R.drawable.ic_lucide_database, CAT_SERVERS, "Lucide"));
        register(new IconItem("phosphor_cloud", "Cloud Service", R.drawable.ic_phosphor_cloud, CAT_SERVERS, "Phosphor"));

        // Actions & Power (Heroicons / Phosphor / Lucide / Ionicons)
        register(new IconItem("lucide_play", "Run Script", R.drawable.ic_lucide_play, CAT_POWER, "Lucide"));
        register(new IconItem("lucide_power", "Power / Daemon", R.drawable.ic_lucide_power, CAT_POWER, "Lucide"));
        register(new IconItem("hero_bolt", "Instant Task", R.drawable.ic_hero_bolt, CAT_POWER, "Heroicons"));
        register(new IconItem("hero_arrow_path", "Reload / Loop", R.drawable.ic_hero_arrow_path, CAT_POWER, "Heroicons"));
        register(new IconItem("ion_rocket", "Deploy / Launch", R.drawable.ic_ion_rocket, CAT_POWER, "Ionicons"));

        // System & Network (Phosphor / Lucide)
        register(new IconItem("phosphor_shield", "Security Shield", R.drawable.ic_phosphor_shield, CAT_SYSTEM, "Phosphor"));
        register(new IconItem("phosphor_wifi", "Wi-Fi / Network", R.drawable.ic_phosphor_wifi, CAT_SYSTEM, "Phosphor"));
        register(new IconItem("lucide_folder", "Directory", R.drawable.ic_lucide_folder, CAT_SYSTEM, "Lucide"));
        register(new IconItem("lucide_settings", "Configuration", R.drawable.ic_lucide_settings, CAT_SYSTEM, "Lucide"));
    }

    private static void register(IconItem item) {
        ICONS.put(item.id, item);
    }

    @NonNull
    public static List<IconItem> getAllIcons() {
        return new ArrayList<>(ICONS.values());
    }

    @DrawableRes
    public static int getDrawableResId(@Nullable String iconId, @DrawableRes int defaultRes) {
        if (iconId == null) return defaultRes;
        IconItem item = ICONS.get(iconId);
        return item != null ? item.drawableResId : defaultRes;
    }

    @Nullable
    public static IconItem getIcon(@Nullable String iconId) {
        if (iconId == null) return null;
        return ICONS.get(iconId);
    }

    @NonNull
    public static List<IconItem> searchIcons(@Nullable String query, @Nullable String category) {
        List<IconItem> result = new ArrayList<>();
        String normalizedQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        boolean filterCategory = category != null && !CAT_ALL.equalsIgnoreCase(category);

        for (IconItem item : ICONS.values()) {
            if (filterCategory && !item.category.equalsIgnoreCase(category)) {
                continue;
            }
            if (normalizedQuery.isEmpty() ||
                item.name.toLowerCase(Locale.ROOT).contains(normalizedQuery) ||
                item.id.toLowerCase(Locale.ROOT).contains(normalizedQuery) ||
                item.pack.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                result.add(item);
            }
        }
        return result;
    }
}
