package com.ne1c.gitteroid.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.models.view.RoomViewModel
import java.util.*

class SearchRoomsAdapter : RecyclerView.Adapter<SearchRoomsAdapter.ViewHolder> {
    private var rooms: ArrayList<RoomViewModel> = ArrayList()

    constructor(rooms: ArrayList<RoomViewModel>) : super() {
        this.rooms = rooms
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.item_room, null, false))
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        val room = rooms[position]

        Glide.with(holder!!.itemView.context)
                .load(room.getAvatarUrl())
                .into(holder.avatarImageView)

        holder.nameTextView.text = room.name
    }

    override fun getItemCount(): Int {
        return rooms.size
    }

    class ViewHolder : RecyclerView.ViewHolder {
        val avatarImageView: ImageView
        val nameTextView: TextView

        constructor(itemView: View) : super(itemView) {
            avatarImageView = itemView.findViewById(R.id.avatar_imageView) as ImageView
            nameTextView = itemView.findViewById(R.id.name_textView) as TextView
        }
    }
}