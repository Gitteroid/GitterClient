package com.ne1c.gitteroid.api.responses;

import android.annotation.SuppressLint;

import com.google.gson.annotations.SerializedName;
import com.ne1c.gitteroid.models.data.RoomModel;

@SuppressLint("ParcelCreator")
public class JoinRoomResponse extends RoomModel {
    @SerializedName("error")
    private String mError;

    public String getError() {
        return mError;
    }
}
