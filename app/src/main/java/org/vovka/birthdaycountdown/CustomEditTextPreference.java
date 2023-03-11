/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 26.12.2021, 1:01
 *
 */

package org.vovka.birthdaycountdown;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.EditText;

public class CustomEditTextPreference extends EditTextPreference {

    public CustomEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CustomEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomEditTextPreference(Context context) {
        super(context);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {

        ContactsEvents contactsEvents = ContactsEvents.getInstance();
        if (contactsEvents.preferences_theme.themeEditText != 0) {
            builder.getContext().setTheme(contactsEvents.preferences_theme.themeEditText);
        } else {
            builder.getContext().setTheme(ContactsEvents.themeEditText_default);
        }

        super.onPrepareDialogBuilder(builder);

    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        TypedArray ta = getContext().getTheme().obtainStyledAttributes(R.styleable.Theme);
        final AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(ta.getColor(R.styleable.Theme_editTextBackgroundCustom, 0)));

        /*int alertTitleId = getContext().getResources().getIdentifier("alertTitle", "id", "android");
        TextView alertTitle = (TextView) dialog.getWindow().getDecorView().findViewById(alertTitleId);
        alertTitle.setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));*/

        final EditText editText = getEditText();
        if (editText != null) {
            //https://www.thetopsites.net/article/51779376.shtml
            //в светлой теме получилось выставить editTextPreference color только таким способом
            editText.setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
            editText.setHintTextColor(ta.getColor(R.styleable.Theme_dialogHintColor, 0));
            //editText.setContentDescription(getContext().getString(R.string.widget_EditText));
            //final float scale = getContext().getResources().getDisplayMetrics().density;
            //editText.setMinimumHeight((int) (48 * scale + 0.5f));
            //todo: сделать опциональный MaxLine
            editText.setMinimumHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getContext().getResources().getDisplayMetrics()));
            ta.recycle();
        }
    }
}
