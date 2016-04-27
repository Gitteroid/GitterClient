package com.ne1c.gitteroid.events;

import com.ne1c.gitteroid.models.view.MessageViewModel;

public class UpdateMessageEvent {
    private MessageViewModel mMessageModel;

    public MessageViewModel getMessageModel() {
        return mMessageModel;
    }

    public void setMessageModel(MessageViewModel messageModel) {
        mMessageModel = messageModel;
    }
}
