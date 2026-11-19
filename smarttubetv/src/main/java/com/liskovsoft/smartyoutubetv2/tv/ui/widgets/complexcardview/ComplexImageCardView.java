package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.complexcardview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.ImageCardView;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class ComplexImageCardView extends ImageCardView {
    private ComplexImageView mComplexImageView;
    private Handler mHandler;
    private boolean mIsCardTextAutoScrollEnabled;
    private boolean mIsBadgeEnabled;
    // NovaTV
    private ImageView mAvatarView;
    private Paint mFocusPaint;
    private final RectF mFocusRect = new RectF();
    private float mFocusRadius;
    private boolean mIsChannelStyle;
    private final android.graphics.Rect mImageRect = new android.graphics.Rect();

    public ComplexImageCardView(Context context) {
        super(context);

        init();
    }

    public ComplexImageCardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public ComplexImageCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        mComplexImageView = findViewById(R.id.main_image_wrapper);
        mHandler = new Handler(Looper.getMainLooper());
        initNovaStyle();
    }

    /**
     * NovaTV: 14dp rounded card, white focus ring (instead of the white info box).
     */
    private void initNovaStyle() {
        int radius = getResources().getDimensionPixelSize(R.dimen.nova_radius_card);

        if (Build.VERSION.SDK_INT >= 21) {
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    if (mIsChannelStyle) {
                        outline.setEmpty(); // no card shape (and no shadow) around a channel
                    } else {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                    }
                }
            });
            setClipToOutline(true);
        }

        // Focus ring is drawn in dispatchDraw(): BaseCardView filters selected/focused drawable states
        // and the Leanback dimmer replaces the card foreground, so a foreground selector can't be used.
        mFocusRadius = radius;
        mFocusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFocusPaint.setStyle(Paint.Style.STROKE);
        mFocusPaint.setStrokeWidth(getResources().getDimension(R.dimen.nova_focus_outline_width));
        mFocusPaint.setColor(ContextCompat.getColor(getContext(), R.color.nova_focus_outline));
        setWillNotDraw(false);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (isSelected() && mFocusPaint != null && mIsChannelStyle) {
            // Ring around the round channel picture (centered in the image area)
            View image = getMainImageView();
            if (image != null && image.getWidth() > 0) {
                image.getDrawingRect(mImageRect);
                offsetDescendantRectToMyCoords(image, mImageRect);
                float radius = Math.min(mImageRect.width(), mImageRect.height()) / 2f;
                canvas.drawCircle(mImageRect.exactCenterX(), mImageRect.exactCenterY(),
                        radius - mFocusPaint.getStrokeWidth() / 2, mFocusPaint); // inside the card bounds
            }
        } else if (isSelected() && mFocusPaint != null) {
            float inset = mFocusPaint.getStrokeWidth() / 2;
            mFocusRect.set(inset, inset, getWidth() - inset, getHeight() - inset);
            canvas.drawRoundRect(mFocusRect, mFocusRadius - inset, mFocusRadius - inset, mFocusPaint);
        }
    }

    /**
     * NovaTV: round channel avatar to the left of title/content.
     * The view is created lazily, so cards that don't use it (e.g. shorts) are untouched.
     */
    public ImageView enableAvatar() {
        if (mAvatarView != null) {
            return mAvatarView;
        }

        View infoField = findViewById(R.id.info_field);
        TextView titleView = findViewById(R.id.title_text);
        TextView contentView = findViewById(R.id.content_text);

        if (!(infoField instanceof RelativeLayout) || titleView == null) {
            return null;
        }

        int size = getResources().getDimensionPixelSize(R.dimen.nova_card_avatar_size);
        mAvatarView = new ImageView(getContext());
        mAvatarView.setId(View.generateViewId());
        mAvatarView.setScaleType(ScaleType.CENTER_CROP);
        mAvatarView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(size, size);
        lp.addRule(RelativeLayout.ALIGN_PARENT_START);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lp.setMarginEnd(getResources().getDimensionPixelSize(R.dimen.nova_space_m));
        lp.topMargin = getResources().getDimensionPixelSize(R.dimen.nova_space_xs);
        ((RelativeLayout) infoField).addView(mAvatarView, lp);

        addEndOf(titleView, mAvatarView.getId());
        addEndOf(contentView, mAvatarView.getId());

        return mAvatarView;
    }

    public ImageView getAvatarView() {
        return mAvatarView;
    }

    /**
     * NovaTV: channel card = round picture (loaded circle-cropped by the presenter) with name and
     * subscribers centered below, no card background. Cards are recycled: call it on every bind.
     */
    public void setChannelStyle(boolean channel) {
        if (channel == mIsChannelStyle) {
            return;
        }

        mIsChannelStyle = channel;

        if (Build.VERSION.SDK_INT >= 21) {
            setClipToOutline(!channel); // the focus ring is drawn just outside the picture
            invalidateOutline();
        }

        int gravity = channel ? android.view.Gravity.CENTER_HORIZONTAL : android.view.Gravity.START;
        int alignment = channel ? View.TEXT_ALIGNMENT_CENTER : View.TEXT_ALIGNMENT_VIEW_START;
        for (int id : new int[] {R.id.title_text, R.id.content_text}) {
            TextView text = findViewById(id);
            if (text != null) {
                text.setGravity(gravity);
                text.setTextAlignment(alignment);
            }
        }

        if (mAvatarView != null) {
            mAvatarView.setVisibility(channel ? View.GONE : View.VISIBLE);
        }

        setMainImageScaleType(channel ? ScaleType.FIT_CENTER : ScaleType.CENTER_CROP);
        invalidate();
    }

    public boolean isChannelStyle() {
        return mIsChannelStyle;
    }

    private static void addEndOf(View view, int anchorId) {
        if (view == null || !(view.getLayoutParams() instanceof RelativeLayout.LayoutParams)) {
            return;
        }

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) view.getLayoutParams();
        lp.removeRule(RelativeLayout.ALIGN_PARENT_START); // would win over END_OF (same as Leanback's icon-left case)
        lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
        lp.addRule(RelativeLayout.END_OF, anchorId);
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        view.setLayoutParams(lp);
    }

    private void enableTitleAnimation(boolean enable) {
        enableTextAnimation(findViewById(R.id.title_text), enable);
    }

    private void enableContentAnimation(boolean enable) {
        TextView content = findViewById(R.id.content_text);
        // NovaTV: "channel\nviews · date" can't scroll on one line (marquee forces single line)
        if (enable && content != null && content.getText() != null && content.getText().toString().contains("\n")) {
            return;
        }
        enableTextAnimation(content, enable);
    }

    private void enableTextAnimation(TextView view, boolean enable) {
        if (view == null) {
            return;
        }

        if (enable) {
            Runnable enableMarquee = () -> ViewUtil.enableMarquee(view);

            mHandler.postDelayed(enableMarquee, 1_000);

            view.setTag(enableMarquee);
        } else {
            if (view.getTag() instanceof Runnable) {
                mHandler.removeCallbacks((Runnable) view.getTag());
                view.setTag(null);
            }

            ViewUtil.disableMarquee(view);
        }
    }

    /**
     * Sets the badge text.
     */
    public void setBadgeText(String text) {
        if (mIsBadgeEnabled) {
            mComplexImageView.setBadgeText(text);
        }
    }

    public void setBadgeColor(int color) {
        mComplexImageView.setBadgeColor(color);
    }

    /**
     * Sets the progress.
     */
    public void setProgress(int percent) {
        mComplexImageView.setProgress(percent);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        invalidate(); // NovaTV: focus ring

        if (mIsCardTextAutoScrollEnabled) {
            enableTitleAnimation(selected);
            enableContentAnimation(selected);
        }

        enableVideoPreview(selected);
    }

    @Override
    public boolean performClick() {
        mComplexImageView.stopPlayback(true);

        return super.performClick();
    }

    @Override
    public boolean performLongClick() {
        mComplexImageView.stopPlayback(true);

        return super.performLongClick();
    }

    private void enableVideoPreview(boolean selected) {
        if (selected) {
            mComplexImageView.startPlayback();
        } else {
            mComplexImageView.stopPlayback();
        }
    }

    public void setPreview(Video video) {
        mComplexImageView.setPreview(video);
    }

    public void setMute(boolean muted) {
        mComplexImageView.setMute(muted);
    }

    public void setTitleLinesNum(int lines) {
        TextView titleView = findViewById(R.id.title_text);

        if (titleView == null || lines <= 0) {
            return;
        }

        titleView.setMaxLines(lines);
        titleView.setLines(lines);
    }

    public void setContentLinesNum(int lines) {
        TextView contentView = findViewById(R.id.content_text);

        if (contentView == null || lines <= 0) {
            return;
        }

        contentView.setMaxLines(lines);
        contentView.setLines(lines);
    }

    public void enableBadge(boolean enabled) {
        mIsBadgeEnabled = enabled;
    }

    public void enableTextAutoScroll(boolean enabled) {
        mIsCardTextAutoScrollEnabled = enabled;
    }

    public void setTextScrollSpeed(float speed) {
        ViewUtil.setTextScrollSpeed(findViewById(R.id.title_text), speed);
        ViewUtil.setTextScrollSpeed(findViewById(R.id.content_text), speed);
    }

    public void enableTitle(boolean enabled) {
        ViewUtil.enableView(findViewById(R.id.title_text), enabled);
    }

    public void enableContent(boolean enabled) {
        ViewUtil.enableView(findViewById(R.id.content_text), enabled);
    }

    /**
     * Enables or disables adjustment of view bounds on the main image.
     */
    @Override
    public void setMainImageAdjustViewBounds(boolean adjustViewBounds) {
        super.setMainImageAdjustViewBounds(adjustViewBounds);
        mComplexImageView.setMainImageAdjustViewBounds(adjustViewBounds);
    }

    /**
     * Sets the ScaleType of the main image.
     */
    @Override
    public void setMainImageScaleType(ScaleType scaleType) {
        super.setMainImageScaleType(scaleType);
        mComplexImageView.setMainImageScaleType(scaleType);
    }

    /**
     * Sets the layout dimensions of the ImageView.
     */
    @Override
    public void setMainImageDimensions(int width, int height) {
        super.setMainImageDimensions(width, height);
        mComplexImageView.setMainImageDimensions(width, height);
    }
}
