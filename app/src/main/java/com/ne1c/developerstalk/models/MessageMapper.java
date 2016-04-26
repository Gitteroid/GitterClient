package com.ne1c.developerstalk.models;

import com.ne1c.developerstalk.models.data.MessageModel;
import com.ne1c.developerstalk.models.view.MessageViewModel;

import java.util.ArrayList;

public class MessageMapper {
    public static MessageViewModel mapToView(MessageModel model) {
        MessageViewModel viewModel = new MessageViewModel();

        viewModel.id = model.id;
        viewModel.text = model.text;
        viewModel.sent = model.sent;
        viewModel.fromUser = model.fromUser;
        viewModel.unread = model.unread;
        viewModel.urls = model.urls;

        return viewModel;
    }

    public static ArrayList<MessageViewModel> mapToView(ArrayList<MessageModel> messageModels) {
        ArrayList<MessageViewModel> list = new ArrayList<>();

        for (MessageModel messageModel : messageModels) {
            list.add(MessageMapper.mapToView(messageModel));
        }

        return list;
    }
}
