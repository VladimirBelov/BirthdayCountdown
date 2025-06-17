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
 * Interface to notify an item ViewHolder of relevant callbacks from {@link ItemTouchHelper}.
 *
 * @author Paul Burke (ipaulpro)
 */
public interface ItemTouchHelperViewHolder {

    /**
     * Called when the {@link ItemTouchHelper} first registers an item as being moved or swiped.
     * Implementations should update the item view to indicate it's active state.
     */
    void onItemSelected();

    /**
     * Called when the {@link ItemTouchHelper} has completed the move or swipe, and the active item
     * state should be cleared.
     */
    void onItemClear();
}
