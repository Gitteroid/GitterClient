package com.ne1c.gitteroid.models.view

import android.os.Parcel
import android.os.Parcelable

data class RoomViewModel(
        var id: String = "",
        var name: String = "",
        var topic: String = "",
        var oneToOne: Boolean = false,
        var unreadItems: Int = 0,
        var userCount: Int = 0,
        var mention: Boolean = false,
        var hide: Boolean = false,
        var listPosition: Int = -1) : Parcelable {
    fun getAvatarUrl(): String {
        if (name.contains("/")) {
            return "https://avatars.githubusercontent.com/" + name.substring(0, name.indexOf("/"))
        } else {
            return "https://avatars.githubusercontent.com/" + name
        }
    }

    constructor(source: Parcel) : this(source.readString(),
            source.readString(),
            source.readString(),
            1.equals(source.readInt()),
            source.readInt(),
            source.readInt(),
            1.equals(source.readInt()),
            1.equals(source.readInt()),
            source.readInt())

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(id)
        dest?.writeString(name)
        dest?.writeString(topic)
        dest?.writeInt((if (oneToOne) 1 else 0))
        dest?.writeInt(unreadItems)
        dest?.writeInt(userCount)
        dest?.writeInt((if (mention) 1 else 0))
        dest?.writeInt((if (hide) 1 else 0))
        dest?.writeInt(listPosition)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<RoomViewModel> = object : Parcelable.Creator<RoomViewModel> {
            override fun createFromParcel(source: Parcel): RoomViewModel = RoomViewModel(source)
            override fun newArray(size: Int): Array<RoomViewModel?> = arrayOfNulls(size)
        }
    }
}
