package com.liskovsoft.smartyoutubetv2.common.app.presenters.settings;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData.ColorScheme;
import com.liskovsoft.smartyoutubetv2.common.prefs.NovaTVData;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * NovaTV: settings of the reskin (theme, accent, cards per row, preview, clock, animations).<br/>
 * Wherever an upstream setting already exists, this screen writes that same setting
 * (color scheme, video grid scale, card preview, clock, 12/24h), so the two screens never disagree.
 */
public class NovaTVSettingsPresenter extends BasePresenter<Void> {
    /** Indexes in MainUIData color schemes (see MainUIData.initColorSchemes) */
    private static final int SCHEME_DARK = 1; // dark grey
    private static final int SCHEME_OLED = 3; // dark grey OLED
    private static final int[] OLED_SCHEMES = {3, 4, 5, 7};
    private static final int[] CARDS_PER_ROW = {3, 4, 5, 6};
    private static final int[] PREVIEW_DELAYS_MS = {500, 1_000, 2_000, 3_000};

    private final MainUIData mMainUIData;
    private final GeneralData mGeneralData;
    private final NovaTVData mNovaTVData;
    private boolean mRestartApp;
    private final Runnable mOnFinish = () -> {
        if (mRestartApp) {
            mRestartApp = false;
            MessageHelpers.showLongMessage(getContext(), R.string.msg_restart_app);
            Utils.restartTheApp(getContext());
        }
    };

    private NovaTVSettingsPresenter(Context context) {
        super(context);
        mMainUIData = MainUIData.instance(context);
        mGeneralData = GeneralData.instance(context);
        mNovaTVData = NovaTVData.instance(context);
    }

    public static NovaTVSettingsPresenter instance(Context context) {
        return new NovaTVSettingsPresenter(context);
    }

    public void show() {
        AppDialogPresenter settingsPresenter = AppDialogPresenter.instance(getContext());

        appendTheme(settingsPresenter);
        appendAccent(settingsPresenter);
        if (Build.VERSION.SDK_INT > 19) { // the grid scale is fixed on older devices (MainUIData.getVideoGridScale)
            appendCardsPerRow(settingsPresenter);
        }
        appendPreview(settingsPresenter);
        appendPreviewDelay(settingsPresenter);
        appendPlayerTitlePosition(settingsPresenter);
        appendClock(settingsPresenter);
        appendAnimations(settingsPresenter);

        settingsPresenter.showDialog(getContext().getString(R.string.novatv_settings), mOnFinish);
    }

