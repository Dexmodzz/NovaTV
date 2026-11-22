package com.liskovsoft.smartyoutubetv2.common.prefs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * NovaTV: settings added by the reskin.<br/>
 * Kept in their own preferences file, so the upstream data format (MainUIData, GeneralData...)
 * is never touched and stays compatible with upstream updates.
 */
public final class NovaTVData {
    private static final String PREFS_NAME = "novatv_prefs";
    private static final String KEY_ACCENT = "accent";
    private static final String KEY_PREVIEW_DELAY_MS = "preview_delay_ms";
    private static final String KEY_FOCUS_ANIMATIONS = "focus_animations";
    private static final String KEY_PLAYER_TITLE_POSITION = "player_title_position";
    private static final String KEY_UI_SOUNDS = "ui_sounds";
    private static final String KEY_UI_SOUND_TYPE = "ui_sound_type";

    public static final int ACCENT_ORANGE = 0;
    public static final int ACCENT_BLUE = 1;
    public static final int ACCENT_VIOLET = 2;
    public static final int ACCENT_GREEN = 3;
    /** Theme overlays defined in the app module (smarttubetv/res/values/novatv_themes.xml) */
    private static final String[] ACCENT_STYLES = {"NovaAccent.Orange", "NovaAccent.Blue", "NovaAccent.Violet", "NovaAccent.Green"};

    public static final int PREVIEW_DELAY_DEFAULT_MS = 2_000;

    /** Player: title + channel (avatar, name, subscribers) in the top header */
    public static final int PLAYER_TITLE_TOP = 0;
    /** Player: title + channel right above the views/date/likes line, near the seek bar */
    public static final int PLAYER_TITLE_ABOVE_INFO = 1;

    /** Navigation sounds (NovaUiSounds) */
    public static final int UI_SOUNDS_OFF = 0;
    public static final int UI_SOUNDS_QUIET = 1;
    public static final int UI_SOUNDS_NORMAL = 2;

    private static NovaTVData sInstance;
    private final SharedPreferences mPrefs;

    private NovaTVData(Context context) {
        mPrefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static NovaTVData instance(Context context) {
        if (sInstance == null) {
            sInstance = new NovaTVData(context);
        }

        return sInstance;
    }

    public int getAccent() {
        int accent = mPrefs.getInt(KEY_ACCENT, ACCENT_ORANGE);
        return accent >= 0 && accent < ACCENT_STYLES.length ? accent : ACCENT_ORANGE;
    }

    public void setAccent(int accent) {
        mPrefs.edit().putInt(KEY_ACCENT, accent).apply();
    }

    public int getPreviewDelayMs() {
        return mPrefs.getInt(KEY_PREVIEW_DELAY_MS, PREVIEW_DELAY_DEFAULT_MS);
    }

    public void setPreviewDelayMs(int delayMs) {
        mPrefs.edit().putInt(KEY_PREVIEW_DELAY_MS, delayMs).apply();
    }

    public boolean isFocusAnimationsEnabled() {
        return mPrefs.getBoolean(KEY_FOCUS_ANIMATIONS, true);
    }

    public void setFocusAnimationsEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_FOCUS_ANIMATIONS, enabled).apply();
    }

    public int getPlayerTitlePosition() {
        int position = mPrefs.getInt(KEY_PLAYER_TITLE_POSITION, PLAYER_TITLE_TOP);
        return position == PLAYER_TITLE_ABOVE_INFO ? PLAYER_TITLE_ABOVE_INFO : PLAYER_TITLE_TOP;
    }

    public void setPlayerTitlePosition(int position) {
        mPrefs.edit().putInt(KEY_PLAYER_TITLE_POSITION, position).apply();
    }

    public int getUiSounds() {
        int level = mPrefs.getInt(KEY_UI_SOUNDS, UI_SOUNDS_NORMAL);
        return level >= UI_SOUNDS_OFF && level <= UI_SOUNDS_NORMAL ? level : UI_SOUNDS_NORMAL;
    }

    public void setUiSounds(int level) {
        mPrefs.edit().putInt(KEY_UI_SOUNDS, level).apply();
    }

    /** Index of the navigation sound (0 = wooden tock) */
    public int getUiSoundType() {
        return Math.max(0, mPrefs.getInt(KEY_UI_SOUND_TYPE, 0));
    }

    public void setUiSoundType(int type) {
        mPrefs.edit().putInt(KEY_UI_SOUND_TYPE, type).apply();
    }

    /**
     * Applies the accent color overlay on top of the activity theme. Call right after setTheme().
     */
    public void applyAccent(Activity activity) {
        int styleId = activity.getResources().getIdentifier(ACCENT_STYLES[getAccent()], "style", activity.getPackageName());

        if (styleId != 0) {
            activity.getTheme().applyStyle(styleId, true);
        }
    }
}
