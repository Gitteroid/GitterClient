package com.ne1c.gitteroid.models.data

import android.os.Parcel
import android.os.Parcelable

class UserModel : Parcelable {
    var id = ""
    var username = ""
    var displayName = ""
    var url = ""
    var avatarUrlSmall = ""
    var avatarUrlMedium = ""

    constructor() {
    }

    protected constructor(`in`: Parcel) {
        this.id = `in`.readString()
        this.username = `in`.readString()
        this.displayName = `in`.readString()
        this.url = `in`.readString()
        this.avatarUrlSmall = `in`.readString()
        this.avatarUrlMedium = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.id)
        dest.writeString(this.username)
        dest.writeString(this.displayName)
        dest.writeString(this.url)
        dest.writeString(this.avatarUrlSmall)
        dest.writeString(this.avatarUrlMedium)
    }

    companion object {

        val CREATOR: Parcelable.Creator<UserModel> = object : Parcelable.Creator<UserModel> {
            override fun createFromParcel(source: Parcel): UserModel {
                return UserModel(source)
            }

            override fun newArray(size: Int): Array<UserModel> {
                return arrayOfNulls(size)
            }
        }
    }
}
