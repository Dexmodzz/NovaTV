package com.liskovsoft.smartyoutubetv2.tv.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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
 * 2) channel cards on screen (e.g. the channel in search results) carry the avatar: reused for its videos<br/>
 * 3) otherwise the video metadata (owner avatar), max {@link #MAX_IN_FLIGHT} requests at a time,
 * newest cards first, only for cards that are actually bound on screen.<br/>
 * Channels are cached by id and by name (some lists have only the name) and saved on the device,
 * so known avatars show at once, also after a restart.<br/>
 * Until the avatar is known a colored initial is shown.
 */
public final class NovaAvatars {
    private static final String TAG = NovaAvatars.class.getSimpleName();
    private static final int MAX_IN_FLIGHT = 4;
    private static final int MAX_QUEUE = 24;
    private static final String NO_AVATAR = "";
    private static final LruCache<String, String> sCache = new LruCache<>(2000);
    private static final String PREFS_NAME = "novatv_avatars_v2";
    /** Test builds only, may hold playlist covers: emptied */
    private static final String OLD_PREFS_NAME = "novatv_avatars";
    private static final int MAX_STORED = 3000;
    @Nullable
    private static SharedPreferences sPrefs;
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

        initStore(view.getContext());
        prefillIfNeeded();
        rememberChannelCard(video);

        String url = getCached(video);

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

    /**
     * Any list item shown on screen: a channel card carries the channel avatar, reused for its videos.
     */
    public static void onItemBound(@Nullable Video video) {
        rememberChannelCard(video);
    }

    private static void rememberChannelCard(@Nullable Video video) {
        // Real channel items only: a playlist shown as a channel carries the playlist cover
        if (video != null && video.isChannel() && video.itemType == MediaItem.TYPE_CHANNEL
                && !TextUtils.isEmpty(video.cardImageUrl)) {
            SharedPreferences.Editor editor = edit();
            put(video.channelId, video.getAuthor() != null ? video.getAuthor() : video.getTitle(), video.cardImageUrl, editor);
            apply(editor);
        }
    }

    @Nullable
    private static String getCached(Video video) {
        String url = !TextUtils.isEmpty(video.channelId) ? sCache.get(video.channelId) : null;

        if (TextUtils.isEmpty(url) && !TextUtils.isEmpty(video.getAuthor())) {
            String byName = sCache.get("author:" + video.getAuthor());
            if (byName != null && (url == null || !byName.isEmpty())) {
                url = byName;
            }
        }

        return url;
    }

    /**
     * Cache an avatar under the channel id and the channel name, show it in the cards waiting for it
     * and add it to the editor (saved on the device) when it changed.
     */
    private static void put(@Nullable String channelId, @Nullable String name, @Nullable String url,
                            @Nullable SharedPreferences.Editor editor) {
        boolean known = !TextUtils.isEmpty(url);
        String value = known ? url : NO_AVATAR;

        for (String key : new String[] {
                TextUtils.isEmpty(channelId) ? null : channelId,
                TextUtils.isEmpty(name) ? null : "author:" + name}) {
            if (key == null) {
                continue;
            }

            if (known && editor != null && !url.equals(sCache.get(key))) {
                editor.putString(key, url);
            }

            sCache.put(key, value);
            deliver(key, url);
        }
    }

    @Nullable
    private static SharedPreferences.Editor edit() {
        return sPrefs != null ? sPrefs.edit() : null;
    }

    private static void apply(@Nullable SharedPreferences.Editor editor) {
        if (editor != null) {
            editor.apply();
        }
    }

    private static void initStore(Context context) {
        if (sPrefs != null) {
            return;
        }

        context.getApplicationContext().getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        sPrefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> stored = sPrefs.getAll();

        if (stored.size() > MAX_STORED) {
            sPrefs.edit().clear().apply(); // avatars are refreshed as they are seen again
            return;
        }

        for (Map.Entry<String, ?> entry : stored.entrySet()) {
            if (entry.getValue() instanceof String) {
                sCache.put(entry.getKey(), (String) entry.getValue());
            }
        }
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
            boolean[] done = {false};
            Runnable onDone = () -> {
                // Called twice (onResult + onComplete): count the request only once
                if (!done[0]) {
                    done[0] = true;
                    sInFlight = Math.max(0, sInFlight - 1);
                    sWaiting.remove(key); // no avatar found: keep the initial
                    drain();
                }
            };
            RxHelper.execute(
                    YouTubeServiceManager.instance().getMediaItemService().getMetadataObserve(video.videoId),
                    metadata -> {
                        String url = metadata.getAuthorImageUrl();
                        sCache.put(key, TextUtils.isEmpty(url) ? NO_AVATAR : url);
                        SharedPreferences.Editor editor = edit();
                        put(video.channelId, video.getAuthor(), url, editor);
                        deliver(key, url);
                        apply(editor);
                        onDone.run();
                    },
                    error -> {
                        Log.e(TAG, "Avatar lookup failed: %s", error.getMessage());
                        onDone.run();
                    },
                    onDone
            );
        }
    }

    /**
     * Show the avatar in the cards waiting for that channel.
     */
    private static void deliver(String key, @Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        List<WeakReference<ImageView>> views = sWaiting.remove(key);

        if (views == null) {
            return;
        }

        for (WeakReference<ImageView> ref : views) {
            ImageView view = ref.get();
            if (view != null && key.equals(view.getTag())) {
                load(view, url, null);
            }
        }
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

        // The items often have no channel id: the channel name is the key then
        SharedPreferences.Editor editor = edit();
        for (MediaItem item : group.getMediaItems()) {
            if (!TextUtils.isEmpty(item.getCardImageUrl())) {
                put(item.getChannelId(), item.getTitle(), item.getCardImageUrl(), editor);
            }
        }

        apply(editor);
    }
}
