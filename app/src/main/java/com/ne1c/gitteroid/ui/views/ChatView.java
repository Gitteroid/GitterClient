package com.ne1c.gitteroid.ui.views;

import android.support.annotation.StringRes;

import com.ne1c.gitteroid.models.view.MessageViewModel;

import java.util.ArrayList;

public interface ChatView{
    void showMessages(ArrayList<MessageViewModel> messages);

    void showError(@StringRes int resId);

    void showUpdateMessage(MessageViewModel message);

    void successReadMessages(int first, int last, String roomId, int i);

    void showLoadBeforeIdMessages(ArrayList<MessageViewModel> messages);

    void deliveredMessage(MessageViewModel message);

    void errorDeliveredMessage();

    void showTopProgressBar();

    void hideTopProgressBar();

    void showListProgressBar();

    void hideListProgress();

    void joinToRoom();
}
