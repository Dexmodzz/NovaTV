package com.liskovsoft.smartyoutubetv2.tv.util;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * NovaTV: loading spinner. A thin white arc with round ends on a faint ring,
 * the arc grows and shrinks while it turns (no theme color).
 */
public final class NovaSpinnerDrawable extends Drawable implements Animatable {
    private static final long DURATION_MS = 1333;
    private static final float MIN_SWEEP = 18f;
    private static final float MAX_SWEEP = 270f;
    private final Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mOval = new RectF();
    private final ValueAnimator mAnimator;
    private final int mSize;
    private final float mStroke;
    private float mFraction;
    private int mCycle;

    public NovaSpinnerDrawable(Context context) {
        mSize = context.getResources().getDimensionPixelSize(R.dimen.nova_spinner_size);
        mStroke = context.getResources().getDimension(R.dimen.nova_spinner_stroke);

        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);
        mArcPaint.setStrokeWidth(mStroke);
        mArcPaint.setColor(0xFFFFFFFF);

        mTrackPaint.setStyle(Paint.Style.STROKE);
        mTrackPaint.setStrokeWidth(mStroke);
        mTrackPaint.setColor(0x26FFFFFF);

        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(DURATION_MS);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(animation -> {
            mFraction = (float) animation.getAnimatedValue();
            invalidateSelf();
        });
        mAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {
                mCycle++;
            }
        });
    }

    /**
     * Use the spinner in a progress bar of the given size (dp from nova_spinner_size).
     */
    public static void apply(@Nullable ProgressBar progressBar) {
        if (progressBar == null) {
            return;
        }

        progressBar.setIndeterminate(true);
        progressBar.setIndeterminateDrawable(new NovaSpinnerDrawable(progressBar.getContext()));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        float size = Math.min(bounds.width(), bounds.height());
        float inset = mStroke / 2f;
        float left = bounds.centerX() - size / 2f + inset;
        float top = bounds.centerY() - size / 2f + inset;
        mOval.set(left, top, left + size - mStroke, top + size - mStroke);

        canvas.drawOval(mOval, mTrackPaint);

        // First half: the head runs ahead (arc grows), second half: the tail catches up (arc shrinks)
        float t = mFraction;
        float grow = ease(Math.min(1f, t * 2f));
        float shrink = ease(Math.max(0f, t * 2f - 1f));
        float sweep = MIN_SWEEP + (MAX_SWEEP - MIN_SWEEP) * (grow - shrink);
        float tail = (MAX_SWEEP - MIN_SWEEP) * shrink;
        // The whole arc also turns, every cycle starts where the previous one ended
        float rotation = t * 360f * 0.8f + mCycle * ((MAX_SWEEP - MIN_SWEEP) + 288f);
        float start = -90f + rotation + tail;

        canvas.drawArc(mOval, start % 360f, sweep, false, mArcPaint);
    }

    private static float ease(float x) {
        // ease in-out
        return x < 0.5f ? 4f * x * x * x : 1f - (float) Math.pow(-2f * x + 2f, 3) / 2f;
    }

    @Override
    public int getIntrinsicWidth() {
        return mSize;
    }

    @Override
    public int getIntrinsicHeight() {
        return mSize;
    }

    @Override
    public void start() {
        if (!mAnimator.isStarted()) {
            mAnimator.start();
        }
    }

    @Override
    public void stop() {
        mAnimator.cancel();
    }

    @Override
    public boolean isRunning() {
        return mAnimator.isRunning();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (visible) {
            start();
        } else {
            stop();
        }
        return changed;
    }

    @Override
    public void setAlpha(int alpha) {
        mArcPaint.setAlpha(alpha);
        mTrackPaint.setAlpha(alpha * 0x26 / 0xFF);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mArcPaint.setColorFilter(colorFilter);
        mTrackPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
