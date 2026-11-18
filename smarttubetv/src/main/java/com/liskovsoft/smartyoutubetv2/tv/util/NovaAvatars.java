package com.liskovsoft.smartyoutubetv2.tv.util;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * NovaTV: real channel avatars for video cards.<br/>
 * Video items carry no avatar, so it is looked up once per channel and cached:<br/>
 * 1) prefill from the subscribed channels list (one request, includes avatars)<br/>
 * 2) otherwise the video metadata (owner avatar), max {@link #MAX_IN_FLIGHT} requests at a time,
 * newest cards first, only for cards that are actually bound on screen.<br/>
 * Until the avatar is known a colored initial is shown.
 */
public final class NovaAvatars {
    private static final String TAG = NovaAvatars.class.getSimpleName();
    private static final int MAX_IN_FLIGHT = 2;
    private static final int MAX_QUEUE = 24;
    private static final String NO_AVATAR = "";
    private static final LruCache<String, String> sCache = new LruCache<>(600);
    // channel key -> views waiting for it
    private static final Map<String, List<WeakReference<ImageView>>> sWaiting = new HashMap<>();
    private static final Deque<Video> sQueue = new ArrayDeque<>();
    private static int sInFlight;
    private static boolean sPrefillStarted;

    private NovaAvatars() {
    }

    /**
     * Show the avatar of the video's channel in the given view (initial placeholder until loaded).
     */
    public static void bind(ImageView view, Video video) {
        if (view == null) {
            return;
        }

        String key = getKey(video);
        String name = video != null ? video.getAuthor() : null;
        view.setTag(key);

        if (key == null) {
            showPlaceholder(view, name);
            return;
        }

        prefillIfNeeded();

        String url = sCache.get(key);

        if (url != null) {
            if (url.equals(NO_AVATAR)) {
                showPlaceholder(view, name);
            } else {
                load(view, url, name);
            }
            return;
        }

        showPlaceholder(view, name);
        enqueue(key, video, view);
    }

    public static void unbind(ImageView view) {
        if (view == null) {
            return;
        }

        view.setTag(null);
        Context context = view.getContext().getApplicationContext();
        Glide.with(context).clear(view);
    }

    @Nullable
    private static String getKey(@Nullable Video video) {
        if (video == null) {
            return null;
        }

        if (!TextUtils.isEmpty(video.channelId)) {
            return video.channelId;
        }

        String author = video.getAuthor();
        return TextUtils.isEmpty(author) ? null : "author:" + author;
    }

    private static void showPlaceholder(ImageView view, @Nullable String name) {
        Glide.with(view.getContext().getApplicationContext()).clear(view);
        view.setImageDrawable(new NovaInitialDrawable(name));
    }

    private static void load(ImageView view, String url, @Nullable String name) {
        Context context = view.getContext();

        if (context instanceof Activity && ((Activity) context).isDestroyed()) {
            return;
        }

        Glide.with(context)
                .load(url)
                .apply(ViewUtil.glideOptions())
                .circleCrop()
                .placeholder(new NovaInitialDrawable(name))
                .error(new NovaInitialDrawable(name))
                .into(view);
    }

    private static void enqueue(String key, Video video, ImageView view) {
        List<WeakReference<ImageView>> views = sWaiting.get(key);
        boolean isNew = views == null;

        if (isNew) {
            views = new ArrayList<>();
            sWaiting.put(key, views);
        }

        views.add(new WeakReference<>(view));

        if (!isNew || TextUtils.isEmpty(video.videoId)) {
            if (TextUtils.isEmpty(video.videoId) && isNew) {
                sWaiting.remove(key);
            }
            return;
        }

        sQueue.addFirst(video); // newest (just bound) first

        while (sQueue.size() > MAX_QUEUE) {
            Video dropped = sQueue.pollLast();
            sWaiting.remove(getKey(dropped)); // will be requested again when bound again
        }

        drain();
    }

    private static void drain() {
        while (sInFlight < MAX_IN_FLIGHT && !sQueue.isEmpty()) {
            Video video = sQueue.pollFirst();
            String key = getKey(video);

            if (key == null || !hasLiveViews(key)) {
                sWaiting.remove(key);
                continue; // card scrolled away
            }

            sInFlight++;
            RxHelper.execute(
                    YouTubeServiceManager.instance().getMediaItemService().getMetadataObserve(video.videoId),
                    metadata -> onAvatar(key, metadata.getAuthorImageUrl()),
                    error -> {
                        Log.e(TAG, "Avatar lookup failed: %s", error.getMessage());
                        finish(key, null);
                    },
                    () -> finish(key, null)
            );
        }
    }

    private static void onAvatar(String key, @Nullable String url) {
        sCache.put(key, TextUtils.isEmpty(url) ? NO_AVATAR : url);
        finish(key, url);
    }

    private static void finish(String key, @Nullable String url) {
        List<WeakReference<ImageView>> views = sWaiting.remove(key);

        if (views != null) {
            // Called twice (onResult + onComplete): count the request only once
            sInFlight = Math.max(0, sInFlight - 1);

            if (url != null) {
                for (WeakReference<ImageView> ref : views) {
                    ImageView view = ref.get();
                    if (view != null && key.equals(view.getTag())) {
                        load(view, url, null);
                    }
                }
            }
        }

        drain();
    }

    private static boolean hasLiveViews(String key) {
        List<WeakReference<ImageView>> views = sWaiting.get(key);

        if (views == null) {
            return false;
        }

        for (Iterator<WeakReference<ImageView>> it = views.iterator(); it.hasNext(); ) {
            ImageView view = it.next().get();
            if (view == null || !key.equals(view.getTag())) {
                it.remove();
            }
        }

        return !views.isEmpty();
    }

    /**
     * One request: subscribed channels come with their avatars.
     */
    private static void prefillIfNeeded() {
        if (sPrefillStarted) {
            return;
        }

        sPrefillStarted = true;

        RxHelper.execute(
                YouTubeServiceManager.instance().getContentService().getSubscribedChannelsObserve(),
                NovaAvatars::onSubscriptions,
                error -> Log.d(TAG, "No subscriptions for avatars: %s", error.getMessage())
        );
    }

    private static void onSubscriptions(MediaGroup group) {
        if (group == null || group.getMediaItems() == null) {
            return;
        }

        for (MediaItem item : group.getMediaItems()) {
            if (!TextUtils.isEmpty(item.getChannelId()) && !TextUtils.isEmpty(item.getCardImageUrl())) {
                sCache.put(item.getChannelId(), item.getCardImageUrl());
            }
        }
    }
}
