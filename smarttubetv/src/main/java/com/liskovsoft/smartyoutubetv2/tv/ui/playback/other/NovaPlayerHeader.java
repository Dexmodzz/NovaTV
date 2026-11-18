package com.liskovsoft.smartyoutubetv2.tv.ui.playback.other;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.time.DateTimeView;
import com.liskovsoft.smartyoutubetv2.tv.util.NovaAvatars;

/**
 * NovaTV: player top header (channel avatar, title, "channel · subscribers", clock).<br/>
 * Pure view: it's fed by PlaybackFragment with the data the player already has
 * and fades together with the controls.
 */
public final class NovaPlayerHeader {
    private static final int FADE_MS = 200;
    private final View mRoot;
    private final ImageView mAvatar;
    private final TextView mTitle;
    private final TextView mSubtitle;
    private final View mClock;
    private boolean mIsShown;

    private NovaPlayerHeader(View root) {
        mRoot = root;
        mAvatar = root.findViewById(R.id.nova_player_header_avatar);
        mTitle = root.findViewById(R.id.nova_player_header_title);
        mSubtitle = root.findViewById(R.id.nova_player_header_subtitle);
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

    public void bind(@Nullable Video video) {
        if (video == null) {
            return;
        }

        CharSequence title = video.getTitleFull() != null ? video.getTitleFull() : video.getTitle();
        mTitle.setText(title);

        String author = video.getAuthor();
        CharSequence subtitle = author;
        if (!TextUtils.isEmpty(video.subscriberCount)) {
            subtitle = TextUtils.isEmpty(author) ? video.subscriberCount : author + "  ·  " + video.subscriberCount;
        }
        mSubtitle.setText(subtitle);
        mSubtitle.setVisibility(TextUtils.isEmpty(subtitle) ? View.GONE : View.VISIBLE);

        NovaAvatars.bind(mAvatar, video);
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
