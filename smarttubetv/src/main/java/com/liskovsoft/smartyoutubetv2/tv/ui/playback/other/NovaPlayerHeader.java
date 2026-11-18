package com.liskovsoft.smartyoutubetv2.tv.ui.playback.other;

import android.view.View;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.time.DateTimeView;

/**
 * NovaTV: player top header (clock in the corner). Fades together with the controls.<br/>
 * Title and channel are in {@link NovaPlayerTitle}.
 */
public final class NovaPlayerHeader {
    private static final int FADE_MS = 200;
    private final View mRoot;
    private final View mClock;
    private boolean mIsShown;

    private NovaPlayerHeader(View root) {
        mRoot = root;
        mClock = root.findViewById(R.id.nova_player_header_clock);

        DateTimeView time = root.findViewById(R.id.nova_player_header_time);
        DateTimeView date = root.findViewById(R.id.nova_player_header_date);
        time.showDate(false);
        date.showTime(false);
        // The first text is written on creation (date + time): refresh with the new flags
        time.onTickle();
        date.onTickle();
    }

    @Nullable
    public static NovaPlayerHeader create(@Nullable View fragmentRoot) {
        View root = fragmentRoot != null ? fragmentRoot.findViewById(R.id.nova_player_header) : null;
        return root != null ? new NovaPlayerHeader(root) : null;
    }

    /**
     * Bottom edge of the header (estimated from its sizes while it has never been laid out).
     */
    public int getBottomInWindow() {
        if (mRoot.getHeight() > 0) {
            int[] pos = new int[2];
            mRoot.getLocationInWindow(pos);
            return pos[1] + mRoot.getHeight();
        }

        return mRoot.getPaddingTop() + mRoot.getResources().getDimensionPixelSize(R.dimen.nova_player_header_avatar_size);
    }

    public void setClockEnabled(boolean enabled) {
        mClock.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    public void show(boolean show, boolean animate) {
        if (show == mIsShown) {
            return;
        }

        mIsShown = show;
        mRoot.animate().cancel(); // interruptible: starts from the current alpha

        if (show) {
            mRoot.setVisibility(View.VISIBLE);
        }

        if (!animate) {
            mRoot.setAlpha(show ? 1 : 0);
            mRoot.setVisibility(show ? View.VISIBLE : View.GONE);
            return;
        }

        mRoot.animate()
                .alpha(show ? 1 : 0)
                .setDuration(FADE_MS)
                .withEndAction(show ? null : () -> mRoot.setVisibility(View.GONE))
                .start();
    }
}
