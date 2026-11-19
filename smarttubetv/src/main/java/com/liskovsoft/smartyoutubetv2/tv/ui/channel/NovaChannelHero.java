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
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
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

import io.reactivex.disposables.Disposable;

/**
 * NovaTV: fills the channel page header (layout novatv_channel_hero.xml):
 * banner, avatar, name, @handle · subscribers · videos and the subscribe/unsubscribe button.
 */
public final class NovaChannelHero {
    private static final String TAG = NovaChannelHero.class.getSimpleName();
    private static Disposable sLoadAction;
    private static Disposable sSubscriptionsAction;
    private static Disposable sToggleAction;

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
            return; // already bound (the header row is rebound on every clear())
        }

        hero.setTag(channelIdOrHandle);
        hero.setVisibility(View.VISIBLE);

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
        subscribe.setVisibility(View.GONE);

        RxHelper.disposeActions(sLoadAction, sSubscriptionsAction, sToggleAction);

        NovaChannelInfo cached = NovaChannelInfo.getCached(channelIdOrHandle);
        if (cached != null) {
            apply(hero, channelIdOrHandle, cached, channel);
            return;
        }

        sLoadAction = RxHelper.execute(NovaChannelInfo.load(channelIdOrHandle),
                info -> apply(hero, channelIdOrHandle, info, channel),
                error -> Log.e(TAG, "Channel header: %s", error.getMessage()));
    }

    private static void apply(View hero, String key, NovaChannelInfo info, @Nullable Video channel) {
        if (!key.equals(hero.getTag()) || isDestroyed(hero.getContext())) {
            return; // another channel was opened meanwhile
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

        initSubscribeButton(hero, key, info.channelId, channel);
    }

    private static void initSubscribeButton(View hero, String key, String channelId, @Nullable Video channel) {
        TextView button = hero.findViewById(R.id.nova_channel_subscribe);
        boolean subscribed = channel != null && (channel.isSubscribed || channel.belongsToSubscriptions());
        setSubscribed(button, subscribed);
        button.setVisibility(View.VISIBLE);

        button.setOnClickListener(v -> {
            boolean newState = !Boolean.TRUE.equals(button.getTag());
            RxHelper.disposeActions(sToggleAction);
            sToggleAction = RxHelper.execute(newState ?
                    YouTubeServiceManager.instance().getMediaItemService().subscribeObserve(channelId) :
                    YouTubeServiceManager.instance().getMediaItemService().unsubscribeObserve(channelId));
            if (channel != null) {
                channel.isSubscribed = newState;
            }
            setSubscribed(button, newState);
            MessageHelpers.showMessage(v.getContext(),
                    newState ? R.string.subscribed_to_channel : R.string.unsubscribed_from_channel);
        });

        if (subscribed) {
            return;
        }

        // The channel item doesn't always know: look it up in the subscriptions list
        sSubscriptionsAction = RxHelper.execute(
                YouTubeServiceManager.instance().getContentService().getSubscribedChannelsObserve(),
                group -> {
                    if (key.equals(hero.getTag()) && contains(group, channelId)) {
                        setSubscribed(button, true);
                    }
                },
                error -> Log.d(TAG, "Subscriptions lookup: %s", error.getMessage()));
    }

    private static boolean contains(@Nullable MediaGroup group, String channelId) {
        if (group == null || group.getMediaItems() == null) {
            return false;
        }

        for (MediaItem item : group.getMediaItems()) {
            if (channelId.equals(item.getChannelId())) {
                return true;
            }
        }

        return false;
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
