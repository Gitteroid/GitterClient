package com.ne1c.gitteroid.ui.views

import android.support.annotation.StringRes

import com.ne1c.gitteroid.models.view.RoomViewModel

import java.util.ArrayList

interface RoomsListView {
    fun showRooms(rooms: List<RoomViewModel>, fresh: Boolean)

    fun showError(@StringRes resId: Int)

    fun showDialog()

    fun dismissDialog()

    fun errorSearch()

    fun resultSearch(rooms: ArrayList<RoomViewModel>)

    fun resultSearchWithOffset(rooms: ArrayList<RoomViewModel>)
}
