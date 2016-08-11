package com.ne1c.gitteroid.ui.fragments


import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView

import com.bumptech.glide.Glide
import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.models.view.RoomViewModel

import java.util.ArrayList

class PickRoomsDialogFragment : DialogFragment() {

    private var mRooms = ArrayList<RoomViewModel>()
    private var mOnPickedRoomCallback: OnPickedRoom? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater!!.inflate(R.layout.fragment_pick_rooms_dialog, container, false)

        val roomsListView = v.findViewById(R.id.rooms_listView) as ListView
        roomsListView.adapter = object : ArrayAdapter<String>(activity, R.layout.item_room) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_room, parent, false)

                (itemView.findViewById(R.id.name_textView) as TextView).text = mRooms[position].name

                Glide.with(activity).load(mRooms[position].avatarUrl).error(R.drawable.ic_room).into(itemView.findViewById(R.id.avatar_imageView) as ImageView)

                return itemView
            }

            override fun getCount(): Int {
                return mRooms.size
            }
        }

        roomsListView.setOnItemClickListener { parent, view, position, id ->
            if (mOnPickedRoomCallback != null) {
                mOnPickedRoomCallback!!.result(mRooms[position])
            }

            dialog.dismiss()
        }

        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)

        return v
    }

    fun show(fm: FragmentManager, list: ArrayList<RoomViewModel>, onPickedRoomCallback: OnPickedRoom) {
        super.show(fm, PickRoomsDialogFragment::class.java.name)
        mRooms = list
        mOnPickedRoomCallback = onPickedRoomCallback
    }

    interface OnPickedRoom {
        fun result(model: RoomViewModel)
    }

    companion object {
        val PICKED_ROOM_DIALOG_REQUEST_CODE = 99
    }
}
