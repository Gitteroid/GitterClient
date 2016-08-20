package com.ne1c.gitteroid.ui.views

import android.support.annotation.StringRes
import com.ne1c.gitteroid.models.view.RoomViewModel
import java.util.*

interface SearchRoomsView {
    fun showError(@StringRes resId: Int)

    fun showDialog()

    fun dismissDialog()

    fun errorSearch()

    fun resultSearch(rooms: ArrayList<RoomViewModel>)
}
