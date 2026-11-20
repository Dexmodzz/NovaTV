package com.liskovsoft.smartyoutubetv2.tv.ui.playback.other;

import com.google.android.exoplayer2.DefaultControlDispatcher;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;

/**
 * NovaTV: seeks made by the user (forward/back keys, seek bar) land on the nearest keyframe
 * (max {@link #TOLERANCE_US} away), so the video resumes at once, also from the buffer.<br/>
 * An exact seek must decode every frame from the previous keyframe (slow on 4K).<br/>
 * Other seeks (segment skipping, saved position, remote) are not affected: they stay exact.
 */
public final class NovaSeekDispatcher extends DefaultControlDispatcher {
    private static final long TOLERANCE_US = 4_000_000;
    private static final SeekParameters USER_SEEK = new SeekParameters(TOLERANCE_US, TOLERANCE_US);

    @Override
    public boolean dispatchSeekTo(Player player, int windowIndex, long positionMs) {
        if (!(player instanceof SimpleExoPlayer)) {
            return super.dispatchSeekTo(player, windowIndex, positionMs);
        }

        SimpleExoPlayer exoPlayer = (SimpleExoPlayer) player;
        SeekParameters previous = exoPlayer.getSeekParameters();
        // The player applies these in order: the seek uses the user parameters only
        exoPlayer.setSeekParameters(USER_SEEK);
        boolean result = super.dispatchSeekTo(player, windowIndex, positionMs);
        exoPlayer.setSeekParameters(previous);
        return result;
    }
}
