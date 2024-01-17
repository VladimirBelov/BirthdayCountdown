/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 09.01.2024, 23:19
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

// https://trinitytuts.com/tips/multiselect-spinner-item-in-android/
public class MultiSelectionSpinner extends androidx.appcompat.widget.AppCompatSpinner implements DialogInterface.OnMultiChoiceClickListener {
    String[] _items = null;
    boolean[] mSelection = null;
    private boolean isSortable = false;
    private String zeroSelectedTitle = Constants.STRING_EMPTY;
    private int zeroSelectedIndex = -1;
    private AlertDialog.Builder dialogBuilder;
    final ArrayAdapter<String> adapter;
    private ArrayList<String> mColored = new ArrayList<>();
    private ArrayList<String> mNonSorted = new ArrayList<>();
    private int mColor = 0;
    FragmentManager fm;
    Fragment fragment;
    Menu menu;

    public MultiSelectionSpinner(Context context) {
        super(context);

        adapter = new ArrayAdapter<>(context, R.layout.list_item_text);
        super.setAdapter(adapter);
    }

    public MultiSelectionSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);

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

            args.putStringArrayList(Constants.EXTRA_LIST, mItems);
            args.putIntegerArrayList(Constants.EXTRA_CHECKS, mSelected);
            args.putStringArrayList(Constants.EXTRA_COLORED, mColored);
            args.putStringArrayList(Constants.EXTRA_NON_SORTED, mNonSorted);
            args.putInt(Constants.EXTRA_COLOR, mColor);
            args.putString(Constants.EXTRA_TITLE, getContext().getString(R.string.pref_List_EventInfo_title));

            fragment.setArguments(args);

            if (menu != null) {
                final MenuItem itemOk = menu.findItem(R.id.menu_ok);
                if (itemOk != null) itemOk.setVisible(true);

                final MenuItem itemHelp = menu.findItem(R.id.menu_help_widgets);
                if (itemHelp != null) itemHelp.setVisible(false);
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
        //throw new RuntimeException("setAdapter is not supported by MultiSelectSpinner.");
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
            final ArrayList<String> selected = args.getStringArrayList(Constants.EXTRA_RESULTS);
            if (selected != null) {
                if (!selected.isEmpty()) {
                    moveToBeginning(selected);
                    setSelection(selected);
                }
                if (!mNonSorted.isEmpty()) {
                    moveToBeginning(mNonSorted);
                    setSelection(selected);
                }
            }
        } else {
            ToastExpander.showInfoMsg(getContext(), "No results!");
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

    public void setSortable(boolean sortable) {
        isSortable = sortable;
    }

    public void setColored(ArrayList<String> listColored, int color) {
        this.mColored = listColored;
        this.mColor = color;
    }

    public void setNonSorted(ArrayList<String> listNonSorted) {
        this.mNonSorted = listNonSorted;
    }

}
