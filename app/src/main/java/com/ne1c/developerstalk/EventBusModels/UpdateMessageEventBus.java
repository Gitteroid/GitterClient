package com.ne1c.developerstalk.EventBusModels;

import com.ne1c.developerstalk.Models.MessageModel;

public class UpdateMessageEventBus {
    private MessageModel mMessageModel;

    public MessageModel getMessageModel() {
        return mMessageModel;
    }

    public void setMessageModel(MessageModel messageModel) {
        mMessageModel = messageModel;
    }
}
