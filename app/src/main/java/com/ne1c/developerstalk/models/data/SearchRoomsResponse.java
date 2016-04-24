package com.ne1c.developerstalk.models.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class SearchRoomsResponse {
    @SerializedName("results")
    private ArrayList<RoomModel> mResult;

    public ArrayList<RoomModel> getResult() {
        return mResult;
    }
}
