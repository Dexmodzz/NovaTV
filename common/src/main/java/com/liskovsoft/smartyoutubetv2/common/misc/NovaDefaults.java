package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import com.liskovsoft.sharedutils.mylogger.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * NovaTV: the NovaTV default configuration (layout, sections, player, NovaTV screen...).<br/>
 * Applied once, on a fresh install only, before any setting is read: the bundled data files
 * ({@code assets/novatv_defaults/app_prefs}) become the starting settings.<br/>
 * An existing install (update) is never touched: its saved settings stay as they are.<br/>
 * No account, history or personal data is bundled.
 */
public final class NovaDefaults {
    private static final String TAG = NovaDefaults.class.getSimpleName();
    private static final String ASSETS_DIR = "novatv_defaults/app_prefs";
    private static final String APP_PREFS_DIR = "app_prefs";
    private static final String NOVATV_PREFS = "novatv_prefs";
    private static final String KEY_APPLIED = "defaults_applied";
    private static final String GLOBAL_PREFS = "com.liskovsoft.sharedutils.prefs.GlobalPreferences";

    private NovaDefaults() {
    }

    public static void applyIfFreshInstall(Context context) {
        try {
            Context app = context.getApplicationContext();
            SharedPreferences nova = app.getSharedPreferences(NOVATV_PREFS, Context.MODE_PRIVATE);

            if (nova.getBoolean(KEY_APPLIED, false)) {
                return;
            }

            File appPrefsDir = new File(app.getFilesDir(), APP_PREFS_DIR);
            boolean freshInstall = !appPrefsDir.exists() && nova.getAll().isEmpty();

            if (freshInstall) {
                copyAppPrefs(app, appPrefsDir);

                nova.edit()
                        .putInt("accent", 0)
                        .putBoolean("focus_animations", true)
                        .putInt("player_title_position", 0)
                        .putInt("ui_sound_type", 1)
                        .apply();

                SharedPreferences global = app.getSharedPreferences(GLOBAL_PREFS, Context.MODE_PRIVATE);
                if (!global.contains("preferred_dns_type")) {
                    global.edit().putInt("preferred_dns_type", 2).apply();
                }
            }

            nova.edit().putBoolean(KEY_APPLIED, true).apply();
        } catch (Exception e) {
            Log.e(TAG, "Defaults not applied: %s", e.getMessage());
        }
    }

    private static void copyAppPrefs(Context context, File targetDir) throws IOException {
        AssetManager assets = context.getAssets();
        String[] names = assets.list(ASSETS_DIR);

        if (names == null || names.length == 0) {
            return;
        }

        if (!targetDir.exists() && !targetDir.mkdirs()) {
            return;
        }

        byte[] buffer = new byte[8192];

        for (String name : names) {
            File target = new File(targetDir, name);

            if (target.exists()) {
                continue;
            }

            try (InputStream in = assets.open(ASSETS_DIR + "/" + name);
                 OutputStream out = new FileOutputStream(target)) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        }
    }
}
