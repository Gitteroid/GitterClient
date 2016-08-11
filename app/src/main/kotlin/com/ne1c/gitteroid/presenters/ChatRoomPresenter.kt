package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.models.MessageMapper
import com.ne1c.gitteroid.models.data.StatusMessage
import com.ne1c.gitteroid.models.data.UserModel
import com.ne1c.gitteroid.models.view.MessageViewModel
import com.ne1c.gitteroid.ui.views.ChatView
import com.ne1c.gitteroid.utils.RxSchedulersFactory
import com.ne1c.gitteroid.utils.Utils

import java.util.ArrayList

import javax.inject.Inject

import rx.Subscription
import rx.subscriptions.CompositeSubscription

class ChatRoomPresenter
@Inject
constructor(private val mSchedulersFactory: RxSchedulersFactory, private val mDataManger: DataManger) : BasePresenter<ChatView>() {
    private var mView: ChatView? = null

    private var mSubscriptions: CompositeSubscription? = null

    private val mCachedMessages = ArrayList<MessageViewModel>()

    override fun bindView(view: ChatView) {
        mView = view
        mSubscriptions = CompositeSubscription()
    }

    override fun unbindView() {
        mView = null
    }

    override fun onDestroy() {
        mSubscriptions!!.unsubscribe()
    }

    fun sendMessage(roomId: String, text: String) {
        if (!Utils.instance.isNetworkConnected) {
            mView!!.showError(R.string.no_network)
            return
        }

        val sub = mDataManger.sendMessage(roomId, text).subscribeOn(mSchedulersFactory.io()).observeOn(mSchedulersFactory.androidMainThread()).map<MessageViewModel>(Func1<MessageModel, MessageViewModel> { MessageMapper.mapToView(it) }).map<MessageViewModel>({ message ->
            mCachedMessages.add(message)
            message
        }).filter { messageViewModel -> mView != null }.subscribe(Action1<MessageViewModel> { mView!!.deliveredMessage(it) }
        ) { throwable -> mView!!.errorDeliveredMessage() }

        mSubscriptions!!.add(sub)
    }


    fun loadMessagesBeforeId(roomId: String, limit: Int, beforeId: String) {
        if (!Utils.instance.isNetworkConnected) {
            mView!!.showError(R.string.no_network)
            return
        }

        val sub = mDataManger.getMessagesBeforeId(roomId, limit, beforeId).subscribeOn(mSchedulersFactory.io()).observeOn(mSchedulersFactory.androidMainThread()).map<ArrayList<MessageViewModel>>(Func1<ArrayList<MessageModel>, ArrayList<MessageViewModel>> { MessageMapper.mapToView(it) }).map<ArrayList<MessageViewModel>>({ messages ->
            mCachedMessages.addAll(messages)
            mCachedMessages
        }).filter { messages -> mView != null }.subscribe(Action1<ArrayList<MessageViewModel>> { mView!!.showLoadBeforeIdMessages(it) }) { throwable -> mView!!.hideTopProgressBar() }

        mSubscriptions!!.add(sub)
    }

    // Load messages from network
    fun loadMessages(roomId: String, limit: Int) {
        if (!Utils.instance.isNetworkConnected) {
            mView!!.showError(R.string.no_network)
            return
        }

        mView!!.showListProgressBar()

        val sub = mDataManger.getMessages(roomId, limit, true).subscribeOn(mSchedulersFactory.io()).observeOn(mSchedulersFactory.androidMainThread()).map<ArrayList<MessageViewModel>>(Func1<ArrayList<MessageModel>, ArrayList<MessageViewModel>> { MessageMapper.mapToView(it) }).map<ArrayList<MessageViewModel>>({ messages ->
            mCachedMessages.clear()
            mCachedMessages.addAll(messages)
            messages
        }).filter { messages -> mView != null }.subscribe({ messages ->
            mView!!.showMessages(messages)
            mView!!.hideListProgress()
        }) { throwable ->
            mView!!.hideListProgress()
            mView!!.hideTopProgressBar()
        }

        mSubscriptions!!.add(sub)
    }

    fun updateMessages(roomId: String, messageId: String, text: String) {
        if (!Utils.instance.isNetworkConnected) {
            mView!!.showError(R.string.no_network)
            return
        }

        val sub = mDataManger.updateMessage(roomId, messageId, text).subscribeOn(mSchedulersFactory.io()).observeOn(mSchedulersFactory.androidMainThread()).filter { messageModel -> mView != null }.map<MessageViewModel>(Func1<MessageModel, MessageViewModel> { MessageMapper.mapToView(it) }).subscribe(Action1<MessageViewModel> { mView!!.showUpdateMessage(it) }) { throwable -> mView!!.showError(R.string.updated_error) }

        mSubscriptions!!.add(sub)
    }

    fun markMessageAsRead(first: Int, last: Int, roomId: String, ids: Array<String>) {
        if (!Utils.instance.isNetworkConnected) {
            return
        }

        val sub = mDataManger.readMessages(roomId, ids).subscribeOn(mSchedulersFactory.io()).observeOn(mSchedulersFactory.androidMainThread()).filter { success -> mView != null && success!! }.subscribe({ success -> mView!!.successReadMessages(first, last, roomId, ids.size - 1) }) { throwable -> }

        mSubscriptions!!.add(sub)
    }

    fun createSendMessage(text: String): MessageViewModel {
        val user = Utils.instance.userPref
        val message = MessageViewModel()

        message.sent = StatusMessage.SENDING.name
        message.fromUser = user
        message.text = text
        message.urls = ArrayList<Urls>()

        return message
    }

    fun joinToRoom(roomUri: String) {
        if (!Utils.instance.isNetworkConnected) {
            mView!!.showError(R.string.no_network)
            return
        }

        val sub = mDataManger.joinToRoom(roomUri).subscribeOn(mSchedulersFactory.io()).observeOn(mSchedulersFactory.androidMainThread()).filter { response -> mView != null }.subscribe({ responseBody ->
            if (responseBody.error == null) {
                mView!!.joinToRoom()
            } else {
                mView!!.showError(R.string.error)
            }
        }) { throwable -> mView!!.showError(R.string.error) }

        mSubscriptions!!.add(sub)
    }
}
