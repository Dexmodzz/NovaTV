package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.browse;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.bumptech.glide.Glide;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.AccountSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager.AccountChangeListener;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/**
 * NovaTV: profile avatar at the top of the side rail (it replaces the one in the top header).<br/>
 * Same actions as before: OK = next account / account list, long press = account settings.
 * Hidden when the "accounts" top button is turned off in the settings.
 */
public class NovaRailAccountView extends AppCompatImageView implements AccountChangeListener {
    public NovaRailAccountView(Context context) {
        super(context);
        init();
    }

    public NovaRailAccountView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NovaRailAccountView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOnClickListener(v -> AccountSelectionPresenter.instance(getContext()).nextAccountOrDialog());
        setOnLongClickListener(v -> {
            AccountSettingsPresenter.instance(getContext()).show();
            return true;
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        MediaServiceManager.instance().addAccountListener(this);
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        MediaServiceManager.instance().removeAccountListener(this);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            update(); // back from the settings: the accounts button may have been turned on/off
        }
    }

    @Override
    public void onAccountChanged(Account account) {
        post(this::update);
    }

    private void update() {
        Context context = getContext();
        boolean enabled = MainUIData.instance(context).isTopButtonEnabled(MainUIData.TOP_BUTTON_BROWSE_ACCOUNTS);
        setVisibility(enabled ? VISIBLE : GONE);

        if (!enabled || (context instanceof Activity && ((Activity) context).isDestroyed())) {
            return;
        }

        Account current = MediaServiceManager.instance().getSelectedAccount();

        if (current != null && current.getAvatarImageUrl() != null) {
            ImageViewCompat.setImageTintList(this, null);
            Glide.with(context)
                    .load(current.getAvatarImageUrl())
                    .apply(ViewUtil.glideOptions())
                    .circleCrop()
                    .into(this);
            String name = current.getName() != null ? current.getName() : current.getEmail();
            TooltipCompat.setTooltipText(this, name);
        } else {
            Glide.with(context.getApplicationContext()).clear(this);
            setImageResource(R.drawable.browse_title_account);
            ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.nova_text_primary)));
            TooltipCompat.setTooltipText(this, context.getString(R.string.dialog_account_none));
        }
    }
}
