/*
 * Copyright (C) 2013 47 Degrees, LLC
 * http://47deg.com
 * hello@47deg.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fortysevendeg.android.swipelistview;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ListAdapter;
import android.widget.ListView;

public class SwipeListView extends ListView {

    public final static int SWIPE_MODE_NONE = 0;
    public final static int SWIPE_MODE_BOTH = 1;
    public final static int SWIPE_MODE_RIGHT = 2;
    public final static int SWIPE_MODE_LEFT = 3;

    public final static int SWIPE_ACTION_REVEAL = 0;
    public final static int SWIPE_ACTION_DISMISS = 1;
    public final static int SWIPE_ACTION_CHECK = 2;
    public final static int SWIPE_ACTION_NONE = 3;

    private final static int TOUCH_STATE_REST = 0;
    private final static int TOUCH_STATE_SCROLLING_X = 1;
    private final static int TOUCH_STATE_SCROLLING_Y = 2;

    private int touchState = TOUCH_STATE_REST;

    private float lastMotionX;
    private float lastMotionY;
    private int touchSlop;

    private SwipeListViewListener swipeListViewListener;

    private SwipeListViewTouchListener touchListener;

    public SwipeListView(Context context) {
        super(context);
        init(null);
    }

    public SwipeListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SwipeListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {

        int swipeMode = SWIPE_MODE_BOTH;
        boolean swipeOpenOnLongPress = true;
        boolean swipeCloseAllItemsWhenMoveList = true;
        int swipeFrontView = 0;
        int swipeBackView = 0;
        long swipeAnimationTime = 0;
        float swipeOffsetLeft = 0;
        float swipeOffsetRight = 0;

        int swipeActionLeft = SWIPE_ACTION_REVEAL;
        int swipeActionRight = SWIPE_ACTION_REVEAL;

        if (attrs != null) {
            TypedArray styled = getContext().obtainStyledAttributes(attrs, R.styleable.SwipeListView);
            swipeMode = styled.getInt(R.styleable.SwipeListView_swipeMode, SWIPE_MODE_BOTH);
            swipeActionLeft = styled.getInt(R.styleable.SwipeListView_swipeActionLeft, SWIPE_ACTION_REVEAL);
            swipeActionRight = styled.getInt(R.styleable.SwipeListView_swipeActionRight, SWIPE_ACTION_REVEAL);
            swipeOffsetLeft = styled.getDimension(R.styleable.SwipeListView_swipeOffsetLeft, 0);
            swipeOffsetRight = styled.getDimension(R.styleable.SwipeListView_swipeOffsetRight, 0);
            swipeOpenOnLongPress = styled.getBoolean(R.styleable.SwipeListView_swipeOpenOnLongPress, true);
            swipeAnimationTime = styled.getInteger(R.styleable.SwipeListView_swipeAnimationTime, 0);
            swipeCloseAllItemsWhenMoveList = styled.getBoolean(R.styleable.SwipeListView_swipeCloseAllItemsWhenMoveList, true);
            swipeFrontView = styled.getResourceId(R.styleable.SwipeListView_swipeFrontView, 0);
            swipeBackView = styled.getResourceId(R.styleable.SwipeListView_swipeBackView, 0);
        }

        if (swipeFrontView == 0 || swipeBackView == 0) {
            throw new RuntimeException("Missed attribute swipeFrontView or swipeBackView");
        }

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        touchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        touchListener = new SwipeListViewTouchListener(this, swipeFrontView, swipeBackView);
        if (swipeAnimationTime > 0) {
            touchListener.setAnimationTime(swipeAnimationTime);
        }
        touchListener.setOffsetRight(swipeOffsetRight);
        touchListener.setOffsetLeft(swipeOffsetLeft);
        touchListener.setSwipeActionLeft(swipeActionLeft);
        touchListener.setSwipeActionRight(swipeActionRight);
        touchListener.setSwipeMode(swipeMode);
        touchListener.setSwipeCloseAllItemsWhenMoveList(swipeCloseAllItemsWhenMoveList);
        touchListener.setSwipeOpenOnLongPress(swipeOpenOnLongPress);
        setOnTouchListener(touchListener);
        setOnScrollListener(touchListener.makeScrollListener());
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        touchListener.resetItems();
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                onListChanged();
                touchListener.resetItems();
            }
        });
    }

    public void openAnimate(int position) {
        touchListener.openAnimate(position);
    }

    public void closeAnimate(int position) {
        touchListener.closeAnimate(position);
    }

    protected void onDismiss(int[] reverseSortedPositions) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onDismiss(reverseSortedPositions);
        }
    }

    protected void onClickFrontView(int position) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onClickFrontView(position);
        }
    }

    protected void onClickBackView(int position) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onClickBackView(position);
        }
    }

    protected void onOpened(int position, boolean toRight) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onOpened(position, toRight);
        }
    }

    protected void onClosed(int position, boolean fromRight) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onClosed(position, fromRight);
        }
    }

    protected void onListChanged() {
        if (swipeListViewListener != null) {
            swipeListViewListener.onListChanged();
        }
    }

    protected void onMove(int position, float x) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onMove(position, x);
        }
    }

    public void setSwipeListViewListener(SwipeListViewListener swipeListViewListener) {
        this.swipeListViewListener = swipeListViewListener;
    }

    public void resetScrolling() {
        touchState = TOUCH_STATE_REST;
    }

    public void setOffsetRight(float offsetRight) {
        touchListener.setOffsetRight(offsetRight);
    }

    public void setOffsetLeft(float offsetLeft) {
        touchListener.setOffsetLeft(offsetLeft);
    }

    public void setSwipeCloseAllItemsWhenMoveList(boolean swipeCloseAllItemsWhenMoveList) {
        touchListener.setSwipeCloseAllItemsWhenMoveList(swipeCloseAllItemsWhenMoveList);
    }

    public void setSwipeOpenOnLongPress(boolean swipeOpenOnLongPress) {
        touchListener.setSwipeOpenOnLongPress(swipeOpenOnLongPress);
    }

    public void setSwipeMode(int swipeMode) {
        touchListener.setSwipeMode(swipeMode);
    }

    public float getSwipeActionLeft() {
        return touchListener.getSwipeActionLeft();
    }

    public void setSwipeActionLeft(int swipeActionLeft) {
        touchListener.setSwipeActionLeft(swipeActionLeft);
    }

    public float getSwipeActionRight() {
        return touchListener.getSwipeActionRight();
    }

    public void setSwipeActionRight(int swipeActionRight) {
        touchListener.setSwipeActionRight(swipeActionRight);
    }

    public void setAnimationTime(long animationTime) {
        touchListener.setAnimationTime(animationTime);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = MotionEventCompat.getActionMasked(ev);
        final float x = ev.getX();
        final float y = ev.getY();

        if (touchState == TOUCH_STATE_SCROLLING_X) {
            return touchListener.onTouch(this, ev);
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                checkInMovingX(x, y);
                return touchState==TOUCH_STATE_SCROLLING_Y;
            case MotionEvent.ACTION_DOWN:
                touchListener.onTouch(this, ev);
                lastMotionX = x;
                lastMotionY = y;
                return false;
            case MotionEvent.ACTION_CANCEL:
                touchState = TOUCH_STATE_REST;
                break;
            case MotionEvent.ACTION_UP:
                touchListener.onTouch(this, ev);
                touchState = TOUCH_STATE_REST;
                return touchState==TOUCH_STATE_SCROLLING_Y;
            default:
                break;
        }

        return true;
    }

    private void checkInMovingX(float x, float y) {
        final int xDiff = (int) Math.abs(x - lastMotionX);
        final int yDiff = (int) Math.abs(y - lastMotionY);

        final int touchSlop = this.touchSlop;
        boolean xMoved = xDiff > touchSlop;
        boolean yMoved = yDiff > touchSlop;

        if (xMoved) {
            touchState = TOUCH_STATE_SCROLLING_X;
            lastMotionX = x;
            lastMotionY = y;
        }

        if (yMoved) {
            touchState = TOUCH_STATE_SCROLLING_Y;
            lastMotionX = x;
            lastMotionY = y;
        }
    }

}
