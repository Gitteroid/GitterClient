package com.ne1c.gitteroid.models.data


import android.os.Parcel
import android.os.Parcelable

import java.util.ArrayList

class MessageModel : Parcelable {
    var id: String
    var text: String
    var html: String
    var sent: String
    var editedAt: String
    var fromUser: UserModel
    var unread: Boolean = false
    var readBy: Int = 0
    var urls: List<Urls>
    var mentions: ArrayList<Mentions>
    //public String issues;
    var v: Int = 0

    class Mentions : Parcelable {
        var screenName: String
        var userId: String

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(this.screenName)
            dest.writeString(this.userId)
        }

        constructor() {
        }

        protected constructor(`in`: Parcel) {
            this.screenName = `in`.readString()
            this.userId = `in`.readString()
        }

        companion object {

            val CREATOR: Parcelable.Creator<Mentions> = object : Parcelable.Creator<Mentions> {
                override fun createFromParcel(source: Parcel): Mentions {
                    return Mentions(source)
                }

                override fun newArray(size: Int): Array<Mentions> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    class Urls : Parcelable {
        var url: String

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(this.url)
        }

        constructor() {
        }

        protected constructor(`in`: Parcel) {
            this.url = `in`.readString()
        }

        companion object {

            val CREATOR: Parcelable.Creator<Urls> = object : Parcelable.Creator<Urls> {
                override fun createFromParcel(source: Parcel): Urls {
                    return Urls(source)
                }

                override fun newArray(size: Int): Array<Urls> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.id)
        dest.writeString(this.text)
        dest.writeString(this.html)
        dest.writeString(this.sent)
        dest.writeString(this.editedAt)
        dest.writeParcelable(this.fromUser, 0)
        dest.writeByte(if (unread) 1.toByte() else 0.toByte())
        dest.writeInt(this.readBy)
        dest.writeTypedList(urls)
        dest.writeTypedList(mentions)
        dest.writeInt(this.v)
    }

    constructor() {
    }

    protected constructor(`in`: Parcel) {
        this.id = `in`.readString()
        this.text = `in`.readString()
        this.html = `in`.readString()
        this.sent = `in`.readString()
        this.editedAt = `in`.readString()
        this.fromUser = `in`.readParcelable<UserModel>(UserModel::class.java.classLoader)
        this.unread = `in`.readByte().toInt() != 0
        this.readBy = `in`.readInt()
        this.urls = `in`.createTypedArrayList(Urls.CREATOR)
        this.mentions = `in`.createTypedArrayList(Mentions.CREATOR)
        this.v = `in`.readInt()
    }

    companion object {

        val CREATOR: Parcelable.Creator<MessageModel> = object : Parcelable.Creator<MessageModel> {
            override fun createFromParcel(source: Parcel): MessageModel {
                return MessageModel(source)
            }

            override fun newArray(size: Int): Array<MessageModel> {
                return arrayOfNulls(size)
            }
        }
    }
}
