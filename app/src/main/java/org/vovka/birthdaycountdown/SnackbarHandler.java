/*
 * *
 *  * Created by Vladimir Belov on 31.03.2025, 10:49
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 31.03.2025, 10:13
 *
 */

package org.vovka.birthdaycountdown;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Queue;

/**
 * SnackbarHandler - это пользовательский обработчик (Handler), ответственный за управление очередью
 * экземпляров {@link FluentSnackbar.Builder} и их последовательное отображение.
 * Он обрабатывает отображение новых снекбаров, закрытие существующих и
 * определяет приоритет важных снекбаров над менее важными.
 *
 * <p>
 *     В этом классе используется очередь ({@link Queue}) для управления снекбарами.
 *     Для предотвращения утечек памяти он использует WeakReference на экземпляр {@link FluentSnackbar}.
 * </p>
 *
 * <p>
 *     <b>Обработка сообщений:</b>
 *     Этот класс обрабатывает два типа сообщений:
 *     <ul>
 *         <li>{@link #MESSAGE_DISMISSED}: Указывает, что снекбар был закрыт.</li>
 *         <li>{@link #MESSAGE_NEW}: Указывает, что поступил новый запрос на отображение снекбара.</li>
 *     </ul>
 * </p>
 */
final class SnackbarHandler extends Handler {
    static final int MESSAGE_DISMISSED = 0;
    static final int MESSAGE_NEW = 1;

    private final Queue<FluentSnackbar.Builder> mQueue = new LinkedList<>();

    private final WeakReference<FluentSnackbar> mSnackbarManager;

    SnackbarHandler(FluentSnackbar manager) {
        mSnackbarManager = new WeakReference<>(manager);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_DISMISSED:
                mQueue.poll();
                showNext();
                break;

            case MESSAGE_NEW:
                onNewMessage(msg);
                break;
        }
    }

    private void onNewMessage(Message msg) {
        FluentSnackbar.Builder shownMessage = mQueue.peek();
        FluentSnackbar.Builder newMessage = (FluentSnackbar.Builder) msg.obj;

        if (shownMessage == null || !shownMessage.isImportant()) {
            mQueue.poll();
            mQueue.add(newMessage);

            showNext();
        } else {
            mQueue.add(newMessage);
        }
    }

    private void showNext() {
        removeLowPriorityMessages();

        if (!mQueue.isEmpty()) {

            final FluentSnackbar.Builder peek = mQueue.peek();
            if (peek != null) {
                if (peek.isImportant()) {
                    show(peek);
                } else {
                    mQueue.poll();
                }
            }

        }
    }

    private void show(FluentSnackbar.Builder message) {
        FluentSnackbar manager = mSnackbarManager.get();
        if (manager != null) {
            manager.showSnackbar(message);
        }
    }

    private void removeLowPriorityMessages() {
        while (hasItemsToRemove()) {
            mQueue.poll();
        }
    }

    public void setNotImportantByType(int type) {
        //if (!mQueue.isEmpty()) removeMessages(type);

        for (FluentSnackbar.Builder msg: mQueue) {
            if (msg.getType() == type) {
                msg.important(false);
            }
        }
/*
        while (!mQueue.isEmpty()) {
            mQueue.poll();
        }*/
    }

    private boolean hasItemsToRemove() {
        if (mQueue.isEmpty()) return false;

        boolean hasImportant = false;
        final FluentSnackbar.Builder peek = mQueue.peek();
        if (peek != null) hasImportant = peek.isImportant();

        boolean hasSingleNonImportant = mQueue.size() == 1 && !hasImportant;

        return !(hasImportant || hasSingleNonImportant);
    }
}