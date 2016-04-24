package com.ne1c.developerstalk.ui.views;

import android.support.annotation.StringRes;

import com.ne1c.developerstalk.models.data.RoomModel;

import java.util.ArrayList;
import java.util.List;

public interface RoomsListView {
    void showRooms(List<RoomModel> rooms);

    void showError(@StringRes int resId);

    void showDialog();

    void dismissDialog();

    void errorSearch();

    void resultSearch(ArrayList<RoomModel> rooms);

    void resultSearchWithOffset(ArrayList<RoomModel> rooms);
}
