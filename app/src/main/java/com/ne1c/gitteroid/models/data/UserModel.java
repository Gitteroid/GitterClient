package com.ne1c.gitteroid.models.data;

import android.os.Parcel;
import android.os.Parcelable;

public class UserModel implements Parcelable {
    public String id = "";
    public String username = "";
    public String displayName = "";
    public String url = "";
    public String avatarUrlSmall = "";
    public String avatarUrlMedium = "";

    public UserModel() {
    }

    protected UserModel(Parcel in) {
        this.id = in.readString();
        this.username = in.readString();
        this.displayName = in.readString();
        this.url = in.readString();
        this.avatarUrlSmall = in.readString();
        this.avatarUrlMedium = in.readString();
    }

    public static final Parcelable.Creator<UserModel> CREATOR = new Parcelable.Creator<UserModel>() {
        public UserModel createFromParcel(Parcel source) {
            return new UserModel(source);
        }

        public UserModel[] newArray(int size) {
            return new UserModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.username);
        dest.writeString(this.displayName);
        dest.writeString(this.url);
        dest.writeString(this.avatarUrlSmall);
        dest.writeString(this.avatarUrlMedium);
    }
}
