package com.ne1c.developerstalk.ui.views;

import com.ne1c.developerstalk.models.MessageModel;

import java.util.ArrayList;

public interface ChatView extends BaseView {
    void showMessages(ArrayList<MessageModel> messages);

    void showError(String error);

    void showUpdateMessage(MessageModel message);

    void successReadMessages(int first, int last, String roomId, int i);

    void showLoadBeforeIdMessages(ArrayList<MessageModel> messages);

    void deliveredMessage(MessageModel message);

    void errorDeliveredMessage();

    void showTopProgressBar();

    void hideTopProgressBar();

    void showListProgressBar();

    void hideListProgress();

    void joinToRoom();
}
