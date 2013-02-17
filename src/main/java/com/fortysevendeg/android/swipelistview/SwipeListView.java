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

/**
 * ListView subclass that provides the swipe functionality
 */
public class SwipeListView extends ListView {

    /**
     * Disables all swipes
     */
    public final static int SWIPE_MODE_NONE = 0;

    /**
     * Enables both left and right swipe
     */
    public final static int SWIPE_MODE_BOTH = 1;

    /**
     * Enables right swipe
     */
    public final static int SWIPE_MODE_RIGHT = 2;

    /**
     * Enables left swipe
     */
    public final static int SWIPE_MODE_LEFT = 3;

    /**
     * Binds the swipe gesture to reveal a view behind the row (Drawer style)
     */
    public final static int SWIPE_ACTION_REVEAL = 0;

    /**
     * Dismisses the cell when swiped over
     */
    public final static int SWIPE_ACTION_DISMISS = 1;

    /**
     * Marks the cell as checked when swiped and release
     */
    public final static int SWIPE_ACTION_CHECK = 2;

    /**
     * No action when swiped
     */
    public final static int SWIPE_ACTION_NONE = 3;

    /**
     * Indicates no movement
     */
    private final static int TOUCH_STATE_REST = 0;

    /**
     * State scrolling x position
     */
    private final static int TOUCH_STATE_SCROLLING_X = 1;

    /**
     * State scrolling y position
     */
    private final static int TOUCH_STATE_SCROLLING_Y = 2;

    private int touchState = TOUCH_STATE_REST;

    private float lastMotionX;
    private float lastMotionY;
    private int touchSlop;

    /**
     * Internal listener for common swipe events
     */
    private SwipeListViewListener swipeListViewListener;

    /**
     * Internal touch listener
     */
    private SwipeListViewTouchListener touchListener;

    /**
     * @see ListView#ListView(android.content.Context)
     */
    public SwipeListView(Context context) {
        super(context);
        init(null);
    }

