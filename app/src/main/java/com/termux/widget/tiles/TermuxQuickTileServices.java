package com.termux.widget.tiles;

import android.os.Build;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public final class TermuxQuickTileServices {

    public static class TermuxQuickTileService1 extends BaseTermuxTileService {
        @Override protected int getSlotIndex() { return 1; }
    }

    public static class TermuxQuickTileService2 extends BaseTermuxTileService {
        @Override protected int getSlotIndex() { return 2; }
    }

    public static class TermuxQuickTileService3 extends BaseTermuxTileService {
        @Override protected int getSlotIndex() { return 3; }
    }

    public static class TermuxQuickTileService4 extends BaseTermuxTileService {
        @Override protected int getSlotIndex() { return 4; }
    }

    public static class TermuxQuickTileService5 extends BaseTermuxTileService {
        @Override protected int getSlotIndex() { return 5; }
    }

    public static class TermuxQuickTileService6 extends BaseTermuxTileService {
        @Override protected int getSlotIndex() { return 6; }
    }

    public static class TermuxQuickTileService7 extends BaseTermuxTileService {
        @Override protected int getSlotIndex() { return 7; }
    }

    public static class TermuxQuickTileService8 extends BaseTermuxTileService {
        @Override protected int getSlotIndex() { return 8; }
    }
}
