package com.ne1c.gitterclient.Models;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageModel implements Parcelable {
    public String id;
    public String text;
    public String html;
    public String sent;
    public String editedAt;
    public UserModel fromUser;
    public boolean unread;
    public int readBy;
    public List<Urls> urls;
    public ArrayList<Mentions> mentions;
    //public String issues;
    public int v;

    public static class Mentions implements Parcelable {
        public String screenName;
        public String userId;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.screenName);
            dest.writeString(this.userId);
        }

        public Mentions() {
        }

        protected Mentions(Parcel in) {
            this.screenName = in.readString();
            this.userId = in.readString();
        }

        public static final Creator<Mentions> CREATOR = new Creator<Mentions>() {
            public Mentions createFromParcel(Parcel source) {
                return new Mentions(source);
            }

            public Mentions[] newArray(int size) {
                return new Mentions[size];
            }
        };
    }

    public static class Urls implements Parcelable {
        public String url;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.url);
        }

        public Urls() {
        }

        protected Urls(Parcel in) {
            this.url = in.readString();
        }

        public static final Creator<Urls> CREATOR = new Creator<Urls>() {
            public Urls createFromParcel(Parcel source) {
                return new Urls(source);
            }

            public Urls[] newArray(int size) {
                return new Urls[size];
            }
        };
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
        dest.writeParcelable(this.fromUser, 0);
        dest.writeByte(unread ? (byte) 1 : (byte) 0);
        dest.writeInt(this.readBy);
        dest.writeTypedList(urls);
        dest.writeTypedList(mentions);
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
        this.urls = in.createTypedArrayList(Urls.CREATOR);
        this.mentions = in.createTypedArrayList(Mentions.CREATOR);
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