    private void appendTheme(AppDialogPresenter settingsPresenter) {
        List<ColorScheme> schemes = mMainUIData.getColorSchemes();
        boolean isOled = contains(OLED_SCHEMES, schemes.indexOf(mMainUIData.getColorScheme()));

        List<OptionItem> options = new ArrayList<>();
        options.add(UiOptionItem.from(getContext().getString(R.string.novatv_theme_dark), option -> {
            mMainUIData.setColorScheme(schemes.get(SCHEME_DARK));
            mRestartApp = true;
        }, !isOled));
        options.add(UiOptionItem.from(getContext().getString(R.string.novatv_theme_oled), option -> {
            mMainUIData.setColorScheme(schemes.get(SCHEME_OLED));
            mRestartApp = true;
        }, isOled));

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.novatv_theme), options);
    }

    private void appendAccent(AppDialogPresenter settingsPresenter) {
        int[][] accents = {
                {R.string.novatv_accent_orange, NovaTVData.ACCENT_ORANGE},
                {R.string.novatv_accent_blue, NovaTVData.ACCENT_BLUE},
                {R.string.novatv_accent_violet, NovaTVData.ACCENT_VIOLET},
                {R.string.novatv_accent_green, NovaTVData.ACCENT_GREEN}};

        List<OptionItem> options = new ArrayList<>();
        for (int[] accent : accents) {
            options.add(UiOptionItem.from(getContext().getString(accent[0]), option -> {
                mNovaTVData.setAccent(accent[1]);
                mRestartApp = true;
            }, mNovaTVData.getAccent() == accent[1]));
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.novatv_accent), options);
    }

    /**
     * Writes the upstream "video grid scale": the scale that fits exactly N cards in a row.
     */
    private void appendCardsPerRow(AppDialogPresenter settingsPresenter) {
        int current = getCardsPerRow(mMainUIData.getVideoGridScale());

        List<OptionItem> options = new ArrayList<>();
        for (int cards : CARDS_PER_ROW) {
            options.add(UiOptionItem.from(String.valueOf(cards), option -> {
                float scale = getGridScaleFor(cards);
                if (scale > 0) {
                    mMainUIData.setVideoGridScale(scale);
                    mRestartApp = true;
                }
            }, current == cards));
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.novatv_cards_per_row), options);
    }

    private void appendPreview(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        for (int[] pair : new int[][] {
                {R.string.novatv_preview_off, MainUIData.CARD_PREVIEW_DISABLED},
                {R.string.card_preview_muted, MainUIData.CARD_PREVIEW_MUTED},
                {R.string.card_preview_full, MainUIData.CARD_PREVIEW_FULL}}) {
            options.add(UiOptionItem.from(getContext().getString(pair[0]),
                    option -> mMainUIData.setCardPreviewType(pair[1]),
                    mMainUIData.getCardPreviewType() == pair[1]));
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.novatv_preview), options);
    }

    private void appendPreviewDelay(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        for (int delayMs : PREVIEW_DELAYS_MS) {
            String seconds = NumberFormat.getNumberInstance().format(delayMs / 1_000f); // device locale: "0.5" or "0,5"
            options.add(UiOptionItem.from(getContext().getString(R.string.novatv_seconds, seconds),
                    option -> mNovaTVData.setPreviewDelayMs(delayMs),
                    mNovaTVData.getPreviewDelayMs() == delayMs));
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.novatv_preview_delay), options);
    }

    /**
     * Title + channel (avatar, name, subscribers) in the player: top header or above the info line.
     * Applied the next time the player shows the video info, no restart needed.
     */
    private void appendPlayerTitlePosition(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        for (int[] pair : new int[][] {
                {R.string.novatv_player_title_top, NovaTVData.PLAYER_TITLE_TOP},
                {R.string.novatv_player_title_above_info, NovaTVData.PLAYER_TITLE_ABOVE_INFO}}) {
            options.add(UiOptionItem.from(getContext().getString(pair[0]),
                    option -> mNovaTVData.setPlayerTitlePosition(pair[1]),
                    mNovaTVData.getPlayerTitlePosition() == pair[1]));
        }

        settingsPresenter.appendRadioCategory(getContext().getString(R.string.novatv_player_title_position), options);
    }

    private void appendClock(AppDialogPresenter settingsPresenter) {
        List<OptionItem> options = new ArrayList<>();

        options.add(UiOptionItem.from(getContext().getString(R.string.novatv_clock_show), option -> {
            mGeneralData.setGlobalClockEnabled(option.isSelected());
            mRestartApp = true;
        }, mGeneralData.isGlobalClockEnabled()));
        options.add(UiOptionItem.from(getContext().getString(R.string.time_format_24), option -> {
            mGeneralData.set24HourLocaleEnabled(option.isSelected());
            mRestartApp = true;
        }, mGeneralData.is24HourLocaleEnabled()));

        settingsPresenter.appendCheckedCategory(getContext().getString(R.string.novatv_clock), options);
    }

    private void appendAnimations(AppDialogPresenter settingsPresenter) {
        settingsPresenter.appendSingleSwitch(UiOptionItem.from(getContext().getString(R.string.novatv_focus_animations), option -> {
            mNovaTVData.setFocusAnimationsEnabled(option.isSelected());
            mRestartApp = true;
        }, mNovaTVData.isFocusAnimationsEnabled()));
    }

    // Same formula as GridFragmentHelper.getMaxColsNumFloatInt() (app module)

    private float getColsNum(float gridScale) {
        Resources res = getContext().getResources();
        int cardWidthId = res.getIdentifier("card_width", "dimen", getContext().getPackageName());
        int spacingId = res.getIdentifier("grid_item_horizontal_spacing", "dimen", getContext().getPackageName());

        if (cardWidthId == 0 || spacingId == 0) {
            return -1;
        }

        DisplayMetrics metrics = res.getDisplayMetrics();
        int displayWidthPx = Math.max(metrics.widthPixels, metrics.heightPixels);
        float uiScale = mMainUIData.getUIScale();
        float usableWidth = displayWidthPx - displayWidthPx * 0.1f * uiScale;

        return usableWidth / (res.getDimensionPixelSize(cardWidthId) * gridScale + res.getDimensionPixelSize(spacingId));
    }

    private int getCardsPerRow(float gridScale) {
        float cols = getColsNum(gridScale);
        return cols > 0 ? (int) cols : -1;
    }

    private float getGridScaleFor(int cards) {
        Resources res = getContext().getResources();
        int cardWidthId = res.getIdentifier("card_width", "dimen", getContext().getPackageName());
        int spacingId = res.getIdentifier("grid_item_horizontal_spacing", "dimen", getContext().getPackageName());

        if (cardWidthId == 0 || spacingId == 0) {
            return -1;
        }

        // cols = usable / (cardWidth * scale + spacing)  ->  aim at N + 0.3 so (int) cols == N
        float usableOverCols = getColsNum(1.0f) * (res.getDimensionPixelSize(cardWidthId) + res.getDimensionPixelSize(spacingId)) / (cards + 0.3f);
        float scale = (usableOverCols - res.getDimensionPixelSize(spacingId)) / res.getDimensionPixelSize(cardWidthId);

        return Math.round(scale * 100) / 100f;
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values) {
            if (v == value) {
                return true;
            }
        }

        return false;
    }
}
