package com.ne1c.developerstalk.models.view;

import android.os.Parcel;
import android.os.Parcelable;

public class RoomViewModel implements Parcelable {
    public String id;
    public String name;
    public String topic;
    public boolean oneToOne;
    public int unreadItems;
    public int userCount;
    public boolean mention;
    public boolean hide;
    public int listPosition = -1;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
        dest.writeString(this.topic);
        dest.writeByte(oneToOne ? (byte) 1 : (byte) 0);
        dest.writeInt(this.unreadItems);
        dest.writeInt(this.userCount);
        dest.writeByte(mention ? (byte) 1 : (byte) 0);
        dest.writeByte(hide ? (byte) 1 : (byte) 0);
        dest.writeInt(this.listPosition);
    }

    public RoomViewModel() {
    }

    protected RoomViewModel(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.topic = in.readString();
        this.oneToOne = in.readByte() != 0;
        this.unreadItems = in.readInt();
        this.userCount = in.readInt();
        this.mention = in.readByte() != 0;
        this.hide = in.readByte() != 0;
        this.listPosition = in.readInt();
    }

    public static final Parcelable.Creator<RoomViewModel> CREATOR = new Parcelable.Creator<RoomViewModel>() {
        @Override
        public RoomViewModel createFromParcel(Parcel source) {
            return new RoomViewModel(source);
        }

        @Override
        public RoomViewModel[] newArray(int size) {
            return new RoomViewModel[size];
        }
    };
}
