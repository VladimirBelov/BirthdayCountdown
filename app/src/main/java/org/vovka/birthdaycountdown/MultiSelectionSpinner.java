/*
 * *
 *  * Created by Vladimir Belov on 26.12.2021, 1:01
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 23.12.2021, 14:16
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import androidx.appcompat.app.AlertDialog;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

// https://trinitytuts.com/tips/multiselect-spinner-item-in-android/
public class MultiSelectionSpinner extends androidx.appcompat.widget.AppCompatSpinner implements DialogInterface.OnMultiChoiceClickListener {
    String[] _items = null;
    boolean[] mSelection = null;
    private String zeroSelectedTitle = STRING_EMPTY;
    private int zeroSelectedIndex = -1;
    private AlertDialog.Builder dialogBuilder;

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

            simple_adapter.clear();
            simple_adapter.add(buildSelectedItemString());

            //todo: добавить выключение взаимоисключающих элементов https://stackoverflow.com/questions/39053333/disable-checkbox-items-in-alertdialog
            // https://stackoverflow.com/questions/7359685/android-disable-all-other-items-on-dialog-when-clicked-on-another
        //} else {
        //    throw new IllegalArgumentException("Argument 'which' is out of bounds.");
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean performClick() {
        //super.performClick(); если это включить, то фокус уходит за 2 клика
        dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setMultiChoiceItems(_items, mSelection, this);
        dialogBuilder.show(); //todo: добавить onDismiss обработчик для пост обработки выбранного (когда будет дерево вариантов)
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

    //Первоначальное заполнение, ничего не выбрано
    public void setItems(List<String> items) {
        _items = items.toArray(new String[0]);
        mSelection = new boolean[_items.length];
        Arrays.fill(mSelection, false);

        simple_adapter.clear();
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

    //Установка выбранных элементов
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

    public void setZeroSelectedIndex(int zeroSelectedIndex) {
        this.zeroSelectedIndex = zeroSelectedIndex;
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
