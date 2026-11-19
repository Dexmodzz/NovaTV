package com.liskovsoft.smartyoutubetv2.tv.ui.channel;

import android.os.Bundle;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.interfaces.VideoGroupPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelView;
import com.liskovsoft.smartyoutubetv2.common.misc.CrashRestorer;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.presenter.ChannelHeaderPresenter.ChannelHeaderCallback;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.video.MultipleRowsFragment;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.misc.ProgressBarManager;
import com.liskovsoft.googlecommon.common.helpers.YouTubeHelper;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;

public class ChannelFragment extends MultipleRowsFragment implements ChannelView {
    private static final String TAG = ChannelFragment.class.getSimpleName();
    private ChannelPresenter mChannelPresenter;
    private ProgressBarManager mProgressBarManager;
    private boolean mIsFragmentCreated;
    private CrashRestorer mCrashRestorer;
    private boolean mIsNovaHeaderReady;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null); // Real restore takes place in the presenter

        if (getContext() == null) {
            return;
        }

        mCrashRestorer = new CrashRestorer(getContext(), savedInstanceState);
        mIsFragmentCreated = true;
        mChannelPresenter = ChannelPresenter.instance(getContext());
        mChannelPresenter.setView(this);

        mProgressBarManager = new ProgressBarManager();
        // NovaTV: the header row is always there (channel banner/avatar/subscribe), the search bar only if enabled
        boolean searchBarEnabled = MainUIData.instance(getContext()).isChannelSearchBarEnabled();
        {
            addHeader(new ChannelHeaderCallback() {
                @Override
                public String getChannelId() {
                    return mChannelPresenter.getChannelId();
                }

                @Override
                public com.liskovsoft.smartyoutubetv2.common.app.models.data.Video getChannel() {
                    return mChannelPresenter.getChannel();
                }

                @Override
                public boolean isSearchBarEnabled() {
                    return searchBarEnabled;
                }

                @Override
                public void onSearchSettingsClicked() {
                    mChannelPresenter.onSearchSettingsClicked();
                }

                @Override
                public boolean onSearchSubmit(String query) {
                    return mChannelPresenter.onSearchSubmit(query);
                }

                @Override
                public String getChannelTitle() {
                    if (mChannelPresenter.getChannel() == null) {
                        return Helpers.startsWith(mChannelPresenter.getChannelId(), "@") ? mChannelPresenter.getChannelId() : null;
                    }

                    String author = mChannelPresenter.getChannel().getAuthor();
                    String title = mChannelPresenter.getChannel().getTitle();
                    String subs = mChannelPresenter.getChannel().subscriberCount;

                    return Helpers.toString(YouTubeHelper.createInfo(Helpers.firstNonNull(author, title), subs));
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Called when the activity is paused
        mCrashRestorer.persistHeaderIndex(outState, getPosition());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() == null) {
            return;
        }

        // Don't move to onCreateView
        mProgressBarManager.setRootView((ViewGroup) getActivity().findViewById(android.R.id.content).getRootView());

        mChannelPresenter.onViewInitialized();
        
        // Restore state after crash
        mCrashRestorer.restoreHeader((idx, video) -> {
            setPosition(idx);
        });
        mCrashRestorer.restorePlayback();
    }

    @Override
    protected VideoGroupPresenter getMainPresenter() {
        return ChannelPresenter.instance(getContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mChannelPresenter.onViewDestroyed();
    }

    public void onFinish() {
        mChannelPresenter.onFinish();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mIsFragmentCreated) {
            mChannelPresenter.onViewResumed();
        }

        mIsFragmentCreated = false;
    }

    @Override
    public void clear() {
        NovaChannelHero.reset();
        super.clear();
        mIsNovaHeaderReady = false;
    }

    @Override
    public void update(VideoGroup group) {
        super.update(group);

        // NovaTV: one channel video is needed to read the subscription state (then the Subscribe button gets the focus)
        if (!mIsNovaHeaderReady && group != null && getView() != null) {
            mIsNovaHeaderReady = NovaChannelHero.onChannelContent(getView(), group.getVideos());
        }
    }

    @Override
    public void showProgressBar(boolean show) {
        if (show) {
            mProgressBarManager.show();
        } else {
            mProgressBarManager.hide();
        }
    }
}
