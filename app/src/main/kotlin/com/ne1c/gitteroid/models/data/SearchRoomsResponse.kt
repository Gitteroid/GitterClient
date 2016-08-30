package com.ne1c.gitteroid.models.data

import com.google.gson.annotations.SerializedName

import java.util.ArrayList

class SearchRoomsResponse {
    @SerializedName("results")
    val result: ArrayList<RoomModel>? = null
}
