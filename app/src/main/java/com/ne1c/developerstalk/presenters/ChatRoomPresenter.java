package com.ne1c.developerstalk.presenters;

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.models.StatusMessage;
import com.ne1c.developerstalk.models.UserModel;
import com.ne1c.developerstalk.services.DataManger;
import com.ne1c.developerstalk.ui.views.ChatView;
import com.ne1c.developerstalk.utils.RxSchedulersFactory;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;

import javax.inject.Inject;

public class ChatRoomPresenter extends BasePresenter<ChatView> {
    private ChatView mView;
    private DataManger mDataManger;
    private RxSchedulersFactory mSchedulersFactory;

    @Inject
    public ChatRoomPresenter(RxSchedulersFactory factory, DataManger dataManger) {
        mSchedulersFactory = factory;
        mDataManger = dataManger;
    }

    @Override
    public void bindView(ChatView view) {
        mView = view;
    }

    @Override
    public void unbindView() {
        mView = null;
    }

    public void sendMessage(String roomId, String text) {
        mDataManger.sendMessage(roomId, text)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(mView::deliveredMessage,
                        throwable -> mView.errorDeliveredMessage());
    }


    public void loadMessagesBeforeId(String roomId, int limit, String beforeId) {
        mDataManger.getMessagesBeforeId(roomId, limit, beforeId)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(mView::successLoadBeforeId, throwable -> {
                    if (!throwable.getMessage().contains("Unable to resolve") &&
                            !throwable.getMessage().contains("timeout")) {
                        mView.showError(throwable.getMessage());
                    }
                });
    }

    public void loadMessages(String roomId, int limit) {
        mView.showListProgress();

        mDataManger.getMessages(roomId, limit)
                .subscribeOn(mSchedulersFactory.io())
                .map(messageModels -> {
                    mDataManger.insertMessagesToDb(messageModels, roomId);
                    return messageModels;
                }).observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(messages -> {
                    mView.showMessages(messages);
                    mView.hideListProgress();
                }, throwable -> {
                    mView.hideListProgress();
                    if (!throwable.getMessage().contains("Unable to resolve") &&
                            !throwable.getMessage().contains("timeout")) {
                        mView.showError(throwable.getMessage());
                    }
                });
    }

    public void loadCachedMessages(String roomId) {
        mDataManger.getCachedMessages(roomId)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(mView::showMessages, throwable -> {
                    mView.showError(throwable.getMessage());
                });
    }

    public void updateMessages(String roomId, String messageId, String text) {
        mDataManger.updateMessage(roomId, messageId, text)
                .subscribeOn(mSchedulersFactory.io())
                .map(messageModel -> {
                    mDataManger.insertMessageToDb(messageModel, roomId);
                    return messageModel;
                })
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(mView::successUpdate, throwable -> {
                    mView.showError(mView.getAppContext().getString(R.string.updated_error));
                });
    }

    public void insertMessageToDb(MessageModel model, String id) {
        mDataManger.insertMessageToDb(model, id);
    }

    public void markMessageAsRead(int first, int last, String roomId, String[] ids) {
        if (!Utils.getInstance().isNetworkConnected()) {
            return;
        }

        mDataManger.readMessages(roomId, ids)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(response -> {
                    mView.successRead(first, last, roomId, ids.length - 1);
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
