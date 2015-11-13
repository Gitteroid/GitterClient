package com.ne1c.developerstalk.Models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Comparator;

public class RoomModel implements Parcelable {
    public String id;
    public String name;
    public String topic;
    public boolean oneToOne;
    public ArrayList<UserModel> users = new ArrayList<>();
    public int unreadItems;
    public int userCount;
    public int mentions;
    public String lastAccessTime;
    public boolean lurk;
    public String url;
    public String githubType;
    public int v;


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
        dest.writeList(this.users);
        dest.writeInt(this.unreadItems);
        dest.writeInt(this.userCount);
        dest.writeInt(this.mentions);
        dest.writeString(this.lastAccessTime);
        dest.writeByte(lurk ? (byte) 1 : (byte) 0);
        dest.writeString(this.url);
        dest.writeString(this.githubType);
        dest.writeInt(this.v);
    }

    public RoomModel() {
    }

    protected RoomModel(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.topic = in.readString();
        this.oneToOne = in.readByte() != 0;
        this.users = new ArrayList<UserModel>();
        in.readList(this.users, ArrayList.class.getClassLoader());
        this.unreadItems = in.readInt();
        this.userCount = in.readInt();
        this.mentions = in.readInt();
        this.lastAccessTime = in.readString();
        this.lurk = in.readByte() != 0;
        this.url = in.readString();
        this.githubType = in.readString();
        this.v = in.readInt();
    }

    public static final Parcelable.Creator<RoomModel> CREATOR = new Parcelable.Creator<RoomModel>() {
        public RoomModel createFromParcel(Parcel source) {
            return new RoomModel(source);
        }

        public RoomModel[] newArray(int size) {
            return new RoomModel[size];
        }
    };

    public final static class TypeComparator implements Comparator<RoomModel> {
        @Override
        public int compare(RoomModel r1, RoomModel r2) {
            return r1.name.compareTo(r2.name);
        }
    }
}
