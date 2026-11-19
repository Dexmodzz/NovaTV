package com.liskovsoft.smartyoutubetv2.common.utils;

import android.text.TextUtils;
import android.util.LruCache;

import androidx.annotation.Nullable;

import com.liskovsoft.sharedutils.okhttp.OkHttpManager;
import com.liskovsoft.sharedutils.rx.RxHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * NovaTV: channel page header data (banner, avatar, name, handle, subscribers, video count).<br/>
 * The core library does not expose the channel banner, so the public channel page data is read here
 * (anonymous request, texts in the device language). Results are cached per channel.
 */
public final class NovaChannelInfo {
    private static final String BASE = "https://www.youtube.com/youtubei/v1/";
    private static final String CLIENT_VERSION = "2.20251101.00.00";
    private static final MediaType JSON = MediaType.parse("application/json");
    private static final LruCache<String, NovaChannelInfo> sCache = new LruCache<>(30);

    /** Channel id (UC...), always resolved */
    public String channelId;
    public String name;
    public String handle;
    /** "151K subscribers · 58 videos" (localized by the server) */
    public String stats;
    public String avatarUrl;
    public String bannerUrl;

    private NovaChannelInfo() {
    }

    @Nullable
    public static NovaChannelInfo getCached(@Nullable String channelIdOrHandle) {
        return channelIdOrHandle != null ? sCache.get(channelIdOrHandle) : null;
    }

    /**
     * @param channelIdOrHandle "UC..." id or "@handle"
     */
    public static Observable<NovaChannelInfo> load(String channelIdOrHandle) {
        // Background thread, result on the main thread
        return RxHelper.fromCallable(() -> {
            NovaChannelInfo cached = sCache.get(channelIdOrHandle);
            if (cached != null) {
                return cached;
            }

            String channelId = channelIdOrHandle;
            if (channelIdOrHandle.startsWith("@")) {
                channelId = resolveHandle(channelIdOrHandle);
            }

            NovaChannelInfo info = parse(post("browse", "\"browseId\":" + JSONObject.quote(channelId)));
            info.channelId = channelId;
            sCache.put(channelIdOrHandle, info);
            sCache.put(channelId, info);
            return info;
        });
    }

    public static boolean isSupportedId(@Nullable String channelIdOrHandle) {
        return channelIdOrHandle != null && (channelIdOrHandle.startsWith("UC") || channelIdOrHandle.startsWith("@"));
    }

    private static String resolveHandle(String handle) throws IOException, JSONException {
        JSONObject result = post("navigation/resolve_url",
                "\"url\":" + JSONObject.quote("https://www.youtube.com/" + handle));
        String id = result.getJSONObject("endpoint").getJSONObject("browseEndpoint").getString("browseId");
        if (TextUtils.isEmpty(id)) {
            throw new IOException("Can't resolve " + handle);
        }
        return id;
    }

    private static JSONObject post(String endpoint, String extra) throws IOException, JSONException {
        Locale locale = Locale.getDefault();
        String body = "{\"context\":{\"client\":{\"clientName\":\"WEB\",\"clientVersion\":\"" + CLIENT_VERSION + "\"," +
                "\"hl\":" + JSONObject.quote(locale.getLanguage()) +
                (TextUtils.isEmpty(locale.getCountry()) ? "" : ",\"gl\":" + JSONObject.quote(locale.getCountry())) +
                "}}," + extra + "}";

        Request request = new Request.Builder()
                .url(BASE + endpoint + "?prettyPrint=false")
                .post(RequestBody.create(JSON, body))
                .build();

        try (Response response = OkHttpManager.instance().getClient().newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (!response.isSuccessful() || responseBody == null) {
                throw new IOException("HTTP " + response.code());
            }
            return new JSONObject(responseBody.string());
        }
    }

    private static NovaChannelInfo parse(JSONObject root) throws JSONException {
        JSONObject header = root.getJSONObject("header").getJSONObject("pageHeaderRenderer")
                .getJSONObject("content").getJSONObject("pageHeaderViewModel");
        NovaChannelInfo info = new NovaChannelInfo();

        JSONObject title = header.optJSONObject("title");
        if (title != null) {
            info.name = title.optJSONObject("dynamicTextViewModel") != null ?
                    title.getJSONObject("dynamicTextViewModel").getJSONObject("text").optString("content", null) : null;
        }
        if (TextUtils.isEmpty(info.name)) {
            info.name = root.getJSONObject("header").getJSONObject("pageHeaderRenderer").optString("pageTitle", null);
        }

        info.avatarUrl = largestSource(header.optJSONObject("image"),
                "decoratedAvatarViewModel", "avatar", "avatarViewModel", "image");
        info.bannerUrl = largestSource(header.optJSONObject("banner"), "imageBannerViewModel", "image");

        List<String> parts = new ArrayList<>();
        JSONObject metadata = header.optJSONObject("metadata");
        JSONArray rows = metadata != null && metadata.optJSONObject("contentMetadataViewModel") != null ?
                metadata.getJSONObject("contentMetadataViewModel").optJSONArray("metadataRows") : null;
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONArray rowParts = rows.getJSONObject(i).optJSONArray("metadataParts");
            for (int j = 0; rowParts != null && j < rowParts.length(); j++) {
                JSONObject text = rowParts.getJSONObject(j).optJSONObject("text");
                String content = text != null ? text.optString("content", null) : null;
                if (TextUtils.isEmpty(content)) {
                    continue;
                }
                if (content.startsWith("@") && info.handle == null) {
                    info.handle = content;
                } else {
                    parts.add(content);
                }
            }
        }
        info.stats = TextUtils.join("  ·  ", parts);

        return info;
    }

    @Nullable
    private static String largestSource(@Nullable JSONObject parent, String... path) {
        JSONObject node = parent;
        for (String key : path) {
            if (node == null) {
                return null;
            }
            node = node.optJSONObject(key);
        }

        JSONArray sources = node != null ? node.optJSONArray("sources") : null;
        if (sources == null || sources.length() == 0) {
            return null;
        }

        String url = sources.optJSONObject(sources.length() - 1).optString("url", null);
        if (url != null && url.startsWith("//")) {
            url = "https:" + url;
        }
        return url;
    }
}
