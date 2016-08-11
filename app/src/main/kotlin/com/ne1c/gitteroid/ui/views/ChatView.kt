package com.ne1c.gitteroid.ui.views

import android.support.annotation.StringRes

import com.ne1c.gitteroid.models.view.MessageViewModel

import java.util.ArrayList

interface ChatView {
    fun showMessages(messages: ArrayList<MessageViewModel>)

    fun showError(@StringRes resId: Int)

    fun showUpdateMessage(message: MessageViewModel)

    fun successReadMessages(first: Int, last: Int, roomId: String, i: Int)

    fun showLoadBeforeIdMessages(messages: ArrayList<MessageViewModel>)

    fun deliveredMessage(message: MessageViewModel)

    fun errorDeliveredMessage()

    fun showTopProgressBar()

    fun hideTopProgressBar()

    fun showListProgressBar()

    fun hideListProgress()

    fun joinToRoom()
}
