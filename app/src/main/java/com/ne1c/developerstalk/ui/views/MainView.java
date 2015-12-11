package com.ne1c.developerstalk.ui.views;

import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.models.UserModel;

import java.util.ArrayList;

public interface MainView extends BaseView {
    void showRooms(ArrayList<RoomModel> rooms);

    void showProfile(UserModel user);

    void leavedFromRoom();
}
