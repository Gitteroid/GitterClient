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

import rx.Observable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class ChatRoomPresenter extends BasePresenter<ChatView> {
    private ChatView mView;
    private DataManger mDataManger;
    private RxSchedulersFactory mSchedulersFactory;

    private CompositeSubscription mSubscriptions;

    @Inject
    public ChatRoomPresenter(RxSchedulersFactory factory, DataManger dataManger) {
        mSchedulersFactory = factory;
        mDataManger = dataManger;
    }

    @Override
    public void bindView(ChatView view) {
        mView = view;

        mSubscriptions = new CompositeSubscription();
    }

    @Override
    public void unbindView() {
        mSubscriptions.unsubscribe();
        mView = null;
    }

    public void sendMessage(String roomId, String text) {
        Subscription sub = mDataManger.sendMessage(roomId, text)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(mView::deliveredMessage,
                        throwable -> mView.errorDeliveredMessage());

        mSubscriptions.add(sub);
    }


    public void loadMessagesBeforeId(String roomId, int limit, String beforeId) {
        Subscription sub = mDataManger.getMessagesBeforeId(roomId, limit, beforeId)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(mView::successLoadBeforeId, throwable -> {
                    if (!throwable.getMessage().contains("Unable to resolve") &&
                            !throwable.getMessage().contains("timeout")) {
                        mView.showError(throwable.getMessage());
                    }
                });

        mSubscriptions.add(sub);
    }

    // Load messages from network
    public void loadNetworkMessages(String roomId, int limit) {
        mView.showListProgress();

        Subscription sub = mDataManger.getNetworkMessages(roomId, limit)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
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

        mSubscriptions.add(sub);
    }

    // Load messages from database, then load from network if possible
    public void loadMessages(String roomId, int limit) {
        final boolean[] fromNetwork = {false};
        final boolean[] fromDatabase = {false};

        Subscription sub = Observable.concat(mDataManger.getCachedMessages(roomId),
                mDataManger.getNetworkMessages(roomId, limit))
                .doOnNext(messages -> {
                    if (fromDatabase[0]) {
                        fromDatabase[0] = false;
                        fromNetwork[0] = true;
                    } else {
                        fromDatabase[0] = true;
                    }
                })
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(messages -> {
                    if (fromDatabase[0] && messages.size() == 0) {
                        mView.showListProgress();
                    } else if (fromDatabase[0] && messages.size() > 0) {
                        mView.showTopProgressBar();
                    }

                    if (fromNetwork[0]) {
                        mView.hideListProgress();
                        mView.hideTopProgressBar();
                    }

                    mView.showMessages(messages);
                });

        mSubscriptions.add(sub);
    }

    // Load messages from database
    public void loadCachedMessages(String roomId) {
        Subscription sub = mDataManger.getCachedMessages(roomId)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(mView::showMessages, throwable -> {
                    mView.showError(throwable.getMessage());
                });

        mSubscriptions.add(sub);
    }

    public void updateMessages(String roomId, String messageId, String text) {
        Subscription sub = mDataManger.updateMessage(roomId, messageId, text)
                .subscribeOn(mSchedulersFactory.io())
                .map(messageModel -> {
                    mDataManger.insertMessageToDb(messageModel, roomId);
                    return messageModel;
                })
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(mView::successUpdate, throwable -> {
                    mView.showError(mView.getAppContext().getString(R.string.updated_error));
                });

        mSubscriptions.add(sub);
    }

    public void insertMessageToDb(MessageModel model, String id) {
        mDataManger.insertMessageToDb(model, id);
    }

    public void markMessageAsRead(int first, int last, String roomId, String[] ids) {
        if (!Utils.getInstance().isNetworkConnected()) {
            return;
        }

        Subscription sub = mDataManger.readMessages(roomId, ids)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(response -> {
                    mView.successRead(first, last, roomId, ids.length - 1);
                });

        mSubscriptions.add(sub);
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
