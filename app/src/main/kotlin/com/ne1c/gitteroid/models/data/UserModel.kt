package com.ne1c.gitteroid.models.data

import android.os.Parcel
import android.os.Parcelable

data class UserModel(var id: String = "",
                     var username: String = "",
                     var displayName: String = "",
                     var url: String = "",
                     var avatarUrlSmall: String = "",
                     var avatarUrlMedium: String = "") : Parcelable {
    constructor(source: Parcel) : this(source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString())

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(id)
        dest?.writeString(username)
        dest?.writeString(displayName)
        dest?.writeString(url)
        dest?.writeString(avatarUrlSmall)
        dest?.writeString(avatarUrlMedium)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<UserModel> = object : Parcelable.Creator<UserModel> {
            override fun createFromParcel(source: Parcel): UserModel = UserModel(source)
            override fun newArray(size: Int): Array<UserModel?> = arrayOfNulls(size)
        }
    }
}
