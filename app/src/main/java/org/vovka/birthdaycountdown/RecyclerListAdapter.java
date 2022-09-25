/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 20.07.2022, 16:37
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

import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.vovka.birthdaycountdown.helper.ItemTouchHelperAdapter;
import org.vovka.birthdaycountdown.helper.ItemTouchHelperViewHolder;
import org.vovka.birthdaycountdown.helper.OnStartDragListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RecyclerView.Adapter that implements {@link ItemTouchHelperAdapter} to respond to move and
 * dismiss events from a {@link ItemTouchHelper}.
 *
 * @author Paul Burke (ipaulpro)
 */
public class RecyclerListAdapter extends RecyclerView.Adapter<RecyclerListAdapter.ItemViewHolder> implements ItemTouchHelperAdapter {

    private final List<String> mItems = new ArrayList<>();
    private final List<String> mColoredItems = new ArrayList<>();
    private final List<Integer> mSelected = new ArrayList<>();
    private final List<Integer> mIndex = new ArrayList<>();
    private final int mColor;
    private final OnStartDragListener mDragStartListener;
    private final Context context;

    public RecyclerListAdapter(@NonNull Context context, OnStartDragListener dragStartListener, List<String> items, List<Integer> selected, List<String> coloredItems, int color) {

        mDragStartListener = dragStartListener;
        this.context = context;

        if (items != null) {
            mItems.addAll(items);
            if (selected == null || selected.size() != items.size()) {
                selected = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) selected.add(0);
            }
            mSelected.addAll(selected);
            for (int i = 0; i < selected.size(); i++) mIndex.add(i);
        }
        if (coloredItems != null) mColoredItems.addAll(coloredItems);
        mColor = color;
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
        if (mColoredItems.contains(mItems.get(position)) && mColor != 0) {
            holder.textView.setTextColor(mColor);
        } else {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.dialogHintColor, typedValue, true);
            holder.textView.setTextColor(typedValue.data);
        }
        holder.checkBoxView.setChecked(mSelected.get(mIndex.get(position)) == 1);
        holder.checkBoxView.setOnClickListener(view -> {
            //mSelected.set(mItems.indexOf(mItems.get(position)), mSelected.get(mItems.indexOf(mItems.get(position))) == 1 ? 0 : 1);
            //Toast.makeText(context, "pos " + position + "=" + mSelected.get(position) + ", set: " + (mSelected.get(position) == 1 ? 0 : 1), Toast.LENGTH_LONG).show();
            mSelected.set(position, mSelected.get(position) == 1 ? 0 : 1);
            //printAll();
        });

        // Start a drag whenever the handle view it touched
        holder.handleView.setOnTouchListener((v, event) -> {
            //noinspection deprecation
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                mDragStartListener.onStartDrag(holder);
            }
            return false;
        });
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
