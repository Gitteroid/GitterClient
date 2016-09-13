package com.ne1c.gitteroid.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.models.view.RoomViewModel
import java.util.*

class SearchRoomsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private val TYPE_ITEM = 0
    private val TYPE_PROGRESS_FOOTER = 1

    private var rooms: ArrayList<RoomViewModel?> = ArrayList()

    constructor(rooms: ArrayList<RoomViewModel?>) : super() {
        this.rooms = rooms
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_ITEM) {
            return RoomViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.item_room, parent, false))
        } else {
            val parentLayout = FrameLayout(parent?.context)
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            params.gravity = Gravity.CENTER
            parentLayout.layoutParams = params

            return ProgressViewHolder(parentLayout)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (rooms[position] != null) {
            return TYPE_ITEM
        } else {
            return TYPE_PROGRESS_FOOTER
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        if (getItemViewType(position) == TYPE_ITEM && holder is RoomViewHolder) {
            val room = rooms[position]

            Glide.with(holder.itemView?.context)
                    .load(room?.getAvatarUrl())
                    .into(holder.avatarImageView)

            holder.nameTextView.text = room?.name
        }
    }

    override fun getItemCount(): Int {
        return rooms.size
    }

    fun addProgressFooter() {
        if (rooms.last() != null) {
            rooms.add(null)
            notifyItemInserted(rooms.size - 1)
        }
    }

    fun removeProgressFooter() {
        if (rooms.last() == null) {
            val pos = rooms.indexOf(rooms.last())
            rooms.removeAt(pos)

            notifyItemRemoved(pos)
        }
    }

    class RoomViewHolder : RecyclerView.ViewHolder {
        val avatarImageView: ImageView
        val nameTextView: TextView

        constructor(itemView: View) : super(itemView) {
            avatarImageView = itemView.findViewById(R.id.avatar_imageView) as ImageView
            nameTextView = itemView.findViewById(R.id.name_textView) as TextView
        }
    }

    class ProgressViewHolder : RecyclerView.ViewHolder {
        val progressBar: ProgressBar

        constructor(itemView: View) : super(itemView) {
            progressBar = ProgressBar(itemView.context)
            progressBar.isIndeterminate = true

            (itemView as ViewGroup).addView(progressBar)
        }
    }
}