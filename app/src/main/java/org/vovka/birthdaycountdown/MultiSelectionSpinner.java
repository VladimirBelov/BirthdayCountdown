/*
 * *
 *  * Created by Vladimir Belov on 17.08.2021, 10:49
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 11.08.2021, 22:23
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import androidx.appcompat.app.AlertDialog;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;

// https://trinitytuts.com/tips/multiselect-spinner-item-in-android/
public class MultiSelectionSpinner extends androidx.appcompat.widget.AppCompatSpinner implements DialogInterface.OnMultiChoiceClickListener {
    String[] _items = null;
    boolean[] mSelection = null;
    private String zeroSelectedTitle = STRING_EMPTY;

    final ArrayAdapter<String> simple_adapter;

    public MultiSelectionSpinner(Context context) {
        super(context);

        simple_adapter = new ArrayAdapter<>(context, R.layout.list_item_1);
        super.setAdapter(simple_adapter);
    }

    public MultiSelectionSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);

        simple_adapter = new ArrayAdapter<>(context, R.layout.list_item_1); //simple_spinner_item
        super.setAdapter(simple_adapter);
    }

    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        if (mSelection != null && which < mSelection.length) {
            mSelection[which] = isChecked;
            simple_adapter.clear();
            simple_adapter.add(buildSelectedItemString());
        //} else {
        //    throw new IllegalArgumentException("Argument 'which' is out of bounds.");
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMultiChoiceItems(_items, mSelection, this);
        builder.show();
        return true;
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        throw new RuntimeException("setAdapter is not supported by MultiSelectSpinner.");
    }

/*    public void setItems(String[] items) {
        _items = items;
        mSelection = new boolean[_items.length];
        simple_adapter.clear();
        simple_adapter.add(_items[0]);
        Arrays.fill(mSelection, false);
    }*/

    public void setItems(List<String> items) {
        _items = items.toArray(new String[0]);
        mSelection = new boolean[_items.length];
        simple_adapter.clear();
        //simple_adapter.add(_items[0]);
        Arrays.fill(mSelection, false);
    }

/*    public void setSelection(String[] selection) {
        for (String cell : selection) {
            for (int j = 0; j < _items.length; ++j) {
                if (_items[j].equals(cell)) {
                    mSelection[j] = true;
                }
            }
        }
    }*/

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
        simple_adapter.clear();
        simple_adapter.add(buildSelectedItemString());
    }

/*    public void setSelection(int index) {
  //      Arrays.fill(mSelection, false);
        *//*for (int i = 0; i < mSelection.length; i++) {
            mSelection[i] = false;
        }*//*
  //      if (index >= 0 && index < mSelection.length) {
  //          mSelection[index] = true;
        //} else {
        //    throw new IllegalArgumentException("Index " + index + " is out of bounds.");
   //     }
        simple_adapter.clear();
        simple_adapter.add(buildSelectedItemString());
    }*/

/*    public void setSelection(int[] selectedIndicies) {
        *//*for (int i = 0; i < mSelection.length; i++) {
            mSelection[i] = false;
        }*//*
        for (int index : selectedIndicies) {
            if (index >= 0 && index < mSelection.length) {
                mSelection[index] = true;
            } else {
                throw new IllegalArgumentException("Index " + index
                        + " is out of bounds.");
            }
        }
        simple_adapter.clear();
        simple_adapter.add(buildSelectedItemString());
    }*/

    public List<String> getSelectedStrings() {
        List<String> selection = new LinkedList<>();
        for (int i = 0; i < _items.length; i++) {
            if (mSelection[i]) {
                selection.add(_items[i]);
            }
        }
        return selection;
    }

/*    public List<Integer> getSelectedIndicies() {
        List<Integer> selection = new LinkedList<>();
        for (int i = 0; i < _items.length; ++i) {
            if (mSelection[i]) {
                selection.add(i);
            }
        }
        return selection;
    }*/

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

/*    public String getSelectedItemsAsString() {
        StringBuilder sb = new StringBuilder();
        boolean foundOne = false;

        for (int i = 0; i < _items.length; ++i) {
            if (mSelection[i]) {
                if (foundOne) {
                    sb.append(", ");
                }
                foundOne = true;
                sb.append(_items[i]);
            }
        }
        return sb.toString();
    }*/
}
