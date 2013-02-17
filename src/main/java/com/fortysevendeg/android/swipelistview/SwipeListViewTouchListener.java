/*
 * Copyright (C) 2013 47 Degrees, LLC
 * http://47deg.com
 * hello@47deg.com
 *
 * Copyright 2012 Roman Nurik
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

import android.graphics.Rect;
import android.view.*;
import android.widget.AbsListView;
import android.widget.ListView;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.nineoldandroids.view.ViewHelper.setAlpha;
import static com.nineoldandroids.view.ViewHelper.setTranslationX;
import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

/**
 * Touch listener impl for the SwipeListView
 */
public class SwipeListViewTouchListener implements View.OnTouchListener {

    private int swipeMode = SwipeListView.SWIPE_MODE_BOTH;
    private boolean swipeOpenOnLongPress = true;
    private boolean swipeClosesAllItemsWhenListMoves = true;

    private int swipeFrontView = 0;
    private int swipeBackView = 0;

    // Cached ViewConfiguration and system-wide constant values
    private int slop;
    private int minFlingVelocity;
    private int maxFlingVelocity;
    private long configShortAnimationTime;
    private long animationTime;

    private float leftOffset = 0;
    private float rightOffset = 0;

    // Fixed properties
    private SwipeListView swipeListView;
    private int viewWidth = 1; // 1 and not 0 to prevent dividing by zero

    private List<PendingDismissData> pendingDismisses = new ArrayList<PendingDismissData>();
    private int dismissAnimationRefCount = 0;

    private float downX;
    private boolean swiping;
    private VelocityTracker velocityTracker;
    private int downPosition;
    private View parentView;
    private View frontView;
    private View backView;
    private boolean paused;

    private int swipeCurrentAction = SwipeListView.SWIPE_ACTION_NONE;

    private int swipeActionLeft = SwipeListView.SWIPE_ACTION_REVEAL;
    private int swipeActionRight = SwipeListView.SWIPE_ACTION_REVEAL;

    private List<Boolean> opened = new ArrayList<Boolean>();
    private List<Boolean> openedRight = new ArrayList<Boolean>();
    private boolean listViewMoving;

    /**
     * Constructor
     * @param swipeListView SwipeListView
     * @param swipeFrontView front view Identifier
     * @param swipeBackView back view Identifier
     */
    public SwipeListViewTouchListener(SwipeListView swipeListView, int swipeFrontView, int swipeBackView) {
        this.swipeFrontView = swipeFrontView;
        this.swipeBackView = swipeBackView;
        ViewConfiguration vc = ViewConfiguration.get(swipeListView.getContext());
        slop = vc.getScaledTouchSlop();
        minFlingVelocity = vc.getScaledMinimumFlingVelocity();
        maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        configShortAnimationTime = swipeListView.getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
        animationTime = configShortAnimationTime;
        this.swipeListView = swipeListView;
    }

    /**
     * Sets current item's parent view
     * @param parentView Parent view
     */
    private void setParentView(View parentView) {
        this.parentView = parentView;
    }

