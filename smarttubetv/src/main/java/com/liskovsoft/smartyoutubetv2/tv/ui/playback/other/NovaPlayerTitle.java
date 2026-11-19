package com.liskovsoft.smartyoutubetv2.tv.ui.playback.other;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.NovaTVData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.NovaAvatars;

/**
 * NovaTV: video title + "avatar · channel · subscribers" in the player.<br/>
 * Where it goes depends on the NovaTV setting "player title position":
 * <ul>
 *     <li>top (default): the centered top header (nova_player_header_info in lb_playback_fragment.xml)</li>
 *     <li>above info: a block inserted at the top of the player description,
 *     right above the views/date/likes line</li>
 * </ul>
 * The leanback title of the description always stays empty.
 */
public final class NovaPlayerTitle {
    private NovaPlayerTitle() {
    }

    public static void bind(@Nullable AbstractDetailsDescriptionPresenter.ViewHolder vh,
                            @Nullable CharSequence title, @Nullable Video video) {
        if (vh == null || !(vh.view instanceof ViewGroup)) {
            return;
        }

        ViewGroup root = (ViewGroup) vh.view;
        boolean onTop = NovaTVData.instance(root.getContext()).getPlayerTitlePosition() == NovaTVData.PLAYER_TITLE_TOP;

        // Top header lives in the fragment layout (same window)
        View header = root.getRootView().findViewById(R.id.nova_player_header_info);
        View block = root.findViewById(R.id.nova_player_title_block);

        if (onTop && header != null) {
            if (block != null) {
                block.setVisibility(View.GONE);
            }

            header.setVisibility(View.VISIBLE);
            bindViews(header.findViewById(R.id.nova_player_header_title),
                    header.findViewById(R.id.nova_player_header_channel_row),
                    header.findViewById(R.id.nova_player_header_subtitle),
                    header.findViewById(R.id.nova_player_header_avatar),
                    title, video);
            return;
        }

        if (header != null) {
            header.setVisibility(View.GONE);
        }

        if (block == null) {
            block = LayoutInflater.from(root.getContext()).inflate(R.layout.novatv_player_title_block, root, false);
            root.addView(block, 0);
        }

        block.setVisibility(View.VISIBLE);
        bindViews(block.findViewById(R.id.nova_player_title),
                block.findViewById(R.id.nova_player_channel_row),
                block.findViewById(R.id.nova_player_channel_text),
                block.findViewById(R.id.nova_player_channel_avatar),
                title, video);
    }

    private static void bindViews(TextView titleView, View channelRow, TextView channelText, ImageView avatar,
                                  @Nullable CharSequence title, @Nullable Video video) {
        titleView.setText(title);
        titleView.setVisibility(TextUtils.isEmpty(title) ? View.GONE : View.VISIBLE);

        String author = video != null ? video.getAuthor() : null;
        CharSequence channel = author;
        if (video != null && !TextUtils.isEmpty(video.subscriberCount)) {
            channel = TextUtils.isEmpty(author) ? video.subscriberCount : author + "  ·  " + video.subscriberCount;
        }

        if (TextUtils.isEmpty(channel)) {
            channelRow.setVisibility(View.GONE);
            NovaAvatars.unbind(avatar);
            avatar.setTag(R.id.nova_player_channel_row, null);
        } else {
            channelRow.setVisibility(View.VISIBLE);
            channelText.setText(channel);
            // The description is rebound often (title/subtitle updates): load the avatar once per video
            String videoId = video.videoId;
            if (videoId == null || !videoId.equals(avatar.getTag(R.id.nova_player_channel_row))) {
                NovaAvatars.bind(avatar, video);
                avatar.setTag(R.id.nova_player_channel_row, videoId);
            }
        }
    }
}
