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
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.NovaAvatars;

/**
 * NovaTV: title + "avatar · channel · subscribers" block, inserted at the top of the player description
 * (right above the views/date/likes line). The leanback title of the description stays empty.
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
        View block = root.findViewById(R.id.nova_player_title_block);

        if (block == null) {
            block = LayoutInflater.from(root.getContext()).inflate(R.layout.novatv_player_title_block, root, false);
            root.addView(block, 0);
        }

        TextView titleView = block.findViewById(R.id.nova_player_title);
        titleView.setText(title);
        titleView.setVisibility(TextUtils.isEmpty(title) ? View.GONE : View.VISIBLE);

        View channelRow = block.findViewById(R.id.nova_player_channel_row);
        TextView channelText = block.findViewById(R.id.nova_player_channel_text);
        ImageView avatar = block.findViewById(R.id.nova_player_channel_avatar);

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
