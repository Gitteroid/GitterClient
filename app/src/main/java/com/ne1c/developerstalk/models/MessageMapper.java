package com.ne1c.developerstalk.models;

import com.ne1c.developerstalk.models.data.MessageModel;
import com.ne1c.developerstalk.models.view.MessageViewModel;

public class MessageMapper {
    public static MessageViewModel mapToView(MessageModel model) {
        MessageViewModel viewModel = new MessageViewModel();

        viewModel.id = model.id;
        viewModel.text = model.id;
        viewModel.sent = model.id;
        viewModel.fromUser = model.fromUser;
        viewModel.unread = model.unread;
        viewModel.urls = model.urls;

        return viewModel;
    }
}
