package com.ne1c.developerstalk.ui.views;

import android.graphics.Bitmap;
import android.support.annotation.StringRes;

import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.models.UserModel;

import java.util.ArrayList;

public interface MainView {
    void showRooms(ArrayList<RoomModel> rooms);

    void showProfile(UserModel user);

    void showError(@StringRes int resId);

    void leavedFromRoom();

    void updatePhoto(Bitmap photo);
}
