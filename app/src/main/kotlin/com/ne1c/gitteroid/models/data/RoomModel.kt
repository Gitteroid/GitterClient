package com.ne1c.gitteroid.models.data

import android.os.Parcel
import android.os.Parcelable

import java.util.ArrayList
import java.util.Comparator

open class RoomModel : Parcelable {
    var id: String
    var name: String
    var topic: String
    var oneToOne: Boolean = false
    var users = ArrayList<UserModel>()
    var unreadItems: Int = 0
    var userCount: Int = 0
    var mentions: Int = 0
    var lastAccessTime: String
    var lurk: Boolean = false
    var url: String
    var githubType: String
    var hide: Boolean = false
    var v: Int = 0
    var listPosition = -1


    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.id)
        dest.writeString(this.name)
        dest.writeString(this.topic)
        dest.writeByte(if (oneToOne) 1.toByte() else 0.toByte())
        dest.writeList(this.users)
        dest.writeInt(this.unreadItems)
        dest.writeInt(this.userCount)
        dest.writeInt(this.mentions)
        dest.writeString(this.lastAccessTime)
        dest.writeByte(if (lurk) 1.toByte() else 0.toByte())
        dest.writeString(this.url)
        dest.writeString(this.githubType)
        dest.writeInt(this.v)
    }

    constructor() {
    }

    protected constructor(`in`: Parcel) {
        this.id = `in`.readString()
        this.name = `in`.readString()
        this.topic = `in`.readString()
        this.oneToOne = `in`.readByte().toInt() != 0
        this.users = ArrayList<UserModel>()
        `in`.readList(this.users, ArrayList<*>::class.java!!.getClassLoader())
        this.unreadItems = `in`.readInt()
        this.userCount = `in`.readInt()
        this.mentions = `in`.readInt()
        this.lastAccessTime = `in`.readString()
        this.lurk = `in`.readByte().toInt() != 0
        this.url = `in`.readString()
        this.githubType = `in`.readString()
        this.v = `in`.readInt()
    }

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

    companion object {

        // r1 - old list, r2 - new list
        // copy position, status hideRoom from r1 to r2
        fun margeRooms(r1: ArrayList<RoomModel>, r2: ArrayList<RoomModel>): ArrayList<RoomModel> {
            for (r1Model in r1) {
                for (r2Model in r2) {
                    if (r1Model.id == r2Model.id) {
                        r2Model.hide = r1Model.hide
                        r2Model.listPosition = r1Model.listPosition
                    }
                }
            }

            return r2
        }

        val CREATOR: Parcelable.Creator<RoomModel> = object : Parcelable.Creator<RoomModel> {
            override fun createFromParcel(source: Parcel): RoomModel {
                return RoomModel(source)
            }

            override fun newArray(size: Int): Array<RoomModel> {
                return arrayOfNulls(size)
            }
        }
    }
}
