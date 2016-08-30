package com.ne1c.gitteroid.api.responses

import android.annotation.SuppressLint
import com.google.gson.annotations.SerializedName
import com.ne1c.gitteroid.models.data.RoomModel

@SuppressLint("ParcelCreator")
class JoinRoomResponse : RoomModel(){
    @SerializedName("error")
    val error: String? = null
}
