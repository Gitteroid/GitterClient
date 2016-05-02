package com.ne1c.gitteroid.events;

import com.ne1c.gitteroid.models.view.MessageViewModel;
import com.ne1c.gitteroid.models.view.RoomViewModel;

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
