package com.ne1c.gitteroid.models.view;

import android.os.Parcel;
import android.os.Parcelable;

import com.ne1c.gitteroid.models.data.MessageModel;
import com.ne1c.gitteroid.models.data.UserModel;

import java.util.List;

public class MessageViewModel implements Parcelable {
    public String id;
    public String text;
    public String sent;
    public UserModel fromUser;
    public boolean unread;
    public List<MessageModel.Urls> urls;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.text);
        dest.writeString(this.sent);
        dest.writeParcelable(this.fromUser, flags);
        dest.writeByte(unread ? (byte) 1 : (byte) 0);
        dest.writeTypedList(urls);
    }

    public MessageViewModel() {
    }

    protected MessageViewModel(Parcel in) {
        this.id = in.readString();
        this.text = in.readString();
        this.sent = in.readString();
        this.fromUser = in.readParcelable(UserModel.class.getClassLoader());
        this.unread = in.readByte() != 0;
        this.urls = in.createTypedArrayList(MessageModel.Urls.CREATOR);
    }

    public static final Parcelable.Creator<MessageViewModel> CREATOR = new Parcelable.Creator<MessageViewModel>() {
        @Override
        public MessageViewModel createFromParcel(Parcel source) {
            return new MessageViewModel(source);
        }

        @Override
        public MessageViewModel[] newArray(int size) {
            return new MessageViewModel[size];
        }
    };
}
