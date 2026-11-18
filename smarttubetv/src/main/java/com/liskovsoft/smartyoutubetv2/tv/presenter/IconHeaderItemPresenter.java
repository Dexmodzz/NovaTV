package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowHeaderPresenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class IconHeaderItemPresenter extends RowHeaderPresenter {
    private static final String TAG = IconHeaderItemPresenter.class.getSimpleName();
    private float mUnselectedAlpha;
    private final int mResId;
    private final String mIconUrl;
    private Drawable mDefaultIcon;
    // NovaTV: rail styling
    private final boolean mIsHidden;
    private int mAccentColor;
    private int mIdleIconColor;
    private int mSelectedTextColor;
    private int mIdleTextColor;

    public IconHeaderItemPresenter(int resId, String iconUrl) {
        this(resId, iconUrl, false);
    }

    /**
     * @param isHidden NovaTV: section shown as a chip, the rail item takes no space and can't be focused
     */
    public IconHeaderItemPresenter(int resId, String iconUrl, boolean isHidden) {
        mResId = resId;
        mIconUrl = iconUrl;
        mIsHidden = isHidden;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        mUnselectedAlpha = viewGroup.getResources()
                .getFraction(R.fraction.lb_browse_header_unselect_alpha, 1, 1);
        LayoutInflater inflater = (LayoutInflater) viewGroup.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDefaultIcon = new ColorDrawable(ContextCompat.getColor(viewGroup.getContext(), R.color.lb_grey));

        View view = inflater.inflate(R.layout.icon_header_item, null);
        // NovaTV: inflated without parent, so restore the fixed rail item width
        view.setLayoutParams(new ViewGroup.LayoutParams(
                viewGroup.getResources().getDimensionPixelSize(R.dimen.nova_rail_item_width), ViewGroup.LayoutParams.WRAP_CONTENT));
        // NovaTV: selection is shown with background + accent instead of opacity
        mUnselectedAlpha = 1.0f;

        Context context = viewGroup.getContext();
        int accentRes = Helpers.getThemeAttr(context, R.attr.novaAccent);
        mAccentColor = ContextCompat.getColor(context, accentRes > 0 ? accentRes : R.color.nova_accent);
        mIdleIconColor = ContextCompat.getColor(context, R.color.nova_text_secondary);
        mSelectedTextColor = ContextCompat.getColor(context, R.color.nova_text_primary);
        mIdleTextColor = ContextCompat.getColor(context, R.color.nova_text_secondary);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        HeaderItem headerItem;

        if (item instanceof PageRow) {
            headerItem = ((PageRow) item).getHeaderItem();
        } else {
            headerItem = ((ListRow) item).getHeaderItem();
        }

        View rootView = viewHolder.view;
        rootView.setFocusable(!mIsHidden);
        applyHidden(rootView);

        ImageView iconView = rootView.findViewById(R.id.header_icon);
        if (iconView != null) {
            if (mIconUrl != null) {
                Glide.with(rootView.getContext())
                        .load(mIconUrl)
                        .apply(ViewUtil.glideOptions().error(mDefaultIcon))
                        .listener(mErrorListener)
                        .into(iconView);

                //ViewUtil.makeMonochrome(iconView);
            } else {
                Drawable icon = mResId > 0 ? ContextCompat.getDrawable(rootView.getContext(), mResId) : mDefaultIcon;
                iconView.setImageDrawable(icon);
            }
        }

        TextView label = rootView.findViewById(R.id.header_label);
        if (label != null) {
            label.setText(headerItem.getName());
        }

        if (viewHolder instanceof RowHeaderPresenter.ViewHolder) {
            applySelected(rootView, ((RowHeaderPresenter.ViewHolder) viewHolder).getSelectLevel() > 0.5f);
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        // NOP
    }

    // TODO: This is a temporary fix. Remove me when leanback onCreateViewHolder no longer sets the
    // mUnselectAlpha, and also assumes the xml inflation will return a RowHeaderView.
    @Override
    protected void onSelectLevelChanged(RowHeaderPresenter.ViewHolder holder) {
        holder.view.setAlpha(mUnselectedAlpha + holder.getSelectLevel() *
                (1.0f - mUnselectedAlpha));
        applySelected(holder.view, holder.getSelectLevel() > 0.5f);
    }

    /**
     * NovaTV: hidden rail items collapse to zero size. The Leanback wrapper (parent) is hidden too,
     * so GridLayoutManager.canScrollTo() skips it during dpad navigation.
     */
    private void applyHidden(View rootView) {
        int visibility = mIsHidden ? View.GONE : View.VISIBLE;
        rootView.setVisibility(visibility);

        if (rootView.getParent() instanceof View) {
            ((View) rootView.getParent()).setVisibility(visibility);
        }
    }

    /**
     * NovaTV: selected item = #26262A rounded background, accent icon, primary label.
     */
    private void applySelected(View rootView, boolean selected) {
        rootView.setBackgroundResource(selected ? R.drawable.novatv_rail_item_selected : 0);

        ImageView iconView = rootView.findViewById(R.id.header_icon);
        // Remote icons (channel avatars) keep their colors
        if (iconView != null && mIconUrl == null) {
            iconView.setColorFilter(selected ? mAccentColor : mIdleIconColor, PorterDuff.Mode.SRC_IN);
        }

        TextView label = rootView.findViewById(R.id.header_label);
        if (label != null) {
            label.setTextColor(selected ? mSelectedTextColor : mIdleTextColor);
        }
    }

    private final RequestListener<Drawable> mErrorListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };
}
