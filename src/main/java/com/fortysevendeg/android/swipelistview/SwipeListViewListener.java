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

public interface SwipeListViewListener {

    /**
     * Called when finish animation open
     * @param position Item of list
     * @param toRight Open to right
     */
    void onOpened(int position, boolean toRight);

    /**
     * Called when finish animation close
     * @param position Item of list
     * @param fromRight Close from right
     */
    void onClosed(int position, boolean fromRight);

    /**
     * Called when list changed
     */
    void onListChanged();

    /**
     * Called when user is moving an item
     * @param position Item of list
     * @param x Current position X
     */
    void onMove(int position, float x);

    /**
     * Called when user click on front view
     * @param position Item of list
     */
    void onClickFrontView(int position);

    /**
     * Called when user click on back view
     * @param position Item of list
     */
    void onClickBackView(int position);

    /**
     * Called when user dismiss items
     * @param reverseSortedPositions Items dismisses
     */
    void onDismiss(int[] reverseSortedPositions);

}
