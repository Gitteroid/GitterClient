package com.ne1c.gitteroid.ui.views

import android.support.annotation.StringRes

import com.ne1c.gitteroid.models.data.UserModel
import com.ne1c.gitteroid.models.view.RoomViewModel

import java.util.ArrayList

interface MainView {
    fun showRooms(rooms: ArrayList<RoomViewModel>)

    fun showProfile(user: UserModel)

    fun showError(@StringRes resId: Int)

    fun leavedFromRoom()

    fun saveAllRooms(rooms: ArrayList<RoomViewModel>)
}
