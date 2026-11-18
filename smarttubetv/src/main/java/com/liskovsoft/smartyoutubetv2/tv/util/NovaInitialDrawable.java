package com.liskovsoft.smartyoutubetv2.tv.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * NovaTV: round placeholder avatar with the channel's first letter (shown until the real avatar loads).
 */
public class NovaInitialDrawable extends Drawable {
    private static final int[] COLORS = {
            0xFF5C6BC0, 0xFF26A69A, 0xFFEF6C00, 0xFF8E24AA, 0xFF43A047,
            0xFF1E88E5, 0xFFD81B60, 0xFF6D4C41, 0xFF00897B, 0xFF7CB342,
    };
    private final Paint mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String mLetter;
    private final Rect mTextBounds = new Rect();

    public NovaInitialDrawable(@Nullable String name) {
        String trimmed = name != null ? name.trim() : "";
        // Skip leading "@" of handles
        if (trimmed.startsWith("@") && trimmed.length() > 1) {
            trimmed = trimmed.substring(1);
        }
        mLetter = trimmed.isEmpty() ? "" : trimmed.substring(0, trimmed.offsetByCodePoints(0, 1)).toUpperCase();
        mCirclePaint.setColor(COLORS[Math.abs(trimmed.hashCode() % COLORS.length)]);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        float radius = Math.min(bounds.width(), bounds.height()) / 2f;
        float cx = bounds.exactCenterX();
        float cy = bounds.exactCenterY();
        canvas.drawCircle(cx, cy, radius, mCirclePaint);

        if (!mLetter.isEmpty()) {
            mTextPaint.setTextSize(radius);
            mTextPaint.getTextBounds(mLetter, 0, mLetter.length(), mTextBounds);
            canvas.drawText(mLetter, cx, cy + mTextBounds.height() / 2f, mTextPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mCirclePaint.setAlpha(alpha);
        mTextPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mCirclePaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
