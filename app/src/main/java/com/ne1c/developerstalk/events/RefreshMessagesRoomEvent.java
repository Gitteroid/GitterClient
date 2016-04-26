package com.ne1c.developerstalk.events;

import com.ne1c.developerstalk.models.view.RoomViewModel;

public class RefreshMessagesRoomEvent {
    private RoomViewModel mRoomModel;

    public RefreshMessagesRoomEvent(RoomViewModel roomModel) {
        mRoomModel = roomModel;
    }

    public RoomViewModel getRoomModel() {
        return mRoomModel;
    }
}
