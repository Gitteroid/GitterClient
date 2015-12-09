package com.ne1c.developerstalk.models.eventBusModels;

import com.ne1c.developerstalk.models.MessageModel;

public class UpdateMessageEventBus {
    private MessageModel mMessageModel;

    public MessageModel getMessageModel() {
        return mMessageModel;
    }

    public void setMessageModel(MessageModel messageModel) {
        mMessageModel = messageModel;
    }
}