    /**
     * Sets current item's front view
     * @param frontView Front view
     */
    private void setFrontView(View frontView) {
        this.frontView = frontView;
        frontView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeListView.onClickFrontView(downPosition);
            }
        });
        if (swipeOpenOnLongPress) {
            frontView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    openAnimate(downPosition);
                    return false;
                }
            });
        }
    }

    /**
     * Set current item's back view
     * @param backView
     */
    private void setBackView(View backView) {
        this.backView = backView;
        backView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeListView.onClickBackView(downPosition);
            }
        });
    }

    /**
     * @return true if the list is in motion
     */
    public boolean isListViewMoving() {
        return listViewMoving;
    }

    /**
     * Sets animation time when the user drops the cell
     *
     * @param animationTime milliseconds
     */
    public void setAnimationTime(long animationTime) {
        if (animationTime > 0) {
            this.animationTime = animationTime;
        } else {
            this.animationTime = configShortAnimationTime;
        }
    }

    /**
     * Sets the right offset
     *
     * @param rightOffset Offset
     */
    public void setRightOffset(float rightOffset) {
        this.rightOffset = rightOffset;
    }

    /**
     * Set the left offset
     *
     * @param leftOffset Offset
     */
    public void setLeftOffset(float leftOffset) {
        this.leftOffset = leftOffset;
    }

    /**
     * Set if all item opened will be close when the user move ListView
     *
     * @param swipeClosesAllItemsWhenListMoves
     */
    public void setSwipeClosesAllItemsWhenListMoves(boolean swipeClosesAllItemsWhenListMoves) {
        this.swipeClosesAllItemsWhenListMoves = swipeClosesAllItemsWhenListMoves;
    }

    /**
     * Set if the user can open an item with long press on cell
     *
     * @param swipeOpenOnLongPress
     */
    public void setSwipeOpenOnLongPress(boolean swipeOpenOnLongPress) {
        this.swipeOpenOnLongPress = swipeOpenOnLongPress;
    }

    /**
     * Sets the swipe mode
     *
     * @param swipeMode
     */
    public void setSwipeMode(int swipeMode) {
        this.swipeMode = swipeMode;
    }

    /**
     * Return action on left
     *
     * @return Action
     */
    public int getSwipeActionLeft() {
        return swipeActionLeft;
    }

    /**
     * Set action on left
     *
     * @param swipeActionLeft Action
     */
    public void setSwipeActionLeft(int swipeActionLeft) {
        this.swipeActionLeft = swipeActionLeft;
    }

    /**
     * Return action on right
     *
     * @return Action
     */
    public int getSwipeActionRight() {
        return swipeActionRight;
    }

    /**
     * Set action on right
     *
     * @param swipeActionRight Action
     */
    public void setSwipeActionRight(int swipeActionRight) {
        this.swipeActionRight = swipeActionRight;
    }

    /**
     * Adds new items when adapter is modified
     */
    public void resetItems() {
        if (swipeListView.getAdapter() != null) {
            int count = swipeListView.getAdapter().getCount();
            for (int i = opened.size(); i <= count; i++) {
                opened.add(false);
                openedRight.add(false);
            }
        }
    }

    /**
     * Open item
     * @param position Position of list
     */
    protected void openAnimate(int position) {
        openAnimate(swipeListView.getChildAt(position - swipeListView.getFirstVisiblePosition()).findViewById(swipeFrontView), position);
    }

    /**
     * Close item
     * @param position Position of list
     */
    protected void closeAnimate(int position) {
        closeAnimate(swipeListView.getChildAt(position - swipeListView.getFirstVisiblePosition()).findViewById(swipeFrontView), position);
    }

    /**
     * Open item
     * @param view affected view
     * @param position Position of list
     */
    private void openAnimate(View view, int position) {
        if (!opened.get(position)) {
            generateRevealAnimate(view, true, false, position);
        }
    }

    /**
     * Close item
     * @param view affected view
     * @param position Position of list
     */
    private void closeAnimate(View view, int position) {
        if (opened.get(position)) {
            generateRevealAnimate(view, true, false, position);
        }
    }

    /**
     * Create animation
     * @param view affected view
     * @param swap If state should change. If "false" returns to the original position
     * @param swapRight If swap is true, this parameter tells if move is to the right or left
     * @param position Position of list
     */
    private void generateAnimate(final View view, final boolean swap, final boolean swapRight, final int position) {
        if (swipeCurrentAction == SwipeListView.SWIPE_ACTION_REVEAL) {
            generateRevealAnimate(view, swap, swapRight, position);
        }
        if (swipeCurrentAction == SwipeListView.SWIPE_ACTION_DISMISS) {
            generateDismissAnimate(parentView, swap, swapRight, position);
        }
    }

    /**
     * Create dismiss animation
     * @param view affected view
     * @param swap If will change state. If is "false" returns to the original position
     * @param swapRight If swap is true, this parameter tells if move is to the right or left
     * @param position Position of list
     */
    private void generateDismissAnimate(final View view, final boolean swap, final boolean swapRight, final int position) {
        int moveTo = 0;
        if (opened.get(position)) {
            if (!swap) {
                moveTo = openedRight.get(position) ? (int) (viewWidth - rightOffset) : (int) (-viewWidth + leftOffset);
            }
        } else {
            if (swap) {
                moveTo = swapRight ? (int) (viewWidth - rightOffset) : (int) (-viewWidth + leftOffset);
            }
        }

        int alpha = 1;
        if (swap) {
            ++dismissAnimationRefCount;
            alpha = 0;
        }

        animate(view)
                .translationX(moveTo)
                .alpha(alpha)
                .setDuration(animationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (swap) {
                            performDismiss(view, position);
                        }
                    }
                });

    }

    /**
     * Create reveal animation
     * @param view affected view
     * @param swap If will change state. If "false" returns to the original position
     * @param swapRight If swap is true, this parameter tells if movement is toward right or left
     * @param position list position
     */
    private void generateRevealAnimate(final View view, final boolean swap, final boolean swapRight, final int position) {
        int moveTo = 0;
        if (opened.get(position)) {
            if (!swap) {
                moveTo = openedRight.get(position) ? (int) (viewWidth - rightOffset) : (int) (-viewWidth + leftOffset);
            }
        } else {
            if (swap) {
                moveTo = swapRight ? (int) (viewWidth - rightOffset) : (int) (-viewWidth + leftOffset);
            }
        }

        animate(view)
                .translationX(moveTo)
                .setDuration(animationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        swipeListView.resetScrolling();
                        if (swap) {
                            boolean aux = !opened.get(position);
                            opened.set(position, aux);
                            if (aux) {
                                swipeListView.onOpened(position, swapRight);
                                openedRight.set(position, swapRight);
                            } else {
                                swipeListView.onClosed(position, openedRight.get(position));
                            }
                        }
                    }
                });
    }

    /**
     * Set enabled
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        paused = !enabled;
    }

    /**
     * Return ScrollListener for ListView
     * @return OnScrollListener
     */
    public AbsListView.OnScrollListener makeScrollListener() {
        return new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                if (swipeClosesAllItemsWhenListMoves && scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    closeOpenedItems();
                }
                if (scrollState==SCROLL_STATE_TOUCH_SCROLL) {
                    listViewMoving = true;
                }
                if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_FLING && scrollState != SCROLL_STATE_TOUCH_SCROLL) {
                    listViewMoving = false;
                    swipeListView.resetScrolling();
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        };
    }

    /**
     * Close all opened items
     */
    private void closeOpenedItems() {
        if (opened != null) {
            int start = swipeListView.getFirstVisiblePosition();
            int end = swipeListView.getLastVisiblePosition();
            for (int i = start; i <= end; i++) {
                if (opened.get(i)) {
                    closeAnimate(swipeListView.getChildAt(i - start).findViewById(swipeFrontView), i);
                }
            }
        }

    }

    /**
     * @see View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (viewWidth < 2) {
            viewWidth = swipeListView.getWidth();
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (paused) {
                    return false;
                }
                swipeCurrentAction = SwipeListView.SWIPE_ACTION_NONE;
                Rect rect = new Rect();
                int childCount = swipeListView.getChildCount();
                int[] listViewCoords = new int[2];
                swipeListView.getLocationOnScreen(listViewCoords);
                int x = (int) motionEvent.getRawX() - listViewCoords[0];
                int y = (int) motionEvent.getRawY() - listViewCoords[1];
                View child;
                for (int i = 0; i < childCount; i++) {
                    child = swipeListView.getChildAt(i);
                    child.getHitRect(rect);
                    if (rect.contains(x, y)) {
                        setParentView(child);
                        setFrontView(child.findViewById(swipeFrontView));
                        downX = motionEvent.getRawX();
                        downPosition = swipeListView.getPositionForView(child);

                        frontView.setClickable(!opened.get(downPosition));
                        frontView.setLongClickable(!opened.get(downPosition));

                        velocityTracker = VelocityTracker.obtain();
                        velocityTracker.addMovement(motionEvent);
                        if (swipeBackView > 0) {
                            setBackView(child.findViewById(swipeBackView));
                        }
                        break;
                    }
                }
                view.onTouchEvent(motionEvent);
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (velocityTracker == null || !swiping) {
                    break;
                }

                float deltaX = motionEvent.getRawX() - downX;
                velocityTracker.addMovement(motionEvent);
                velocityTracker.computeCurrentVelocity(1000);
                float velocityX = Math.abs(velocityTracker.getXVelocity());
                if (!opened.get(downPosition)) {
                    if (swipeMode == SwipeListView.SWIPE_MODE_LEFT && velocityTracker.getXVelocity() > 0) {
                        velocityX = 0;
                    }
                    if (swipeMode == SwipeListView.SWIPE_MODE_RIGHT && velocityTracker.getXVelocity() < 0) {
                        velocityX = 0;
                    }
                }
                float velocityY = Math.abs(velocityTracker.getYVelocity());
                boolean swap = false;
                boolean swapRight = false;
                if (minFlingVelocity <= velocityX && velocityX <= maxFlingVelocity && velocityY < velocityX) {
                    swapRight = velocityTracker.getXVelocity() > 0;
                    if (opened.get(downPosition) && openedRight.get(downPosition) && swapRight) {
                        swap = false;
                    } else if (opened.get(downPosition) && !openedRight.get(downPosition) && !swapRight) {
                        swap = false;
                    } else {
                        swap = true;
                    }
                } else if (Math.abs(deltaX) > viewWidth / 2) {
                    swap = true;
                    swapRight = deltaX > 0;
                }
                generateAnimate(frontView, swap, swapRight, downPosition);

                velocityTracker = null;
                downX = 0;
                // change clickable front view
                if (swap) {
                    frontView.setClickable(opened.get(downPosition));
                    frontView.setLongClickable(opened.get(downPosition));
                }
                frontView = null;
                backView = null;
                this.downPosition = ListView.INVALID_POSITION;
                swiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (velocityTracker == null || paused) {
                    break;
                }

                velocityTracker.addMovement(motionEvent);
                velocityTracker.computeCurrentVelocity(1000);
                float velocityX = Math.abs(velocityTracker.getXVelocity());
                float velocityY = Math.abs(velocityTracker.getYVelocity());

                float deltaX = motionEvent.getRawX() - downX;
                float deltaMode = Math.abs(deltaX);
                if (swipeMode == SwipeListView.SWIPE_MODE_NONE) {
                    deltaMode = 0;
                } else if (swipeMode != SwipeListView.SWIPE_MODE_BOTH) {
                    if (opened.get(downPosition)) {
                        if (swipeMode == SwipeListView.SWIPE_MODE_LEFT && deltaX < 0) {
                            deltaMode = 0;
                        } else if (swipeMode == SwipeListView.SWIPE_MODE_RIGHT && deltaX > 0) {
                            deltaMode = 0;
                        }
                    } else {
                        if (swipeMode == SwipeListView.SWIPE_MODE_LEFT && deltaX > 0) {
                            deltaMode = 0;
                        } else if (swipeMode == SwipeListView.SWIPE_MODE_RIGHT && deltaX < 0) {
                            deltaMode = 0;
                        }
                    }
                }
                if (deltaMode > slop && swipeCurrentAction == SwipeListView.SWIPE_ACTION_NONE && velocityY < velocityX) {
                    swiping = true;
                    boolean swipingRight = (deltaX > 0);
                    if (opened.get(downPosition)) {
                        swipeCurrentAction = SwipeListView.SWIPE_ACTION_REVEAL;
                    } else {
                        if (swipingRight && swipeActionRight == SwipeListView.SWIPE_ACTION_DISMISS) {
                            swipeCurrentAction = SwipeListView.SWIPE_ACTION_DISMISS;
                        } else if (!swipingRight && swipeActionLeft == SwipeListView.SWIPE_ACTION_DISMISS) {
                            swipeCurrentAction = SwipeListView.SWIPE_ACTION_DISMISS;
                        } else if (swipingRight && swipeActionRight == SwipeListView.SWIPE_ACTION_CHECK) {
                            swipeCurrentAction = SwipeListView.SWIPE_ACTION_CHECK;
                        } else if (!swipingRight && swipeActionLeft == SwipeListView.SWIPE_ACTION_CHECK) {
                            swipeCurrentAction = SwipeListView.SWIPE_ACTION_CHECK;
                        } else {
                            swipeCurrentAction = SwipeListView.SWIPE_ACTION_REVEAL;
                        }
                    }
                    swipeListView.requestDisallowInterceptTouchEvent(true);
                    MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (motionEvent.getActionIndex()
                                    << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    swipeListView.onTouchEvent(cancelEvent);
                }

                if (swiping) {
                    if (opened.get(downPosition)) {
                        deltaX += openedRight.get(downPosition) ? viewWidth - rightOffset : -viewWidth + leftOffset;
                    }
                    move(deltaX);
                    return true;
                }
                break;
            }
        }
        return false;
    }

    /**
     * Moves the view
     * @param deltaX delta
     */
    public void move(float deltaX) {
        swipeListView.onMove(downPosition, deltaX);
        if (swipeCurrentAction == SwipeListView.SWIPE_ACTION_DISMISS) {
            setTranslationX(parentView, deltaX);
            setAlpha(parentView, Math.max(0f, Math.min(1f,
                    1f - 2f * Math.abs(deltaX) / viewWidth)));
        } else {
            setTranslationX(frontView, deltaX);
        }
    }

    /**
     * Class that saves pending dismiss data
     */
    class PendingDismissData implements Comparable<PendingDismissData> {
        public int position;
        public View view;

        public PendingDismissData(int position, View view) {
            this.position = position;
            this.view = view;
        }

        @Override
        public int compareTo(PendingDismissData other) {
            // Sort by descending position
            return other.position - position;
        }
    }

    /**
     * Perform dismiss action
     * @param dismissView View
     * @param dismissPosition Position of list
     */
    private void performDismiss(final View dismissView, final int dismissPosition) {
        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        final int originalHeight = dismissView.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(animationTime);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                --dismissAnimationRefCount;
                if (dismissAnimationRefCount == 0) {
                    // No active animations, process all pending dismisses.
                    // Sort by descending position
                    Collections.sort(pendingDismisses);

                    int[] dismissPositions = new int[pendingDismisses.size()];
                    for (int i = pendingDismisses.size() - 1; i >= 0; i--) {
                        dismissPositions[i] = pendingDismisses.get(i).position;
                    }
                    swipeListView.onDismiss(dismissPositions);

                    ViewGroup.LayoutParams lp;
                    for (PendingDismissData pendingDismiss : pendingDismisses) {
                        // Reset view presentation
                        setAlpha(pendingDismiss.view, 1f);
                        setTranslationX(pendingDismiss.view, 0);
                        lp = pendingDismiss.view.getLayoutParams();
                        lp.height = originalHeight;
                        pendingDismiss.view.setLayoutParams(lp);
                    }

                    pendingDismisses.clear();
                }
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (Integer) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });

        pendingDismisses.add(new PendingDismissData(dismissPosition, dismissView));
        animator.start();
    }

}