    /**
     * @see ListView#ListView(android.content.Context, android.util.AttributeSet)
     */
    public SwipeListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    /**
     * @see ListView#ListView(android.content.Context, android.util.AttributeSet, int)
     */
    public SwipeListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    /**
     * Init ListView
     *
     * @param attrs AttributeSet
     */
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
        touchListener.setRightOffset(swipeOffsetRight);
        touchListener.setLeftOffset(swipeOffsetLeft);
        touchListener.setSwipeActionLeft(swipeActionLeft);
        touchListener.setSwipeActionRight(swipeActionRight);
        touchListener.setSwipeMode(swipeMode);
        touchListener.setSwipeClosesAllItemsWhenListMoves(swipeCloseAllItemsWhenMoveList);
        touchListener.setSwipeOpenOnLongPress(swipeOpenOnLongPress);
        setOnTouchListener(touchListener);
        setOnScrollListener(touchListener.makeScrollListener());
    }

    /**
     * @see ListView#setAdapter(android.widget.ListAdapter)
     */
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

    /**
     * Open ListView's item
     *
     * @param position Position that you want open
     */
    public void openAnimate(int position) {
        touchListener.openAnimate(position);
    }

    /**
     * Close ListView's item
     *
     * @param position Position that you want open
     */
    public void closeAnimate(int position) {
        touchListener.closeAnimate(position);
    }

    /**
     * Notifies onDismiss
     *
     * @param reverseSortedPositions All dismissed positions
     */
    protected void onDismiss(int[] reverseSortedPositions) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onDismiss(reverseSortedPositions);
        }
    }

    /**
     * Notifies onClickFrontView
     *
     * @param position item clicked
     */
    protected void onClickFrontView(int position) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onClickFrontView(position);
        }
    }

    /**
     * Notifies onClickBackView
     *
     * @param position back item clicked
     */
    protected void onClickBackView(int position) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onClickBackView(position);
        }
    }

    /**
     * Notifies onOpened
     *
     * @param position Item opened
     * @param toRight  If should be opened toward the right
     */
    protected void onOpened(int position, boolean toRight) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onOpened(position, toRight);
        }
    }

    /**
     * Notifies onClosed
     *
     * @param position  Item closed
     * @param fromRight If open from right
     */
    protected void onClosed(int position, boolean fromRight) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onClosed(position, fromRight);
        }
    }

    /**
     * Notifies onListChanged
     */
    protected void onListChanged() {
        if (swipeListViewListener != null) {
            swipeListViewListener.onListChanged();
        }
    }

    /**
     * Notifies onMove
     *
     * @param position Item moving
     * @param x        Current position
     */
    protected void onMove(int position, float x) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onMove(position, x);
        }
    }

    /**
     * Sets the Listener
     *
     * @param swipeListViewListener Listener
     */
    public void setSwipeListViewListener(SwipeListViewListener swipeListViewListener) {
        this.swipeListViewListener = swipeListViewListener;
    }

    /**
     * Resets scrolling
     */
    public void resetScrolling() {
        touchState = TOUCH_STATE_REST;
    }

    /**
     * Set offset on right
     *
     * @param offsetRight Offset
     */
    public void setOffsetRight(float offsetRight) {
        touchListener.setRightOffset(offsetRight);
    }

    /**
     * Set offset on left
     *
     * @param offsetLeft Offset
     */
    public void setOffsetLeft(float offsetLeft) {
        touchListener.setLeftOffset(offsetLeft);
    }

    /**
     * Set if all items opened will be closed when the user moves the ListView
     *
     * @param swipeCloseAllItemsWhenMoveList
     */
    public void setSwipeCloseAllItemsWhenMoveList(boolean swipeCloseAllItemsWhenMoveList) {
        touchListener.setSwipeClosesAllItemsWhenListMoves(swipeCloseAllItemsWhenMoveList);
    }

    /**
     * Sets if the user can open an item with long pressing on cell
     *
     * @param swipeOpenOnLongPress
     */
    public void setSwipeOpenOnLongPress(boolean swipeOpenOnLongPress) {
        touchListener.setSwipeOpenOnLongPress(swipeOpenOnLongPress);
    }

    /**
     * Set swipe mode
     *
     * @param swipeMode
     */
    public void setSwipeMode(int swipeMode) {
        touchListener.setSwipeMode(swipeMode);
    }

    /**
     * Return action on left
     *
     * @return Action
     */
    public int getSwipeActionLeft() {
        return touchListener.getSwipeActionLeft();
    }

    /**
     * Set action on left
     *
     * @param swipeActionLeft Action
     */
    public void setSwipeActionLeft(int swipeActionLeft) {
        touchListener.setSwipeActionLeft(swipeActionLeft);
    }

    /**
     * Return action on right
     *
     * @return Action
     */
    public int getSwipeActionRight() {
        return touchListener.getSwipeActionRight();
    }

    /**
     * Set action on right
     *
     * @param swipeActionRight Action
     */
    public void setSwipeActionRight(int swipeActionRight) {
        touchListener.setSwipeActionRight(swipeActionRight);
    }

    /**
     * Sets animation time when user drops cell
     *
     * @param animationTime milliseconds
     */
    public void setAnimationTime(long animationTime) {
        touchListener.setAnimationTime(animationTime);
    }

    /**
     * @see ListView#onInterceptTouchEvent(android.view.MotionEvent)
     */
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
                checkInMoving(x, y);
                return touchState == TOUCH_STATE_SCROLLING_Y;
            case MotionEvent.ACTION_DOWN:
                touchListener.onTouch(this, ev);
                touchState = TOUCH_STATE_REST;
                lastMotionX = x;
                lastMotionY = y;
                return false;
            case MotionEvent.ACTION_CANCEL:
                touchState = TOUCH_STATE_REST;
                break;
            case MotionEvent.ACTION_UP:
                touchListener.onTouch(this, ev);
                return touchState == TOUCH_STATE_SCROLLING_Y;
            default:
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    /**
     * Check if the user is moving the cell
     *
     * @param x Position X
     * @param y Position Y
     */
    private void checkInMoving(float x, float y) {
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
