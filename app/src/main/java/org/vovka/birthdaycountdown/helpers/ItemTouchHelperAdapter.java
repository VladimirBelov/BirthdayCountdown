/*
 * *
 *  * Created by Vladimir Belov on 17.06.2025, 10:00
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 16.06.2025, 23:33
 *
 */

package org.vovka.birthdaycountdown.helpers;

import androidx.recyclerview.widget.ItemTouchHelper;

/**
 * Interface to listen for a move or dismissal event from a {@link ItemTouchHelper}.
 *
 * @author Paul Burke (ipaulpro)
 */
public interface ItemTouchHelperAdapter {

    /**
     * Called when an item has been dragged far enough to trigger a move. This is called every time
     * an item is shifted, and <strong>not</strong> at the end of a "drop" event.<br/>
     * <br/>
     * Implementations should call RecyclerView.Adapter#notifyItemMoved(int, int) after
     * adjusting the underlying data to reflect this move.
     *
     * @param fromPosition The start position of the moved item.
     * @param toPosition   Then resolved position of the moved item.
     */
    void onItemMove(int fromPosition, int toPosition);

    /**
     * Called when an item has been dismissed by a swipe.<br/>
     * <br/>
     * Implementations should call RecyclerView.Adapter#notifyItemRemoved(int) after
     * adjusting the underlying data to reflect this removal.
     *
     * @param position The position of the item dismissed.
     */
    @SuppressWarnings("EmptyMethod")
    void onItemDismiss(int position);
}
