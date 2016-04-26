package com.ne1c.developerstalk.presenters;

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.dataproviders.DataManger;
import com.ne1c.developerstalk.models.MessageMapper;
import com.ne1c.developerstalk.models.data.StatusMessage;
import com.ne1c.developerstalk.models.data.UserModel;
import com.ne1c.developerstalk.models.view.MessageViewModel;
import com.ne1c.developerstalk.ui.views.ChatView;
import com.ne1c.developerstalk.utils.RxSchedulersFactory;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;

import javax.inject.Inject;

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
    }

    @Override
    public void unbindView() {
        mView = null;
    }

    @Override
    public void onCreate() {
        mSubscriptions = new CompositeSubscription();
    }

    @Override
    public void onDestroy() {
        mSubscriptions.unsubscribe();
    }

    public void sendMessage(String roomId, String text) {
        if (!Utils.getInstance().isNetworkConnected()) {
            mView.showError(R.string.no_network);
            return;
        }

        Subscription sub = mDataManger.sendMessage(roomId, text)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .map(MessageMapper::mapToView)
                .subscribe(mView::deliveredMessage,
                        throwable -> mView.errorDeliveredMessage());

        mSubscriptions.add(sub);
    }


    public void loadMessagesBeforeId(String roomId, int limit, String beforeId) {
        if (!Utils.getInstance().isNetworkConnected()) {
            mView.showError(R.string.no_network);
            return;
        }

        Subscription sub = mDataManger.getMessagesBeforeId(roomId, limit, beforeId)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .map(MessageMapper::mapToView)
                .subscribe(mView::showLoadBeforeIdMessages, throwable -> {
                    mView.hideTopProgressBar();
                });

        mSubscriptions.add(sub);
    }

    // Load messages from network
    public void loadNetworkMessages(String roomId, int limit) {
        if (!Utils.getInstance().isNetworkConnected()) {
            mView.showError(R.string.no_network);
            return;
        }

        mView.showListProgressBar();

        Subscription sub = mDataManger.getMessages(roomId, limit, true)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .map(MessageMapper::mapToView)
                .subscribe(messages -> {
                    mView.showMessages(messages);
                    mView.hideListProgress();
                }, throwable -> {
                    mView.hideListProgress();
                    mView.hideTopProgressBar();
                });

        mSubscriptions.add(sub);
    }

    // Load messages from database, then load from network if possible
    public void loadMessages(String roomId, int limit) {
        final boolean[] fromNetwork = {false};
        final boolean[] fromDatabase = {false};

        Subscription sub = mDataManger.getMessages(roomId, limit, true)
                .map(MessageMapper::mapToView)
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
                        mView.showListProgressBar();
                    } else if (fromDatabase[0] && messages.size() > 0) {
                        mView.showTopProgressBar();
                    }

                    if (fromNetwork[0]) {
                        mView.hideListProgress();
                        mView.hideTopProgressBar();
                    }

                    if (messages.size() > 0) {
                        mView.showMessages(messages);
                    }
                }, throwable -> {
                    mView.hideListProgress();
                    mView.hideTopProgressBar();
                }, () -> {

                });

        mSubscriptions.add(sub);
    }

    // Load messages from database
    public void loadCachedMessages(String roomId) {
        Subscription sub = mDataManger.getMessages(roomId, 10, false)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .map(MessageMapper::mapToView)
                .subscribe(mView::showMessages, throwable -> {
                    mView.showError(R.string.error);
                });

        mSubscriptions.add(sub);
    }

    public void updateMessages(String roomId, String messageId, String text) {
        if (!Utils.getInstance().isNetworkConnected()) {
            mView.showError(R.string.no_network);
            return;
        }

        Subscription sub = mDataManger.updateMessage(roomId, messageId, text)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .map(MessageMapper::mapToView)
                .subscribe(mView::showUpdateMessage, throwable -> {
                    mView.showError(R.string.updated_error);
                });

        mSubscriptions.add(sub);
    }

    public void markMessageAsRead(int first, int last, String roomId, String[] ids) {
        if (!Utils.getInstance().isNetworkConnected()) {
            return;
        }

        Subscription sub = mDataManger.readMessages(roomId, ids)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(success -> {
                    if (success) {
                        mView.successReadMessages(first, last, roomId, ids.length - 1);
                    }
                }, throwable -> {});

        mSubscriptions.add(sub);
    }

    public MessageViewModel createSendMessage(String text) {
        UserModel user = Utils.getInstance().getUserPref();
        MessageViewModel message = new MessageViewModel();

        message.sent = StatusMessage.SENDING.name();
        message.fromUser = user;
        message.text = text;
        message.urls = new ArrayList<>();

        return message;
    }

    public void joinToRoom(String roomUri) {
        if (!Utils.getInstance().isNetworkConnected()) {
            mView.showError(R.string.no_network);
            return;
        }

        mDataManger.joinToRoom(roomUri)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(responseBody -> {
                    if (responseBody.getError() == null) {
                        mView.joinToRoom();
                    } else {
                        mView.showError(R.string.error);
                    }
                }, throwable -> {
                    mView.showError(R.string.error);
                });
    }
}
