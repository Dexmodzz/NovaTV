package com.liskovsoft.smartyoutubetv2.common.misc;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.prefs.NovaTVData;

/**
 * NovaTV: soft navigation sounds, in place of the system click (muted in the app themes with
 * {@code android:soundEffectsEnabled=false}).<br/>
 * A short wooden "tock" when the remote moves the selection, a lower one on OK.
 * Volume (or off) from the NovaTV settings. Silent when the system sound effects are turned off in the TV settings.
 */
public final class NovaUiSounds {
    private static final long MIN_INTERVAL_MS = 55; // holding a direction key repeats fast
    /** Volumes per level (NovaTVData.UI_SOUNDS_*): off, quiet, normal */
    private static final float[] FOCUS_VOLUMES = {0f, 0.035f, 0.08f};
    private static final float[] SELECT_VOLUMES = {0f, 0.045f, 0.1f};
    private static SoundPool sPool;
    private static int sFocusId;
    private static int sSelectId;
    private static long sLastPlayMs;

    private NovaUiSounds() {
    }

    /**
     * Call for every key event the activity dispatches (before handling it).
     */
    public static void onKey(Activity activity, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN || !isEnabled(activity)) {
            return;
        }

        int level = NovaTVData.instance(activity).getUiSounds();
        if (level == NovaTVData.UI_SOUNDS_OFF) {
            return;
        }

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                View decor = activity.getWindow() != null ? activity.getWindow().peekDecorView() : null;
                if (decor == null) {
                    return;
                }
                init(activity);
                View before = activity.getCurrentFocus();
                // The focus moves after the key is dispatched: only a real move makes a sound
                decor.post(() -> {
                    View after = activity.getCurrentFocus();
                    if (after != null && after != before) {
                        play(sFocusId, FOCUS_VOLUMES[level]);
                    }
                });
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                View focus = activity.getCurrentFocus();
                if (event.getRepeatCount() == 0 && focus != null && focus.isClickable()) {
                    init(activity);
                    play(sSelectId, SELECT_VOLUMES[level]);
                }
                break;
        }
    }

    /**
     * Load the sounds in advance (the first one would be missed otherwise).
     */
    public static void preload(Context context) {
        init(context);
    }

    /**
     * Settings: let the user hear the chosen level.
     */
    public static void preview(Context context, int level) {
        if (level < 0 || level >= FOCUS_VOLUMES.length || level == NovaTVData.UI_SOUNDS_OFF) {
            return;
        }

        init(context);
        sLastPlayMs = 0;
        play(sFocusId, FOCUS_VOLUMES[level]);
    }

    private static boolean isEnabled(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0;
        } catch (SecurityException e) {
            return true;
        }
    }

    private static void init(Context context) {
        if (sPool != null) {
            return;
        }

        sPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        Context app = context.getApplicationContext();
        sFocusId = sPool.load(app, R.raw.novatv_focus, 1);
        sSelectId = sPool.load(app, R.raw.novatv_select, 1);
    }

    private static void play(int soundId, float volume) {
        long now = SystemClock.uptimeMillis();
        if (sPool == null || now - sLastPlayMs < MIN_INTERVAL_MS) {
            return;
        }

        sLastPlayMs = now;
        sPool.play(soundId, volume, volume, 1, 0, 1f);
    }
}
