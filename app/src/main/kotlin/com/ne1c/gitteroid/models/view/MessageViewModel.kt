package com.ne1c.gitteroid.models.view

import android.os.Parcel
import android.os.Parcelable

import com.ne1c.gitteroid.models.data.MessageModel
import com.ne1c.gitteroid.models.data.UserModel
import java.util.*

data class MessageViewModel(
        var id: String = "",
        var text: String = "",
        var sent: String = "",
        var fromUser: UserModel? = null,
        var unread: Boolean = false,
        var urls: List<MessageModel.Urls> = Collections.emptyList()) : Parcelable {

    constructor(source: Parcel) : this(source.readString(),
            source.readString(),
            source.readString(),
            source.readParcelable(UserModel::class.java.classLoader),
            1.equals(source.readInt()),
            source.createTypedArrayList(MessageModel.Urls.CREATOR))

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(id)
        dest?.writeString(text)
        dest?.writeString(sent)
        dest?.writeParcelable(fromUser, 0)
        dest?.writeInt((if (unread) 1 else 0))
        dest?.writeTypedList(urls)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<MessageViewModel> = object : Parcelable.Creator<MessageViewModel> {
            override fun createFromParcel(source: Parcel): MessageViewModel = MessageViewModel(source)
            override fun newArray(size: Int): Array<MessageViewModel?> = arrayOfNulls(size)
        }
    }
}
