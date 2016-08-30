package com.ne1c.gitteroid.models

import com.ne1c.gitteroid.models.data.MessageModel
import com.ne1c.gitteroid.models.view.MessageViewModel

import java.util.ArrayList

object MessageMapper {
    fun mapToView(model: MessageModel): MessageViewModel {
        val viewModel = MessageViewModel()

        viewModel.id = model.id
        viewModel.text = model.text
        viewModel.sent = model.sent
        viewModel.fromUser = model.fromUser
        viewModel.unread = model.unread
        viewModel.urls = model.urls

        return viewModel
    }

    fun mapToView(messageModels: ArrayList<MessageModel>): ArrayList<MessageViewModel> {
        val list = ArrayList<MessageViewModel>()

        for (messageModel in messageModels) {
            list.add(MessageMapper.mapToView(messageModel))
        }

        return list
    }
}
