package com.ne1c.gitteroid.presenters;

import com.ne1c.gitteroid.R;
import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.models.MessageMapper;
import com.ne1c.gitteroid.models.data.StatusMessage;
import com.ne1c.gitteroid.models.data.UserModel;
import com.ne1c.gitteroid.models.view.MessageViewModel;
import com.ne1c.gitteroid.ui.views.ChatView;
import com.ne1c.gitteroid.utils.RxSchedulersFactory;
import com.ne1c.gitteroid.utils.Utils;

import java.util.ArrayList;

import javax.inject.Inject;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class ChatRoomPresenter extends BasePresenter<ChatView> {
    private ChatView mView;
    private DataManger mDataManger;
    private RxSchedulersFactory mSchedulersFactory;

    private CompositeSubscription mSubscriptions;

    private ArrayList<MessageViewModel> mCachedMessages = new ArrayList<>();

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
        mView = null;
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
                .map(message -> {
                    mCachedMessages.add(message);
                    return message;
                })
                .filter(messageViewModel -> mView != null)
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
                .map(messages -> {
                    mCachedMessages.addAll(messages);
                    return mCachedMessages;
                })
                .filter(messages -> mView != null)
                .subscribe(mView::showLoadBeforeIdMessages, throwable -> {
                    mView.hideTopProgressBar();
                });

        mSubscriptions.add(sub);
    }

    // Load messages from network
    public void loadMessages(String roomId, int limit) {
        if (!Utils.getInstance().isNetworkConnected()) {
            mView.showError(R.string.no_network);
            return;
        }

        mView.showListProgressBar();

        Subscription sub = mDataManger.getMessages(roomId, limit, true)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .map(MessageMapper::mapToView)
                .map(messages -> {
                    mCachedMessages.clear();
                    mCachedMessages.addAll(messages);
                    return messages;
                })
                .filter(messages -> mView != null)
                .subscribe(messages -> {
                    mView.showMessages(messages);
                    mView.hideListProgress();
                }, throwable -> {
                    mView.hideListProgress();
                    mView.hideTopProgressBar();
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
                .filter(messageModel -> mView != null)
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
                .filter(success -> mView != null && success)
                .subscribe(success -> {
                    mView.successReadMessages(first, last, roomId, ids.length - 1);
                }, throwable -> {
                });

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

        Subscription sub = mDataManger.joinToRoom(roomUri)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .filter(response -> mView != null)
                .subscribe(responseBody -> {
                    if (responseBody.getError() == null) {
                        mView.joinToRoom();
                    } else {
                        mView.showError(R.string.error);
                    }
                }, throwable -> {
                    mView.showError(R.string.error);
                });

        mSubscriptions.add(sub);
    }
}
