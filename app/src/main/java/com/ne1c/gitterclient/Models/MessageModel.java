package com.ne1c.gitterclient.Models;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class MessageModel implements Parcelable {
    public String id;
    public String text;
    public String html;
    public String sent;
    public String editedAt;
    public UserModel fromUser;
    public boolean unread;
    public int readBy;
    //public List<String> urls;
    public List<Mentions> mentions;
    //public String issues;
    public int v;

    public static class Mentions {
        public String screenName;
        public String userId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.text);
        dest.writeString(this.html);
        dest.writeString(this.sent);
        dest.writeString(this.editedAt);
        dest.writeParcelable(this.fromUser, flags);
        dest.writeByte(unread ? (byte) 1 : (byte) 0);
        dest.writeInt(this.readBy);
        dest.writeList(this.mentions);
        dest.writeInt(this.v);
    }

    public MessageModel() {
    }

    protected MessageModel(Parcel in) {
        this.id = in.readString();
        this.text = in.readString();
        this.html = in.readString();
        this.sent = in.readString();
        this.editedAt = in.readString();
        this.fromUser = in.readParcelable(UserModel.class.getClassLoader());
        this.unread = in.readByte() != 0;
        this.readBy = in.readInt();
        this.mentions = new ArrayList<Mentions>();
        in.readList(this.mentions, List.class.getClassLoader());
        this.v = in.readInt();
    }

    public static final Parcelable.Creator<MessageModel> CREATOR = new Parcelable.Creator<MessageModel>() {
        public MessageModel createFromParcel(Parcel source) {
            return new MessageModel(source);
        }

        public MessageModel[] newArray(int size) {
            return new MessageModel[size];
        }
    };
}
