package com.ne1c.gitteroid.models.data

import android.os.Parcel
import android.os.Parcelable

import java.util.*

open class RoomModel(
        var id: String = "",
        var name: String = "",
        var topic: String = "",
        var oneToOne: Boolean = false,
        var users: ArrayList<UserModel> = ArrayList(),
        var unreadItems: Int = 0,
        var userCount: Int = 0,
        var mentions: Int = 0,
        var lastAccessTime: String = "",
        var lurk: Boolean = false,
        var url: String = "",
        var githubType: String = "",
        var hide: Boolean = false,
        var v: Int = 0,
        var listPosition: Int = -1) : Parcelable {

    class SortedByName : Comparator<RoomModel> {
        override fun compare(r1: RoomModel, r2: RoomModel): Int {
            return r1.name.compareTo(r2.name)
        }
    }

    class SortedByPosition : Comparator<RoomModel> {
        override fun compare(r1: RoomModel, r2: RoomModel): Int {
            if (r1.listPosition > r2.listPosition) {
                return 1
            } else if (r1.listPosition < r2.listPosition) {
                return -1
            } else {
                return 0
            }
        }
    }

    constructor(source: Parcel) : this(source.readString(),
            source.readString(),
            source.readString(),
            1.equals(source.readInt()),
            source.createTypedArrayList(UserModel.CREATOR),
            source.readInt(),
            source.readInt(),
            source.readInt(),
            source.readString(),
            1.equals(source.readInt()),
            source.readString(),
            source.readString(),
            1.equals(source.readInt()),
            source.readInt(),
            source.readInt())

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(id)
        dest?.writeString(name)
        dest?.writeString(topic)
        dest?.writeInt((if (oneToOne) 1 else 0))
        dest?.writeTypedList(users)
        dest?.writeInt(unreadItems)
        dest?.writeInt(userCount)
        dest?.writeInt(mentions)
        dest?.writeString(lastAccessTime)
        dest?.writeInt((if (lurk) 1 else 0))
        dest?.writeString(url)
        dest?.writeString(githubType)
        dest?.writeInt((if (hide) 1 else 0))
        dest?.writeInt(v)
        dest?.writeInt(listPosition)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<RoomModel> = object : Parcelable.Creator<RoomModel> {
            override fun createFromParcel(source: Parcel): RoomModel = RoomModel(source)
            override fun newArray(size: Int): Array<RoomModel?> = arrayOfNulls(size)
        }
    }
}
