package com.termux.widget.tiles;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.termux.widget.R;
import com.termux.widget.TermuxWidgetProvider;
import com.termux.widget.activities.TermuxTileConfigActivity;
import com.termux.widget.icons.IconHelper;
import com.termux.widget.icons.IconRegistry;

@RequiresApi(api = Build.VERSION_CODES.N)
public abstract class BaseTermuxTileService extends TileService {

    protected abstract int getSlotIndex();

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    public void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        int slot = getSlotIndex();
        String scriptPath = TermuxTilePreferences.getScriptPath(this, slot);
        String label = TermuxTilePreferences.getLabel(this, slot);
        String iconId = TermuxTilePreferences.getIconId(this, slot);

        if (scriptPath != null && !scriptPath.isEmpty()) {
            tile.setLabel(label != null && !label.isEmpty() ? label : ("Tile " + slot));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle(getString(R.string.quick_tile_ready));
            }
            if (IconHelper.isCustomFile(iconId)) {
                android.graphics.Bitmap bmp = IconHelper.loadCustomBitmap(iconId);
                if (bmp != null) {
                    tile.setIcon(Icon.createWithBitmap(bmp));
                } else {
                    tile.setIcon(Icon.createWithResource(this, R.drawable.ic_lucide_terminal));
                }
            } else {
                int iconRes = IconRegistry.getDrawableResId(iconId, R.drawable.ic_lucide_terminal);
                tile.setIcon(Icon.createWithResource(this, iconRes));
            }
            tile.setState(Tile.STATE_INACTIVE);
        } else {
            tile.setLabel("Termux Tile " + slot);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle(getString(R.string.quick_tile_tap_to_configure));
            }
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_lucide_terminal));
            tile.setState(Tile.STATE_INACTIVE);
        }

        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        int slot = getSlotIndex();
        String scriptPath = TermuxTilePreferences.getScriptPath(this, slot);

        if (scriptPath != null && !scriptPath.isEmpty()) {
            String label = TermuxTilePreferences.getLabel(this, slot);
            Toast.makeText(this, "Executing: " + (label != null ? label : scriptPath), Toast.LENGTH_SHORT).show();
            TermuxWidgetProvider.sendExecutionIntentToTermuxService(this, scriptPath, "QuickTile" + slot);
        } else {
            openTileConfiguration();
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private void openTileConfiguration() {
        Intent intent = new Intent(this, TermuxTileConfigActivity.class);
        intent.putExtra(TermuxTileConfigActivity.EXTRA_SLOT_INDEX, getSlotIndex());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT >= 34) {
            PendingIntent pi = PendingIntent.getActivity(this, getSlotIndex(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            startActivityAndCollapse(pi);
        } else {
            startActivityAndCollapse(intent);
        }
    }
}
