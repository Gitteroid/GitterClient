package com.ne1c.developerstalk.events;

import com.ne1c.developerstalk.models.data.MessageModel;
import com.ne1c.developerstalk.models.data.RoomModel;

public class NewMessageEvent {
    private MessageModel mMessageModel;
    private RoomModel mRoomModel;

    public NewMessageEvent(MessageModel messageModel, RoomModel roomModel) {
        mMessageModel = messageModel;
        mRoomModel = roomModel;
    }

    public MessageModel getMessage() {
        return mMessageModel;
    }

    public RoomModel getRoom() {
        return mRoomModel;
    }
}
