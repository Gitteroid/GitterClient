package com.ne1c.developerstalk.api.responses;

import android.annotation.SuppressLint;

import com.google.gson.annotations.SerializedName;
import com.ne1c.developerstalk.models.RoomModel;

@SuppressLint("ParcelCreator")
public class JoinRoomResponse extends RoomModel {
    @SerializedName("error")
    private String mError;

    public String getError() {
        return mError;
    }
}
