package com.ne1c.developerstalk.events;

import com.ne1c.developerstalk.models.RoomModel;

public class RefreshMessagesRoomEvent {
    private RoomModel mRoomModel;

    public RefreshMessagesRoomEvent(RoomModel roomModel) {
        mRoomModel = roomModel;
    }

    public RoomModel getRoomModel() {
        return mRoomModel;
    }
}
