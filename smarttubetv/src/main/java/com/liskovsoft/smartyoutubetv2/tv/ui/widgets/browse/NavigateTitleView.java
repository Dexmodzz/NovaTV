package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.browse;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.FocusFinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.SearchOrbView;
import androidx.leanback.widget.SearchOrbView.Colors;
import androidx.leanback.widget.TitleView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.sharedutils.locale.LocaleUtility;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AccountSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.LanguageSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager.AccountChangeListener;
import com.liskovsoft.smartyoutubetv2.common.prefs.common.DataChangeBase.OnDataChange;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tooltips.TooltipCompatHandler;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.search.LongClickSearchOrbView;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.time.DateTimeView;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.List;
import java.util.Locale;

import static androidx.leanback.widget.TitleViewAdapter.BRANDING_VIEW_VISIBLE;
import static androidx.leanback.widget.TitleViewAdapter.FULL_VIEW_VISIBLE;
import static androidx.leanback.widget.TitleViewAdapter.SEARCH_VIEW_VISIBLE;

/**
 * View that supports dpad navigation between children<br/>
 * NOTE: You should set android:nextFocusLeft and android:nextFocusRight<br/>
 * https://stackoverflow.com/questions/38169378/use-multiple-orb-buttons-or-other-buttons-in-the-leanbacks-title-view<br/>
 * https://stackoverflow.com/questions/40802470/add-button-to-browsefragment
 */
public class NavigateTitleView extends TitleView implements OnDataChange, AccountChangeListener {
    private LongClickSearchOrbView mAccountView;
    private SearchOrbView mLanguageView;
    private SearchOrbView mExitPip;
    private TextView mPipTitle;
    private int mSearchVisibility = View.INVISIBLE;
    private int mBrandingVisibility = View.INVISIBLE;
    private DateTimeView mGlobalClock;
    private DateTimeView mGlobalDate;
    private SearchOrbView mSearchOrbView;
    private boolean mInitDone;
    private int mFlags = FULL_VIEW_VISIBLE;
    private int mIconWidth;
    private int mIconHeight;
    private boolean mIsSearchOrbEnabled;
    private boolean mIsAccountViewEnabled;
    private boolean mIsLanguageViewEnabled;
    private boolean mIsGlobalClockEnabled;
    // NovaTV: search pill + category chips
    private ViewGroup mTopBar;
    private View mSearchPill;
    private View mSearchField;
    private View mSearchMic;
    private HorizontalScrollView mChipsScroll;
    private LinearLayout mChipsContainer;
    private boolean mIsSearchPillEnabled;
    private boolean mIsChipsRowWanted;
    private int mActiveChipId = -1;
    private OnChipClickListener mOnChipClickListener;

    /**
     * NovaTV: category chip pressed
     */
    public interface OnChipClickListener {
        void onChipClick(int sectionId);
    }

    /**
     * NovaTV: chip data (section id + label)
     */
    public static class Chip {
        public final int sectionId;
        public final CharSequence title;

        public Chip(int sectionId, CharSequence title) {
            this.sectionId = sectionId;
            this.title = title;
        }
    }

    public NavigateTitleView(Context context) {
        super(context);
    }

