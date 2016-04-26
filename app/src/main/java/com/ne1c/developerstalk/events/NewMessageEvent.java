package com.ne1c.developerstalk.events;

import com.ne1c.developerstalk.models.view.MessageViewModel;
import com.ne1c.developerstalk.models.view.RoomViewModel;

public class NewMessageEvent {
    private MessageViewModel mMessageModel;
    private RoomViewModel mRoomModel;

    public NewMessageEvent(MessageViewModel messageModel, RoomViewModel roomModel) {
        mMessageModel = messageModel;
        mRoomModel = roomModel;
    }

    public MessageViewModel getMessage() {
        return mMessageModel;
    }

    public RoomViewModel getRoom() {
        return mRoomModel;
    }
}
