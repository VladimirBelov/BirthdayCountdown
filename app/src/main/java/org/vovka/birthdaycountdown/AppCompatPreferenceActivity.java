/*
 * *
 *  * Created by Vladimir Belov on 31.03.2025, 10:49
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 31.03.2025, 08:41
 *
 */

package org.vovka.birthdaycountdown;

import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

/**
 * Класс AppCompatPreferenceActivity действует как прокси для {@link AppCompatDelegate}, обрабатывая
 * необходимые вызовы для интеграции функций AppCompat внутри {@link PreferenceActivity}.
 * Это включает управление панелью действий, инфляцию меню и другие события жизненного цикла.
 * </p>
 * <p>
 * Хотя он и предназначен для использования с {@link PreferenceActivity}, основной
 * метод использования {@link AppCompatDelegate} может быть применен к любому
 * {@link android.app.Activity} для интеграции возможностей AppCompat.
 * </p>
 * <p>
 * Использование этого класса обеспечит:
 * <ul>
 *     <li>Панель действий.</li>
 *     <li>Темы AppCompat.</li>
 *     <li>Инфляцию меню из AppCompat.</li>
 *     <li>И многое другое.</li>
 * </ul>
 * </p>
 */

@SuppressWarnings("deprecation")
public abstract class AppCompatPreferenceActivity extends PreferenceActivity {
    private AppCompatDelegate mDelegate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }
    ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }
    void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }
    @NonNull
    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }
    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }
    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }
    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }
    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }
    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }
    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }
    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }
    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }
}