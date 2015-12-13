package com.ne1c.developerstalk.events;

import com.ne1c.developerstalk.models.MessageModel;

public class UpdateMessageEvent {
    private MessageModel mMessageModel;

    public MessageModel getMessageModel() {
        return mMessageModel;
    }

    public void setMessageModel(MessageModel messageModel) {
        mMessageModel = messageModel;
    }
}
