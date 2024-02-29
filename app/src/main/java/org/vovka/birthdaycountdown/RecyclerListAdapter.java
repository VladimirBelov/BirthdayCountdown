/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 04.12.2022, 12:46
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import org.vovka.birthdaycountdown.helper.ItemTouchHelperAdapter;
import org.vovka.birthdaycountdown.helper.ItemTouchHelperViewHolder;
import org.vovka.birthdaycountdown.helper.OnStartDragListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView.Adapter that implements {@link ItemTouchHelperAdapter} to respond to move and
 * dismiss events from a {@link ItemTouchHelper}.
 *
 * @author Paul Burke (ipaulpro)
 */
public class RecyclerListAdapter extends RecyclerView.Adapter<RecyclerListAdapter.ItemViewHolder> implements ItemTouchHelperAdapter {
    private final List<String> mItems = new ArrayList<>();
    private final List<String> mColoredItems = new ArrayList<>();
    private final List<String> mNonSortableItems = new ArrayList<>();
    private final List<Integer> mSelected = new ArrayList<>();
    private final List<Integer> mIndex = new ArrayList<>();
    private final int colorItem;
    private final int colorAlt;
    private final OnStartDragListener mDragStartListener;

    public RecyclerListAdapter(@NonNull Context context, OnStartDragListener dragStartListener, List<String> items, List<Integer> selected, List<String> coloredItems, int color, List<String> nonSortableItems) {

        List<Integer> selectedItems = selected;
        mDragStartListener = dragStartListener;

        if (items != null) {
            mItems.addAll(items);
            if (selectedItems == null || selectedItems.size() != items.size()) {
                selectedItems = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) selectedItems.add(0);
            }
            mSelected.addAll(selectedItems);
            for (int i = 0; i < selectedItems.size(); i++) mIndex.add(i);
        }
        if (coloredItems != null) mColoredItems.addAll(coloredItems);
        if (nonSortableItems != null) mNonSortableItems.addAll(nonSortableItems);
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.dialogHintColor, typedValue, true);
        colorItem = typedValue.data;
        colorAlt = color;
        //printAll();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.draggable_item, parent, false);
        return new ItemViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(final ItemViewHolder holder, int position) {
        holder.textView.setText(mItems.get(position));
        if (mColoredItems.contains(mItems.get(position)) && colorAlt != 0) {
            holder.textView.setTextColor(colorAlt);
        } else {
            holder.textView.setTextColor(colorItem);
        }
        holder.checkBoxView.setChecked(mSelected.get(mIndex.get(position)) == 1);
        holder.checkBoxView.setOnClickListener(view -> mSelected.set(position, mSelected.get(position) == 1 ? 0 : 1));

        if (!mNonSortableItems.isEmpty() && mNonSortableItems.contains(mItems.get(position))) {
            holder.handleView.setVisibility(View.GONE);
        } else {
            holder.handleView.setVisibility(View.VISIBLE);
            holder.handleView.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mDragStartListener.onStartDrag(holder);
                }
                return false;
            });
        }
    }

    @Override
    public void onItemDismiss(int position) {
        //Toast.makeText(context, "remove: " + position, Toast.LENGTH_SHORT).show();
        //mSelected.remove((int)mIndex.get(position));
        //mIndex.remove(position);
        //mItems.remove(position);

        //notifyItemRemoved(position);
        //printAll();
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        if (!mNonSortableItems.isEmpty() && toPosition < mNonSortableItems.size()) return; //Dragged too much
        Collections.swap(mItems, fromPosition, toPosition);
        //Collections.swap(mSelected, fromPosition, toPosition);
        Collections.swap(mIndex, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        //printAll();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    /**
     * Holder that implements {@link ItemTouchHelperViewHolder} and has a
     * "handle" view that initiates a drag event when touched.
     */
    public static class ItemViewHolder extends RecyclerView.ViewHolder implements
            ItemTouchHelperViewHolder {

        public final CheckBox checkBoxView;
        public final TextView textView;
        public final ImageView handleView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            checkBoxView = itemView.findViewById(R.id.checkbox);
            textView = itemView.findViewById(R.id.text);
            handleView = itemView.findViewById(R.id.handle);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }
    }

    public ArrayList<String> getAllSelectedItems() {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            if(mSelected.get(mIndex.get(i)) == 1) result.add(mItems.get(i));
        }
        return result;
    }

    /*public void printAll() {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            result.add(mIndex.get(i) + ": " + mItems.get(i) + ", selected:" + mSelected.get(i) + "\n"); //mSelected.get(mIndex.get(i)) + "\n");
        }
       Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show();
    }*/
}
