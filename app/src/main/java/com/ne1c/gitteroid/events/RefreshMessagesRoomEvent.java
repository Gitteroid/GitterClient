package com.ne1c.gitteroid.events;

import com.ne1c.gitteroid.models.view.RoomViewModel;

public class RefreshMessagesRoomEvent {
    private RoomViewModel mRoomModel;

    public RefreshMessagesRoomEvent(RoomViewModel roomModel) {
        mRoomModel = roomModel;
    }

    public RoomViewModel getRoomModel() {
        return mRoomModel;
    }
}
