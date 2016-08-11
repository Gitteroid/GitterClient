package com.ne1c.gitteroid.models.view

import android.os.Parcel
import android.os.Parcelable

class RoomViewModel : Parcelable {
    var id: String
    var name: String
    var topic: String
    var oneToOne: Boolean = false
    var unreadItems: Int = 0
    var userCount: Int = 0
    var mention: Boolean = false
    var hide: Boolean = false
    var listPosition = -1

    val avatarUrl: String
        get() {
            if (name.contains("/")) {
                return "https://avatars.githubusercontent.com/" + name.substring(0, name.indexOf("/"))
            } else {
                return "https://avatars.githubusercontent.com/" + name
            }
        }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.id)
        dest.writeString(this.name)
        dest.writeString(this.topic)
        dest.writeByte(if (oneToOne) 1.toByte() else 0.toByte())
        dest.writeInt(this.unreadItems)
        dest.writeInt(this.userCount)
        dest.writeByte(if (mention) 1.toByte() else 0.toByte())
        dest.writeByte(if (hide) 1.toByte() else 0.toByte())
        dest.writeInt(this.listPosition)
    }

    constructor() {
    }

    protected constructor(`in`: Parcel) {
        this.id = `in`.readString()
        this.name = `in`.readString()
        this.topic = `in`.readString()
        this.oneToOne = `in`.readByte().toInt() != 0
        this.unreadItems = `in`.readInt()
        this.userCount = `in`.readInt()
        this.mention = `in`.readByte().toInt() != 0
        this.hide = `in`.readByte().toInt() != 0
        this.listPosition = `in`.readInt()
    }

    companion object {

        val CREATOR: Parcelable.Creator<RoomViewModel> = object : Parcelable.Creator<RoomViewModel> {
            override fun createFromParcel(source: Parcel): RoomViewModel {
                return RoomViewModel(source)
            }

            override fun newArray(size: Int): Array<RoomViewModel> {
                return arrayOfNulls(size)
            }
        }
    }
}
