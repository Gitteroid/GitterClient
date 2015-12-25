package com.ne1c.developerstalk.presenters;

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.models.StatusMessage;
import com.ne1c.developerstalk.models.UserModel;
import com.ne1c.developerstalk.services.DataManger;
import com.ne1c.developerstalk.ui.views.ChatView;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ChatRoomPresenter extends BasePresenter<ChatView> {
    private ChatView mView;
    private DataManger mDataManger;

    @Override
    public void bindView(ChatView view) {
        mView = view;

        mDataManger = new DataManger(mView.getAppContext());
    }

    @Override
    public void unbindView() {
        mView = null;
    }

    public void sendMessage(String roomId, String text) {
        mDataManger.sendMessage(roomId, text)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mView::deliveredMessage,
                        throwable -> mView.errorDeliveredMessage());
    }


    public void loadMessagesBeforeId(String roomId, int limit, String beforeId) {
        mDataManger.getMessagesBeforeId(roomId, limit, beforeId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mView::successLoadBeforeId, throwable -> {
                    mView.showError(throwable.getMessage());
                });
    }

    public void loadMessages(String roomId, int limit) {
        mView.showListProgress();

        mDataManger.getMessages(roomId, limit)
                .subscribeOn(Schedulers.io())
                .map(messageModels -> {
                    mDataManger.insertMessagesToDb(messageModels, roomId);
                    return messageModels;
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(mView::showMessages, throwable -> {
                    mView.showError(throwable.getMessage());
                });
    }

    public void loadCachedMessages(String roomId) {
        mDataManger.getCachedMessages(roomId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mView::showMessages, throwable -> {
                    mView.showError(throwable.getMessage());
                });
    }

    public void updateMessages(String roomId, String messageId, String text) {
        mDataManger.updateMessage(roomId, messageId, text)
                .subscribeOn(Schedulers.io())
                .map(messageModel -> {
                    mDataManger.insertMessageToDb(messageModel, roomId);
                    return messageModel;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mView::successUpdate, throwable -> {
                    mView.showError(mView.getAppContext().getString(R.string.updated_error));
                });
    }

    public void insertMessageToDb(MessageModel model, String id) {
        mDataManger.insertMessageToDb(model, id);
    }

    public void markMessageAsRead(int first, int last, String roomId, String[] ids) {
        mDataManger.readMessages(roomId, ids)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    mView.successRead(first, last, roomId, ids.length - 1);
                }, throwable -> {
                    mView.showError(throwable.getMessage());
                });
    }

    public MessageModel createSendMessage(String text) {
        UserModel user = Utils.getInstance().getUserPref();
        MessageModel message = new MessageModel();

        message.sent = StatusMessage.SENDING.name();
        message.fromUser = user;
        message.text = text;
        message.urls = new ArrayList<>();

        return message;
    }
}
