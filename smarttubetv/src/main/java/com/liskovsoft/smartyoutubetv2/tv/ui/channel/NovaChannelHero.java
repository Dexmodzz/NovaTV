package com.liskovsoft.smartyoutubetv2.tv.ui.channel;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.utils.NovaChannelInfo;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.NovaAvatars;
import com.liskovsoft.smartyoutubetv2.tv.util.NovaInitialDrawable;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.util.List;

import io.reactivex.disposables.Disposable;

/**
 * NovaTV: fills the channel page header (layout novatv_channel_hero.xml):
 * banner, avatar, name, @handle · subscribers · videos and the subscribe/unsubscribe button.<br/>
 * The button gets the focus when the channel opens. Its state comes from the signed-in account:
 * the metadata of one of the channel's videos says whether the user is subscribed
 * (same source as the player's subscribe button).
 */
public final class NovaChannelHero {
    private static final String TAG = NovaChannelHero.class.getSimpleName();
    private static Disposable sLoadAction;
    private static Disposable sStateAction;
    private static Disposable sToggleAction;
    /** Resolved UC... id of the channel shown (null until known) */
    @Nullable
    private static String sChannelId;
    @Nullable
    private static Video sChannel;
    /** One video of the channel shown: its metadata says whether the user is subscribed */
    @Nullable
    private static String sSampleVideoId;
    private static boolean sStateChecked;

    private NovaChannelHero() {
    }

    public static void bind(View root, @Nullable String channelIdOrHandle, @Nullable Video channel) {
        View hero = root.findViewById(R.id.nova_channel_hero);
        if (hero == null) {
            return;
        }

        if (!NovaChannelInfo.isSupportedId(channelIdOrHandle)) {
            hero.setTag(null);
            hero.setVisibility(View.GONE);
            return;
        }

        if (channelIdOrHandle.equals(hero.getTag())) {
            checkState(root);
            return; // already bound (the header row is rebound on every clear())
        }

        hero.setTag(channelIdOrHandle);
        hero.setVisibility(View.VISIBLE);
        sChannel = channel;
        sChannelId = channelIdOrHandle.startsWith("UC") ? channelIdOrHandle :
                channel != null && channel.channelId != null && channel.channelId.startsWith("UC") ? channel.channelId : null;

        ImageView banner = hero.findViewById(R.id.nova_channel_banner);
        ImageView avatar = hero.findViewById(R.id.nova_channel_avatar);
        TextView name = hero.findViewById(R.id.nova_channel_name);
        TextView meta = hero.findViewById(R.id.nova_channel_meta);
        TextView subscribe = hero.findViewById(R.id.nova_channel_subscribe);

        // What we already know, until the full data arrives
        String author = channel != null ? channel.getAuthor() : null;
        name.setText(author);
        meta.setText(channel != null ? channel.subscriberCount : null);
        Glide.with(hero.getContext().getApplicationContext()).clear(banner);
        banner.setImageDrawable(null);
        if (channel != null) {
            NovaAvatars.bind(avatar, channel);
        } else {
            NovaAvatars.unbind(avatar);
            avatar.setImageDrawable(new NovaInitialDrawable(null));
        }

        RxHelper.disposeActions(sLoadAction, sToggleAction);
        initSubscribeButton(subscribe, channel != null && (channel.isSubscribed || channel.belongsToSubscriptions()));
        checkState(root);

        NovaChannelInfo cached = NovaChannelInfo.getCached(channelIdOrHandle);
        if (cached != null) {
            apply(hero, channelIdOrHandle, cached);
            return;
        }

        sLoadAction = RxHelper.execute(NovaChannelInfo.load(channelIdOrHandle),
                info -> apply(hero, channelIdOrHandle, info),
                error -> Log.e(TAG, "Channel header: %s", error.getMessage()));
    }

    /**
     * A new channel is being loaded (the fragment cleared its rows).
     */
    public static void reset() {
        RxHelper.disposeActions(sStateAction);
        sSampleVideoId = null;
        sStateChecked = false;
    }

    /**
     * The channel videos arrived: remember one video, used to read the subscription state.
     * @return true when a video with an id was found
     */
    public static boolean onChannelContent(View root, @Nullable List<Video> videos) {
        if (videos == null) {
            return false;
        }

        for (Video video : videos) {
            if (video != null && !TextUtils.isEmpty(video.videoId)) {
                sSampleVideoId = video.videoId;
                checkState(root);
                return true;
            }
        }

        return false;
    }

