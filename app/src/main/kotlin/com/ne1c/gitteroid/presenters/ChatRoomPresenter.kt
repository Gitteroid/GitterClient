package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.base.ExecutorService
import com.ne1c.gitteroid.di.base.NetworkService
import com.ne1c.gitteroid.models.MessageMapper
import com.ne1c.gitteroid.models.data.MessageModel
import com.ne1c.gitteroid.models.data.StatusMessage
import com.ne1c.gitteroid.models.view.MessageViewModel
import com.ne1c.gitteroid.ui.views.ChatView
import com.ne1c.rainbowmvp.base.BasePresenter
import rx.subscriptions.CompositeSubscription
import java.util.*

class ChatRoomPresenter(private val executor: ExecutorService,
                        private val dataManager: DataManger,
                        private val networkService: NetworkService) : BasePresenter<ChatView>() {
    companion object {
        val TAG: String = ChatRoomPresenter::class.java.simpleName
    }

    private var mSubscriptions: CompositeSubscription? = null
    private val mCachedMessages = ArrayList<MessageViewModel>()

    override fun bindView(view: ChatView) {
        super.bindView(view)
        mSubscriptions = CompositeSubscription()
    }

    override fun unbindView() {
        mView = null
    }

    override fun onDestroy() {
        mSubscriptions?.unsubscribe()
    }

    fun sendMessage(roomId: String, text: String) {
        if (!networkService.isConnected()) {
            mView?.showError(R.string.no_network)
            return
        }

        val sub = dataManager.sendMessage(roomId, text)
                .map { MessageMapper.mapToView(it) }
                .map {
                    mCachedMessages.add(it)
                    return@map it
                }
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .subscribe({ mView?.deliveredMessage(it) },
                        { mView?.errorDeliveredMessage() })

        mSubscriptions?.add(sub)
    }


    fun loadMessagesBeforeId(roomId: String, limit: Int, beforeId: String) {
        if (!networkService.isConnected()) {
            mView?.showError(R.string.no_network)
            return
        }

        val sub = dataManager.getMessagesBeforeId(roomId, limit, beforeId)
                .map { MessageMapper.mapToView(it) }
                .map {
                    mCachedMessages.addAll(it)
                    return@map it
                }
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .subscribe({ mView?.showLoadBeforeIdMessages(it) },
                        { throwable -> mView?.hideTopProgressBar() })

        mSubscriptions?.add(sub)
    }

    // Load messages from network
    fun loadMessages(roomId: String, limit: Int) {
        if (!networkService.isConnected()) {
            mView?.showError(R.string.no_network)
            return
        }

        mView?.showListProgressBar()

        val sub = dataManager.getMessages(roomId, limit, true)
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .map { MessageMapper.mapToView(it) }
                .map { messages ->
                    mCachedMessages.clear()
                    mCachedMessages.addAll(messages)
                    return@map messages
                }.subscribe({
            mView?.showMessages(it)
            mView?.hideListProgress()
        }) { throwable ->
            mView?.hideListProgress()
            mView?.hideTopProgressBar()
        }

        mSubscriptions?.add(sub)
    }

    fun updateMessages(roomId: String, messageId: String, text: String) {
        if (!networkService.isConnected()) {
            mView?.showError(R.string.no_network)
            return
        }

        val sub = dataManager.updateMessage(roomId, messageId, text)
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .map { MessageMapper.mapToView(it) }
                .subscribe({ mView?.showUpdateMessage(it) },
                        { throwable -> mView?.showError(R.string.updated_error) })

        mSubscriptions?.add(sub)
    }

    fun markMessageAsRead(first: Int, last: Int, roomId: String, ids: Array<String>) {
        if (!networkService.isConnected()) {
            return
        }

        val sub = dataManager.readMessages(roomId, ids)
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .filter { it }
                .subscribe({ mView?.successReadMessages(first, last, roomId, ids.size - 1) },
                        { throwable -> })

        mSubscriptions?.add(sub)
    }

    fun createSendMessage(text: String): MessageViewModel {
        val user = dataManager.getUser()
        val message = MessageViewModel()

        message.sent = StatusMessage.SENDING.name
        message.fromUser = user
        message.text = text
        message.urls = ArrayList<MessageModel.Urls>()

        return message
    }

    fun joinToRoom(roomUri: String) {
        if (!networkService.isConnected()) {
            mView?.showError(R.string.no_network)
            return
        }

        val sub = dataManager.joinToRoom(roomUri)
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .subscribe({ responseBody ->
                    if (responseBody.error === null) {
                        mView?.joinToRoom()
                    } else {
                        mView?.showError(R.string.error)
                    }
                }) { throwable -> mView?.showError(R.string.error) }

        mSubscriptions?.add(sub)
    }
}
