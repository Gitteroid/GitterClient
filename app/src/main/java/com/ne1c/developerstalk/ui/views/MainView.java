package com.ne1c.developerstalk.ui.views;

import android.support.annotation.StringRes;

import com.ne1c.developerstalk.models.data.UserModel;
import com.ne1c.developerstalk.models.view.RoomViewModel;

import java.util.ArrayList;

public interface MainView {
    void showRooms(ArrayList<RoomViewModel> rooms);

    void showProfile(UserModel user);

    void showError(@StringRes int resId);

    void leavedFromRoom();
}
