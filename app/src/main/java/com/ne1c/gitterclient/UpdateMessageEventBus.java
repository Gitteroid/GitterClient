package com.ne1c.gitterclient;

import com.ne1c.gitterclient.Models.MessageModel;

public class UpdateMessageEventBus {
    private MessageModel mMessageModel;

    public MessageModel getMessageModel() {
        return mMessageModel;
    }

    public void setMessageModel(MessageModel messageModel) {
        mMessageModel = messageModel;
    }
}
