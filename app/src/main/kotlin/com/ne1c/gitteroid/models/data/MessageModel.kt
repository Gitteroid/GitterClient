package com.ne1c.gitteroid.models.data

import android.os.Parcel
import android.os.Parcelable
import java.util.*

data class MessageModel(
        var id: String = "",
        var text: String = "",
        var html: String = "",
        var sent: String = "",
        var editedAt: String = "",
        var fromUser: UserModel? = null,
        var unread: Boolean = false,
        var readBy: Int = 0,
        var v: Int = 0,
        var urls: List<Urls> = Collections.emptyList(),
        var mentions: List<Mentions> = Collections.emptyList()) : Parcelable {

    data class Mentions(var screenName: String = "",
                        var userId: String = "") : Parcelable {
        constructor(source: Parcel) : this(source.readString(), source.readString())

        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel?, flags: Int) {
            dest?.writeString(screenName)
            dest?.writeString(userId)
        }

        companion object {
            @JvmField val CREATOR: Parcelable.Creator<Mentions> = object : Parcelable.Creator<Mentions> {
                override fun createFromParcel(source: Parcel): Mentions = Mentions(source)
                override fun newArray(size: Int): Array<Mentions?> = arrayOfNulls(size)
            }
        }
    }

    data class Urls(var url: String = "") : Parcelable {
        constructor(source: Parcel) : this(source.readString())

        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel?, flags: Int) {
            dest?.writeString(url)
        }

        companion object {
            @JvmField val CREATOR: Parcelable.Creator<Urls> = object : Parcelable.Creator<Urls> {
                override fun createFromParcel(source: Parcel): Urls = Urls(source)
                override fun newArray(size: Int): Array<Urls?> = arrayOfNulls(size)
            }
        }
    }

    constructor(source: Parcel) : this(source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readParcelable(UserModel::class.java.classLoader),
            1.equals(source.readInt()),
            source.readInt(),
            source.readInt(),
            source.createTypedArrayList(Urls.CREATOR),
            source.createTypedArrayList(Mentions.CREATOR))

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(id)
        dest?.writeString(text)
        dest?.writeString(html)
        dest?.writeString(sent)
        dest?.writeString(editedAt)
        dest?.writeParcelable(fromUser, 0)
        dest?.writeInt((if (unread) 1 else 0))
        dest?.writeInt(readBy)
        dest?.writeInt(v)
        dest?.writeTypedList(urls)
        dest?.writeTypedList(mentions)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<MessageModel> = object : Parcelable.Creator<MessageModel> {
            override fun createFromParcel(source: Parcel): MessageModel = MessageModel(source)
            override fun newArray(size: Int): Array<MessageModel?> = arrayOfNulls(size)
        }
    }
}
