/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tweaks;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;

class ControlBar extends LinearLayout {

    public interface OnChildFocusedListener {

        public void onChildFocusedListener(View child, View focused);
    }

    private int mChildMarginFromCenter;
    private OnChildFocusedListener mOnChildFocusedListener;
    // Can't set to static. Because we have two control bars.
    private int mLastFocusIndex = -1;
    // MOD: Sync focus index between rows
    private static int sSharedFocusIndex = -1;
    private boolean mIsSharedFocusEnabled = true;
    private boolean mDefaultFocusToMiddle = true;
    private boolean mFocusRecovery = true;
    private int mMinChildGap; // NovaTV

    public ControlBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        resetFocus(); // MOD: reset global focus index
        initNovaStyle();
    }

    public ControlBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        resetFocus(); // MOD: reset global focus index
        initNovaStyle();
    }

    /**
     * NovaTV: the focused button widens to show its name. Keep a minimum gap so it pushes
     * its neighbours instead of overlapping them.<br/>
     * No LayoutTransition here: while animating, a button can briefly get a zero size and Android
     * then drops its focus (it lands on the seek bar and the next presses seek the video).
     */
    private void initNovaStyle() {
        mMinChildGap = getResources().getDimensionPixelSize(com.liskovsoft.smartyoutubetv2.tv.R.dimen.nova_player_button_min_gap);
    }

    void setDefaultFocusToMiddle(boolean defaultFocusToMiddle) {
        mDefaultFocusToMiddle = defaultFocusToMiddle;
    }

    /**
     * MOD: enable/disable focus restoration
     */
    void setFocusRecovery(boolean focusRecovery) {
        mFocusRecovery = focusRecovery;
    }

    /**
     * MOD: synced navigation between rows
     */
    void setSharedFocusEnabled(boolean enabled) {
        mIsSharedFocusEnabled = enabled;
    }

    void resetFocus() {
        setFocusIndex(-1);
    }

    int getDefaultFocusIndex() {
        return mDefaultFocusToMiddle ? getChildCount() / 2 : 0;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (getChildCount() > 0) {
            // MOD: fix situation when rows have different length
            if (getFocusIndex() >= getChildCount()) {
                setFocusIndex(getChildCount() - 1);
            }

            int index = getFocusIndex() >= 0 && getFocusIndex() < getChildCount()
                    ? getFocusIndex() : getDefaultFocusIndex();

            if (getChildAt(index).requestFocus(direction, previouslyFocusedRect)) {
                return true;
            }
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if ((direction == ViewGroup.FOCUS_UP || direction == ViewGroup.FOCUS_DOWN)) {
            if (getFocusIndex() >= 0 && getFocusIndex() < getChildCount()) {
                views.add(getChildAt(getFocusIndex()));
            } else if (getChildCount() > 0) {
                views.add(getChildAt(getDefaultFocusIndex()));
            }
        } else {
            super.addFocusables(views, direction, focusableMode);
        }
    }

    /**
     * NovaTV: left/right move one button at a time and stop at the first/last button of the row.<br/>
     * The default (geometric) search may pick the seek bar below, especially while a focused button
     * is widening to show its name; then the next presses seek the video.
     */
    @Override
    public View focusSearch(View focused, int direction) {
        if ((direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) && focused != null) {
            int index = indexOfChildContaining(focused);

            if (index >= 0) {
                boolean isRtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
                boolean forward = (direction == View.FOCUS_RIGHT) != isRtl;

                for (int next = forward ? index + 1 : index - 1; next >= 0 && next < getChildCount(); next += forward ? 1 : -1) {
                    View child = getChildAt(next);
                    if (child.getVisibility() != View.VISIBLE) {
                        continue;
                    }
                    ArrayList<View> focusables = new ArrayList<>();
                    child.addFocusables(focusables, direction);
                    if (!focusables.isEmpty()) {
                        return focusables.get(0);
                    }
                }

                return focused; // edge of the row
            }
        }

        return super.focusSearch(focused, direction);
    }

    private int indexOfChildContaining(View view) {
        while (view != null && view.getParent() != this) {
            view = view.getParent() instanceof View ? (View) view.getParent() : null;
        }

        return view != null ? indexOfChild(view) : -1;
    }

    public void setOnChildFocusedListener(OnChildFocusedListener listener) {
        mOnChildFocusedListener = listener;
    }

    public void setChildMarginFromCenter(int marginFromCenter) {
        mChildMarginFromCenter = marginFromCenter;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (mFocusRecovery) {
            setFocusIndex(indexOfChild(child));
        }
        if (mOnChildFocusedListener != null) {
            mOnChildFocusedListener.onChildFocusedListener(child, focused);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mChildMarginFromCenter <= 0) {
            return;
        }

        int totalExtraMargin = 0;
        for (int i = 0; i < getChildCount() - 1; i++) {
            View first = getChildAt(i);
            View second = getChildAt(i+1);
            int measuredWidth = first.getMeasuredWidth() + second.getMeasuredWidth();
            int marginStart = Math.max(mChildMarginFromCenter - measuredWidth / 2, mMinChildGap); // NovaTV: min gap
            LayoutParams lp = (LayoutParams) second.getLayoutParams();
            int extraMargin = marginStart - lp.getMarginStart();
            lp.setMarginStart(marginStart);
            second.setLayoutParams(lp);
            totalExtraMargin += extraMargin;
        }
        setMeasuredDimension(getMeasuredWidth() + totalExtraMargin, getMeasuredHeight());
    }

    private void setFocusIndex(int index) {
        mLastFocusIndex = index;
        if (index != -1) // don't reset global focus between player launches
            sSharedFocusIndex = index;
    }

    private int getFocusIndex() {
        return mIsSharedFocusEnabled ? sSharedFocusIndex : mLastFocusIndex;
    }
}
