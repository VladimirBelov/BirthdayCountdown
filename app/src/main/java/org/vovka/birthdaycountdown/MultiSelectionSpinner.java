/*
 * *
 *  * Created by Vladimir Belov on 26.12.2021, 1:01
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 23.12.2021, 14:16
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.EXTRA_CHECKS;
import static org.vovka.birthdaycountdown.Constants.EXTRA_COLOR;
import static org.vovka.birthdaycountdown.Constants.EXTRA_COLORED;
import static org.vovka.birthdaycountdown.Constants.EXTRA_LIST;
import static org.vovka.birthdaycountdown.Constants.EXTRA_RESULTS;
import static org.vovka.birthdaycountdown.Constants.EXTRA_TITLE;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

// https://trinitytuts.com/tips/multiselect-spinner-item-in-android/
public class MultiSelectionSpinner extends androidx.appcompat.widget.AppCompatSpinner implements DialogInterface.OnMultiChoiceClickListener {
    String[] _items = null;
    boolean[] mSelection = null;
    private boolean isSortable = false;
    private String zeroSelectedTitle = STRING_EMPTY;
    private int zeroSelectedIndex = -1;
    private AlertDialog.Builder dialogBuilder;
    final ArrayAdapter<String> adapter;
    private ArrayList<String> mColored = new ArrayList<>();
    private int mColor = 0;
    private final Context context;
    FragmentManager fm;
    Fragment fragment;
    Menu menu;

    public MultiSelectionSpinner(Context context) {
        super(context);

        this.context = context;
        adapter = new ArrayAdapter<>(context, R.layout.list_item_text);
        super.setAdapter(adapter);
    }

    public MultiSelectionSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        adapter = new ArrayAdapter<>(context, R.layout.list_item_text);
        super.setAdapter(adapter);
    }

    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        if (mSelection != null && which < mSelection.length) {
            mSelection[which] = isChecked;

            if (zeroSelectedIndex > -1 && isChecked) {
                if (which == zeroSelectedIndex) { //выбрали "Ничего"
                    Arrays.fill(mSelection, false);
                    mSelection[zeroSelectedIndex] = true;
                    dialog.dismiss();
                } else if (mSelection[zeroSelectedIndex]) { //выбрали что-то другое
                    dialog.dismiss();
                    mSelection[zeroSelectedIndex] = false;
                    dialogBuilder.setMultiChoiceItems(_items, mSelection, this);
                    dialogBuilder.show();
                }

            }

            adapter.clear();
            adapter.add(buildSelectedItemString());

            //todo: добавить выключение взаимоисключающих элементов https://stackoverflow.com/questions/39053333/disable-checkbox-items-in-alertdialog
            // https://stackoverflow.com/questions/7359685/android-disable-all-other-items-on-dialog-when-clicked-on-another
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean performClick() {
        if (!isSortable || fm == null) {

            //super.performClick(); если это включить, то фокус уходит за 2 клика
            dialogBuilder = new AlertDialog.Builder(getContext());
            dialogBuilder.setMultiChoiceItems(_items, mSelection, this);
            dialogBuilder.show();
            //todo: добавить onDismiss обработчик для пост обработки выбранного (когда будет дерево вариантов)

        } else {

            fragment = new RecyclerListFragment();
            Bundle args = new Bundle();
            ArrayList<String> mItems = new ArrayList<>(Arrays.asList(_items));
            ArrayList<Integer> mSelected = new ArrayList<>();
            for (boolean b : mSelection) {
                mSelected.add(b ? 1 : 0);
            }

            args.putStringArrayList(EXTRA_LIST, mItems);
            args.putIntegerArrayList(EXTRA_CHECKS, mSelected);
            args.putStringArrayList(EXTRA_COLORED, mColored);
            args.putInt(EXTRA_COLOR, mColor);
            args.putString(EXTRA_TITLE, getContext().getString(R.string.pref_List_EventInfo_title));

            fragment.setArguments(args);

            if (menu != null) {
                menu.getItem(0).setVisible(true);
            }

            fm.beginTransaction()
                    .replace(R.id.layout_fragment, fragment)
                    .addToBackStack(null)
                    .commit();

        }
        return true;
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        throw new RuntimeException("setAdapter is not supported by MultiSelectSpinner.");
    }

    /**
     * Set {@link MultiSelectionSpinner} values. Clear selection
     * @param items String values
     */
    public void setItems(List<String> items) {
        _items = items.toArray(new String[0]);
        mSelection = new boolean[_items.length];
        Arrays.fill(mSelection, false);

        adapter.clear();
    }

    /**
     * Set {@link MultiSelectionSpinner} selected values
     * @param selection Selected string values
     */
    public void setSelection(List<String> selection) {
        Arrays.fill(mSelection, false);
        if (selection.size() > 0) {
            for (String sel: selection) {
                for (int j = 0; j < _items.length; ++j) {
                    if (_items[j].equals(sel)) {
                        mSelection[j] = true;
                    }
                }
            }
        }
        adapter.clear();
        adapter.add(buildSelectedItemString());
    }

    /**
     * Set {@link MultiSelectionSpinner} selection from {@link RecyclerListFragment} and move selected to the beginning
     */
    public void setSelectedFromFragmentResults() {
        Bundle args = fragment.getArguments();
        if (args != null) {
            final ArrayList<String> selected = args.getStringArrayList(EXTRA_RESULTS);
            //Toast.makeText(getContext(), "Selected:\n" + selected.toString() , Toast.LENGTH_LONG).show();
            if (!selected.isEmpty()) {
                moveToBeginning(selected);
                setSelection(selected);
            }
        } else {
            Toast.makeText(getContext(), "No results!" , Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Move elements to beginning of the list and clear selection
     */
    public void moveToBeginning(List<String> list) {
        List<String> itemsA = new ArrayList<>(list);
        List<String> itemsB = new ArrayList<>();

        for (String item: _items) {
            if (!list.contains(item)) itemsB.add(item);
        }
        if (itemsA.addAll(itemsB)) {
            setItems(itemsA);
            Arrays.fill(mSelection, false);
        }
    }

    public List<String> getSelectedStrings() {
        List<String> selection = new LinkedList<>();
        for (int i = 0; i < _items.length; i++) {
            if (mSelection[i]) {
                selection.add(_items[i]);
            }
        }
        return selection;
    }

    private String buildSelectedItemString() {
        StringBuilder sb = new StringBuilder();
        boolean foundOne = false;

        for (int i = 0; i < _items.length; i++) {
            if (mSelection[i]) {
                if (foundOne) {
                    sb.append("\n");
                }
                foundOne = true;

                sb.append(_items[i]);
            }
        }
        return sb.length() == 0 ? zeroSelectedTitle : sb.toString();
    }

    public void setZeroSelectedTitle(String zeroSelectedTitle) {
        this.zeroSelectedTitle = zeroSelectedTitle;
    }

    public void setZeroSelectedIndex(int zeroSelectedIndex) {
        this.zeroSelectedIndex = zeroSelectedIndex;
    }

    public boolean isSortable() {
        return isSortable;
    }

    public void setSortable(boolean sortable) {
        isSortable = sortable;
    }

    public void setColored(ArrayList<String> listColored, int color) {
        this.mColored = listColored;
        this.mColor = color;
    }

}