    /**
     * Runs once both the header is bound and a channel video is known (they arrive in any order):
     * focuses the Subscribe button and reads the real subscription state of the signed-in account.
     */
    private static void checkState(View root) {
        View hero = root.findViewById(R.id.nova_channel_hero);
        TextView button = root.findViewById(R.id.nova_channel_subscribe);
        if (sStateChecked || sSampleVideoId == null || hero == null || button == null || hero.getTag() == null) {
            return;
        }

        sStateChecked = true;

        // After the rows are laid out (the list selects the first video row on load)
        button.post(button::requestFocus);

        Object key = hero.getTag();
        RxHelper.disposeActions(sStateAction);
        sStateAction = RxHelper.execute(
                YouTubeServiceManager.instance().getMediaItemService().getMetadataObserve(sSampleVideoId),
                metadata -> {
                    if (!key.equals(hero.getTag())) {
                        return;
                    }
                    if (sChannelId == null && metadata.getChannelId() != null) {
                        sChannelId = metadata.getChannelId();
                    }
                    setSubscribed(button, metadata.isSubscribed());
                },
                error -> Log.d(TAG, "Subscription state: %s", error.getMessage()));
    }

    private static void apply(View hero, String key, NovaChannelInfo info) {
        if (!key.equals(hero.getTag()) || isDestroyed(hero.getContext())) {
            return; // another channel was opened meanwhile
        }

        if (info.channelId != null) {
            sChannelId = info.channelId;
        }

        TextView name = hero.findViewById(R.id.nova_channel_name);
        TextView meta = hero.findViewById(R.id.nova_channel_meta);
        ImageView banner = hero.findViewById(R.id.nova_channel_banner);
        ImageView avatar = hero.findViewById(R.id.nova_channel_avatar);

        if (!TextUtils.isEmpty(info.name)) {
            name.setText(info.name);
        }

        StringBuilder line = new StringBuilder();
        if (!TextUtils.isEmpty(info.handle)) {
            line.append(info.handle);
        }
        if (!TextUtils.isEmpty(info.stats)) {
            line.append(line.length() > 0 ? "  ·  " : "").append(info.stats);
        }
        if (line.length() > 0) {
            meta.setText(line);
        }

        Context context = hero.getContext();
        if (!TextUtils.isEmpty(info.bannerUrl)) {
            int radius = context.getResources().getDimensionPixelSize(R.dimen.nova_radius_card);
            Glide.with(context)
                    .load(info.bannerUrl)
                    .apply(ViewUtil.glideOptions())
                    .transform(new CenterCrop(), new RoundedCorners(radius))
                    .into(banner);
        }
        if (!TextUtils.isEmpty(info.avatarUrl)) {
            Glide.with(context)
                    .load(info.avatarUrl)
                    .apply(ViewUtil.glideOptions())
                    .circleCrop()
                    .into(avatar);
        }
    }

    private static void initSubscribeButton(TextView button, boolean subscribed) {
        setSubscribed(button, subscribed);
        button.setVisibility(View.VISIBLE);

        button.setOnClickListener(v -> {
            String channelId = sChannelId;
            if (channelId == null) {
                MessageHelpers.showMessage(v.getContext(), R.string.wait_data_loading);
                return;
            }

            boolean newState = !Boolean.TRUE.equals(button.getTag());
            RxHelper.disposeActions(sToggleAction);
            sToggleAction = RxHelper.execute(newState ?
                    YouTubeServiceManager.instance().getMediaItemService().subscribeObserve(channelId) :
                    YouTubeServiceManager.instance().getMediaItemService().unsubscribeObserve(channelId));
            if (sChannel != null) {
                sChannel.isSubscribed = newState;
            }
            setSubscribed(button, newState);
            MessageHelpers.showMessage(v.getContext(),
                    newState ? R.string.subscribed_to_channel : R.string.unsubscribed_from_channel);
        });
    }

    private static void setSubscribed(TextView button, boolean subscribed) {
        Context context = button.getContext();
        button.setTag(subscribed);
        button.setText(subscribed ? R.string.novatv_channel_unsubscribe : R.string.novatv_channel_subscribe);
        // Not subscribed = white pill (main action), subscribed = dark pill
        button.setSelected(!subscribed);
        button.setTextColor(ContextCompat.getColor(context, subscribed ? R.color.nova_text_primary : R.color.nova_text_on_light));
    }

    private static boolean isDestroyed(Context context) {
        return context instanceof Activity && ((Activity) context).isDestroyed();
    }
}
