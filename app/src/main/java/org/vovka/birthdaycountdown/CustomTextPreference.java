/*
 * *
 *  * Created by Vladimir Belov on 20.03.2026, 12:34
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 20.03.2026, 12:32
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class CustomTextPreference extends Preference {

    public interface ValueProvider {
        String getCurrentValue();
    }

    private ValueProvider mValueProvider = () -> Constants.STRING_EMPTY;

    public CustomTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public CustomTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CustomTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomTextPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.settings_widget_text);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView widgetText = view.findViewById(R.id.widget_text);
        if (widgetText != null) {
            String value = mValueProvider.getCurrentValue();
            widgetText.setText(value != null ? value : Constants.STRING_EMPTY);
        }
    }

    public String getCurrentValue() {
        return mValueProvider.getCurrentValue();
    }

    public void setValueProvider(ValueProvider provider) {
        mValueProvider = provider;
        notifyChanged();
    }

    public void refresh() {
        notifyChanged();
    }
}