    public NavigateTitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NavigateTitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // findViewById is null in constructor. Init mAccountView later.
    }

    @Override
    public View focusSearch(View focused, int direction) {
        View novaResult = novaFocusSearch(focused, direction);
        if (novaResult != null) {
            return novaResult;
        }

        View nextFoundFocusableViewInLayout = null;

        // Only concerned about focusing left and right at the moment
        if (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) {

            // Try to find the next focusable item in this layout for the supplied direction
            int nextFoundFocusableViewInLayoutId = -1;
            switch(direction) {
                case View.FOCUS_LEFT :
                    nextFoundFocusableViewInLayoutId = focused.getNextFocusLeftId();
                    break;
                case View.FOCUS_RIGHT :
                    nextFoundFocusableViewInLayoutId = focused.getNextFocusRightId();
                    break;
            }

            // View id for next focus direction found....get the View
            if (nextFoundFocusableViewInLayoutId != -1) {
                nextFoundFocusableViewInLayout = findViewById(nextFoundFocusableViewInLayoutId);
            }
        }

        //  Return the found View in the layout if it's focusable
        if (nextFoundFocusableViewInLayout != null && nextFoundFocusableViewInLayout != focused && nextFoundFocusableViewInLayout.isFocusable()) {
            if (nextFoundFocusableViewInLayout.getVisibility() == View.VISIBLE) {
                return nextFoundFocusableViewInLayout;
            } else {
                return focusSearch(nextFoundFocusableViewInLayout, direction);
            }
        } else {
            // No focusable view found in layout...propagate to super (should invoke the BrowseFrameLayout.OnFocusSearchListener
            return super.focusSearch(focused, direction);
        }
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        // NovaTV: coming up from the rows lands on the active chip, otherwise on the search pill
        if (direction == View.FOCUS_UP && focusActiveChip()) {
            return true;
        }

        if (mSearchField != null && mSearchField.getVisibility() == View.VISIBLE && mSearchPill.getVisibility() == View.VISIBLE
                && mSearchField.requestFocus()) {
            return true;
        }

        // Gives focus to the SearchOrb first....if not...default to normal descendant focus search
        return getSearchAffordanceView().requestFocus() || super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    /**
     * NovaTV: dpad navigation inside the two header rows (top bar and chips).
     * Returns null to fall back to the stock behavior.
     */
    @Nullable
    private View novaFocusSearch(View focused, int direction) {
        if (focused == null || mTopBar == null) {
            return null;
        }

        boolean inChips = isChild(mChipsContainer, focused);
        boolean inTopBar = !inChips && isChild(mTopBar, focused);

        if (!inChips && !inTopBar) {
            return null;
        }

        if (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) {
            View next = FocusFinder.getInstance().findNextFocus(inChips ? mChipsContainer : mTopBar, focused, direction);

            if (next != null) {
                return next;
            }

            // End of the row: stay. Start of the row: stock behavior (opens the side rail).
            boolean towardEnd = (direction == View.FOCUS_RIGHT) != (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
            return towardEnd ? focused : null;
        }

        if (direction == View.FOCUS_DOWN && inTopBar && isChipsRowVisible()) {
            View chip = getActiveChip();
            return chip != null ? chip : focused;
        }

        if (direction == View.FOCUS_UP && inChips) {
            if (mSearchPill.getVisibility() == View.VISIBLE) {
                return mSearchField;
            }
            View first = FocusFinder.getInstance().findNextFocus(mTopBar, null, View.FOCUS_RIGHT);
            return first != null ? first : focused;
        }

        return null;
    }

    private static boolean isChild(ViewGroup parent, View view) {
        if (parent == null) {
            return false;
        }

        View current = view;
        while (current != null) {
            if (current == parent) {
                return true;
            }
            current = current.getParent() instanceof View ? (View) current.getParent() : null;
        }

        return false;
    }

    // ===================== NovaTV: category chips =====================

    /**
     * Rebuild the chips. First chip should be the "All" one.
     */
    public void setChips(List<Chip> chips, OnChipClickListener listener) {
        init();

        if (mChipsContainer == null) {
            return;
        }

        mOnChipClickListener = listener;
        mChipsContainer.removeAllViews();

        int height = getResources().getDimensionPixelSize(R.dimen.nova_chip_height);
        int paddingH = getResources().getDimensionPixelSize(R.dimen.nova_chip_padding_h);
        int spacing = getResources().getDimensionPixelSize(R.dimen.nova_chip_spacing);

        for (Chip chip : chips) {
            TextView chipView = new TextView(getContext());
            chipView.setText(chip.title);
            chipView.setTag(chip.sectionId);
            chipView.setSingleLine(true);
            chipView.setGravity(android.view.Gravity.CENTER);
            chipView.setPadding(paddingH, 0, paddingH, 0);
            chipView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.nova_text_body));
            chipView.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.novatv_chip_text));
            int chipBg = com.liskovsoft.sharedutils.helpers.Helpers.getThemeAttr(getContext(), R.attr.novaChipBg); // accent dependent
            chipView.setBackgroundResource(chipBg > 0 ? chipBg : R.drawable.novatv_chip_bg);
            chipView.setFocusable(true);
            chipView.setClickable(true);
            chipView.setSelected(chip.sectionId == mActiveChipId);
            chipView.setOnClickListener(v -> {
                if (mOnChipClickListener != null) {
                    mOnChipClickListener.onChipClick((int) v.getTag());
                }
            });
            chipView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    mChipsScroll.requestChildRectangleOnScreen(mChipsContainer,
                            new Rect(v.getLeft() - spacing, 0, v.getRight() + spacing, v.getHeight()), false);
                }
            });
            com.liskovsoft.smartyoutubetv2.common.misc.NovaFonts.apply(chipView);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height);
            lp.setMarginEnd(spacing);
            mChipsContainer.addView(chipView, lp);
        }

        updateChipsVisibility();
    }

    /**
     * Highlight the chip of the open section and show the row only on Home/category sections.
     */
    public void setActiveChip(int sectionId, boolean showRow) {
        mActiveChipId = sectionId;
        mIsChipsRowWanted = showRow;

        if (mChipsContainer != null) {
            for (int i = 0; i < mChipsContainer.getChildCount(); i++) {
                View chip = mChipsContainer.getChildAt(i);
                chip.setSelected(chip.getTag() instanceof Integer && (int) chip.getTag() == sectionId);
            }
        }

        updateChipsVisibility();
    }

    private void updateChipsVisibility() {
        if (mChipsScroll == null) {
            return;
        }

        boolean visible = mIsChipsRowWanted && mChipsContainer.getChildCount() > 1 && mSearchVisibility == View.VISIBLE;
        mChipsScroll.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private boolean isChipsRowVisible() {
        return mChipsScroll != null && mChipsScroll.getVisibility() == View.VISIBLE;
    }

    @Nullable
    private View getActiveChip() {
        if (!isChipsRowVisible() || mChipsContainer.getChildCount() == 0) {
            return null;
        }

        for (int i = 0; i < mChipsContainer.getChildCount(); i++) {
            View chip = mChipsContainer.getChildAt(i);
            if (chip.isSelected()) {
                return chip;
            }
        }

        return mChipsContainer.getChildAt(0);
    }

    private boolean focusActiveChip() {
        View chip = getActiveChip();
        return chip != null && chip.requestFocus();
    }

    @Override
    public void updateComponentsVisibility(int flags) {
        // Fix for: Fatal Exception: java.lang.IllegalStateException
        // Fragment has not been attached yet.
        // Inside: super.updateComponentsVisibility(flags);
        if (getWindowToken() == null) {
            return;
        }

        super.updateComponentsVisibility(flags);

        init();

        mFlags = flags;

        mSearchVisibility = (flags & SEARCH_VIEW_VISIBLE) == SEARCH_VIEW_VISIBLE
                ? View.VISIBLE : View.INVISIBLE;

        mBrandingVisibility = (flags & BRANDING_VIEW_VISIBLE) == BRANDING_VIEW_VISIBLE
                ? View.VISIBLE : View.INVISIBLE;

        // NovaTV: the leanback orb is replaced by the search pill
        mSearchOrbView.setVisibility(View.GONE);

        if (mSearchPill != null) {
            mSearchPill.setVisibility(mIsSearchPillEnabled ? mSearchVisibility : View.GONE);
        }

        updateChipsVisibility();

        if (mIsAccountViewEnabled) {
            mAccountView.setVisibility(mSearchVisibility);
        }

        if (mIsLanguageViewEnabled) {
            mLanguageView.setVisibility(mSearchVisibility);
        }

        if (mExitPip != null && (PlaybackPresenter.instance(getContext()).isRunningInBackground() || mSearchVisibility != View.VISIBLE)) {
            mExitPip.setVisibility(mSearchVisibility);
            mPipTitle.setVisibility(mSearchVisibility);
        }

        if (mIsGlobalClockEnabled) {
            mGlobalClock.setVisibility(mBrandingVisibility);
        }

        if (mIsGlobalClockEnabled) {
            mGlobalDate.setVisibility(mBrandingVisibility);
        }
    }

    private void init() {
        if (mInitDone) {
            return;
        }

        MediaServiceManager.instance().addAccountListener(this);

        setupButtons();

        MainUIData mainUIData = MainUIData.instance(getContext());
        mainUIData.setOnChange(this);

        mInitDone = true;
    }

    private void setupButtons() {
        MainUIData mainUIData = MainUIData.instance(getContext());

        mSearchOrbView = findViewById(R.id.title_orb);

        // NovaTV
        mTopBar = findViewById(R.id.nova_top_bar);
        mSearchPill = findViewById(R.id.nova_search_pill);
        mSearchField = findViewById(R.id.nova_search_field);
        mSearchMic = findViewById(R.id.nova_search_mic);
        mChipsScroll = findViewById(R.id.nova_chips_scroll);
        mChipsContainer = findViewById(R.id.nova_chips);

        if (mSearchField != null) {
            mSearchField.setOnClickListener(v -> SearchPresenter.instance(getContext()).startSearch(null));
            mSearchMic.setOnClickListener(v -> SearchPresenter.instance(getContext()).startVoice());
            TooltipCompatHandler.setTooltipText(mSearchMic, getContext().getString(R.string.novatv_voice_search));
            // Chain with the tooltip's focus listener (dark icon on the white focused disc)
            OnFocusChangeListener tooltipListener = mSearchMic.getOnFocusChangeListener();
            mSearchMic.setOnFocusChangeListener((v, hasFocus) -> {
                ((android.widget.ImageView) v).setColorFilter(
                        ContextCompat.getColor(getContext(), hasFocus ? R.color.nova_text_on_light : R.color.nova_text_primary));
                if (tooltipListener != null) {
                    tooltipListener.onFocusChange(v, hasFocus);
                }
            });
            ((android.widget.ImageView) mSearchMic).setColorFilter(ContextCompat.getColor(getContext(), R.color.nova_text_primary));
        }

        mAccountView = findViewById(R.id.account_orb);
        mAccountView.setOnOrbClickedListener(v -> AccountSelectionPresenter.instance(getContext()).nextAccountOrDialog());
        mAccountView.setOnOrbLongClickedListener(v -> {
            AccountSettingsPresenter.instance(getContext()).show();
            return true;
        });
        TooltipCompatHandler.setTooltipText(mAccountView, getContext().getString(R.string.settings_accounts));

        mLanguageView = findViewById(R.id.language_orb);
        mLanguageView.setOnOrbClickedListener(v -> LanguageSettingsPresenter.instance(getContext()).show());
        TooltipCompatHandler.setTooltipText(mLanguageView, getContext().getString(R.string.settings_language_country));

        mExitPip = findViewById(R.id.exit_pip);
        mPipTitle = findViewById(R.id.pip_title);
        mExitPip.setOnOrbClickedListener(v -> ViewManager.instance(getContext()).startView(PlaybackView.class));
        ViewUtil.enableMarquee(mPipTitle);
        ViewUtil.setTextScrollSpeed(mPipTitle, mainUIData.getCardTextScrollSpeed());
        TooltipCompatHandler.setTooltipText(mExitPip, getContext().getString(R.string.return_to_background_video));

        mGlobalClock = findViewById(R.id.global_time);
        mGlobalClock.showDate(false);

        mGlobalDate = findViewById(R.id.global_date);
        mGlobalDate.showTime(false);
        mGlobalDate.showDate(true);

        updateButtonsVisibility();
    }

    private void updateButtonsVisibility() {
        MainUIData mainUIData = MainUIData.instance(getContext());

        mIsSearchOrbEnabled = !mainUIData.isTopButtonEnabled(MainUIData.TOP_BUTTON_SEARCH);
        mIsAccountViewEnabled = mainUIData.isTopButtonEnabled(MainUIData.TOP_BUTTON_BROWSE_ACCOUNTS);
        mIsLanguageViewEnabled = mainUIData.isTopButtonEnabled(MainUIData.TOP_BUTTON_CHANGE_LANGUAGE);
        mIsGlobalClockEnabled = GeneralData.instance(getContext()).isGlobalClockEnabled();

        // NovaTV: TOP_BUTTON_SEARCH now drives the search pill
        mIsSearchPillEnabled = !mIsSearchOrbEnabled && mSearchPill != null;
        mSearchOrbView.setVisibility(View.GONE);
        if (mSearchPill != null) {
            mSearchPill.setVisibility(mIsSearchPillEnabled ? View.VISIBLE : View.GONE);
        }
        mAccountView.setVisibility(mIsAccountViewEnabled ? View.VISIBLE : View.GONE);
        mLanguageView.setVisibility(mIsLanguageViewEnabled ? View.VISIBLE : View.GONE);
        mGlobalClock.setVisibility(mIsGlobalClockEnabled ? View.VISIBLE : View.GONE);
        mGlobalDate.setVisibility(mIsGlobalClockEnabled ? View.VISIBLE : View.GONE);

        Utils.postDelayed(this::updateAccountIcon, 1_000); // give a time to engine to fetch an updated icon url
        //updateAccountIcon();
        updateLanguageIcon();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility == View.VISIBLE) { // scroll grid up, scroll grid down
            applyPipParameters();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if (hasWindowFocus) { // pip window closed, dialog closed
            applyPipParameters();
        }
    }

    @Override
    public void onAccountChanged(Account account) {
        updateAccountIcon();
    }

    private void applyPipParameters() {
        if (mExitPip != null) {
            int newVisibility = PlaybackPresenter.instance(getContext()).isRunningInBackground() ? mSearchVisibility : View.INVISIBLE;
            mExitPip.setVisibility(newVisibility);
            mPipTitle.setVisibility(newVisibility);

            if (newVisibility == View.VISIBLE) {
                Video video = PlaybackPresenter.instance(getContext()).getVideo();
                mPipTitle.setText(video != null ? String.format("%s - %s", video.getTitle(), video.getAuthor()) : "");
            }
        }
    }

    private void updateAccountIcon() {
        if (!mIsAccountViewEnabled) {
            return;
        }

        Account current = MediaServiceManager.instance().getSelectedAccount();

        if (current != null && current.getAvatarImageUrl() != null) {
            loadIcon(mAccountView, current.getAvatarImageUrl(), false);
            String accountName = current.getName() != null ? current.getName() : current.getEmail();
            //TooltipCompatHandler.setTooltipText(mAccountView, Utils.updateTooltip(getContext(), accountName));
            TooltipCompatHandler.setTooltipText(mAccountView, accountName);
        } else {
            Colors orbColors = mAccountView.getOrbColors();
            mAccountView.setOrbColors(new Colors(orbColors.color, orbColors.brightColor, ContextCompat.getColor(getContext(), R.color.orb_icon_color)));
            mAccountView.setOrbIcon(ContextCompat.getDrawable(getContext(), R.drawable.browse_title_account));
            TooltipCompatHandler.setTooltipText(mAccountView, getContext().getString(R.string.dialog_account_none));
        }
    }

    private void updateLanguageIcon() {
        if (!mIsLanguageViewEnabled) {
            return;
        }

        // Use delay to fix icon initialization on app boot
        Utils.postDelayed(() -> {
            Locale locale = LocaleUtility.getCurrentLocale(getContext());
            loadIcon(mLanguageView, Utils.getCountryFlagUrl(locale.getCountry()), true); // flag server could be down
            TooltipCompatHandler.setTooltipText(mLanguageView, String.format("%s (%s)", locale.getDisplayCountry(), locale.getDisplayLanguage()));
        }, 100);
    }

    private void loadIcon(SearchOrbView view, String url, boolean useCache) {
        if (view == null) {
            return;
        }

        // The view with GONE visibility has zero width and height
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            Utils.postDelayed(() -> loadIcon(view, url, useCache), 500);
            return;
        }

        Context context = view.getContext();

        if (context instanceof Activity && !Utils.checkActivity((Activity) context)) {
            return;
        }

        // Size of the view might increase after icon change (bug on some firmwares). So, it's better to cache initial values.
        if (mIconWidth == 0 || mIconHeight == 0) {
            mIconWidth = view.getWidth();
            mIconHeight = view.getHeight();
        }

        try {
            loadIcon(context, view, url, mIconWidth, mIconHeight, useCache);
        } catch (ExceptionInInitializerError e) {
            // Glide Kivi error
            e.printStackTrace();
        }
    }

    private static void loadIcon(Context context, SearchOrbView view, String url, int iconWidth, int iconHeight, boolean useCache) {
        Glide.with(context)
                .load(url)
                .apply(ViewUtil.glideOptions())
                .diskCacheStrategy(useCache ? DiskCacheStrategy.ALL : DiskCacheStrategy.NONE)
                .circleCrop() // resize image
                .into(new SimpleTarget<Drawable>(iconWidth, iconHeight) {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        Colors orbColors = view.getOrbColors();
                        view.setOrbColors(new Colors(orbColors.color, orbColors.brightColor, Color.TRANSPARENT));
                        view.setOrbIcon(resource);
                    }
                });
    }

    @Override
    public void onDataChange() {
        try {
            updateButtonsVisibility();
            updateComponentsVisibility(mFlags);
        } catch (IllegalStateException e) {
            // Fragment BrowseFragment has not been attached yet.
            e.printStackTrace();
        }
    }
}
