package com.ne1c.gitteroid.models.view

import android.os.Parcel
import android.os.Parcelable

import com.ne1c.gitteroid.models.data.MessageModel
import com.ne1c.gitteroid.models.data.UserModel

class MessageViewModel : Parcelable {
    var id: String
    var text: String
    var sent: String
    var fromUser: UserModel
    var unread: Boolean = false
    var urls: List<MessageModel.Urls>


    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.id)
        dest.writeString(this.text)
        dest.writeString(this.sent)
        dest.writeParcelable(this.fromUser, flags)
        dest.writeByte(if (unread) 1.toByte() else 0.toByte())
        dest.writeTypedList(urls)
    }

    constructor() {
    }

    protected constructor(`in`: Parcel) {
        this.id = `in`.readString()
        this.text = `in`.readString()
        this.sent = `in`.readString()
        this.fromUser = `in`.readParcelable<UserModel>(UserModel::class.java.classLoader)
        this.unread = `in`.readByte().toInt() != 0
        this.urls = `in`.createTypedArrayList(MessageModel.Urls.CREATOR)
    }

    companion object {

        val CREATOR: Parcelable.Creator<MessageViewModel> = object : Parcelable.Creator<MessageViewModel> {
            override fun createFromParcel(source: Parcel): MessageViewModel {
                return MessageViewModel(source)
            }

            override fun newArray(size: Int): Array<MessageViewModel> {
                return arrayOfNulls(size)
            }
        }
    }
}
