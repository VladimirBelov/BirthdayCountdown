/*
 * *
 *  * Created by Vladimir Belov on 23.10.2026, 23:46
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 23.10.2026, 22:20
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

    private ValueProvider mValueProvider = () -> "";

    @SuppressWarnings("unused")
    public CustomTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @SuppressWarnings("unused")
    public CustomTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressWarnings("unused")
    public CustomTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("unused")
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
            widgetText.setText(value != null ? value : "");
        }
    }

    public void setValueProvider(ValueProvider provider) {
        mValueProvider = provider;
        notifyChanged();
    }

    public void refresh() {
        notifyChanged();
    }
}