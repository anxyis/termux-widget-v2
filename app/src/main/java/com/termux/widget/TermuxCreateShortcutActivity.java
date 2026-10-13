package com.termux.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.shared.activity.media.AppCompatActivityUtils;
import com.termux.shared.logger.Logger;
import com.termux.widget.utils.ShortcutUtils;

import java.util.ArrayList;
import java.util.List;

public class TermuxCreateShortcutActivity extends AppCompatActivity {

    private ListView mListView;
    private List<ShortcutFile> mFiles = new ArrayList<>();

    private static final String LOG_TAG = "TermuxCreateShortcutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_termux_create_shortcut);
        mListView = findViewById(R.id.list);

        AppCompatActivityUtils.setToolbar(this, com.termux.shared.R.id.toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!ShortcutUtils.isTermuxAppAccessible(this, LOG_TAG, true)) {
            finish();
            return;
        }

        mFiles.clear();
        ShortcutUtils.enumerateShortcutFiles(this, mFiles, true);

        if (mFiles.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.msg_no_shortcut_scripts)
                    .setOnDismissListener(dialog -> finish()).show();
            return;
        }

        final String[] values = new String[mFiles.size()];
        for (int i = 0; i < mFiles.size(); i++) {
            ShortcutFile sf = mFiles.get(i);
            values[i] = "[" + sf.getGroupName() + "] " + sf.getLabel();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, values);
        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener((parent, view, position, id) -> {
            ShortcutFile sf = mFiles.get(position);
            createShortcut(TermuxCreateShortcutActivity.this, sf);
            finish();
        });
    }

    private void createShortcut(Context context, ShortcutFile shortcutFile) {
        boolean isPinnedShortcutSupported = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = ShortcutUtils.getShortcutManager(context, LOG_TAG, true);
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported())
                isPinnedShortcutSupported = true;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPinnedShortcutSupported)
                createPinnedShortcut(context, shortcutFile);
            else
                createStaticShortcut(context, shortcutFile);
        } catch (Exception e) {
            String message = context.getString(
                    isPinnedShortcutSupported ? R.string.error_create_pinned_shortcut_failed : R.string.error_create_static_shortcut_failed,
                    shortcutFile.getUnExpandedPath());
            Logger.logErrorAndShowToast(context, LOG_TAG, message + ": " + e.getMessage());
            Logger.logStackTraceWithMessage(LOG_TAG, message, e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createPinnedShortcut(Context context, ShortcutFile shortcutFile) {
        ShortcutManager shortcutManager = ShortcutUtils.getShortcutManager(context, LOG_TAG, true);
        if (shortcutManager == null) return;

        Logger.showToast(context, context.getString(R.string.msg_request_create_pinned_shortcut,
                shortcutFile.getUnExpandedPath()), true);
        shortcutManager.requestPinShortcut(shortcutFile.getShortcutInfo(context, true), null);
    }

    private void createStaticShortcut(Context context, ShortcutFile shortcutFile) {
        Logger.showToast(context, context.getString(R.string.msg_request_create_static_shortcut,
                shortcutFile.getUnExpandedPath()), true);
        setResult(RESULT_OK, shortcutFile.getStaticShortcutIntent(context));
    }

}
