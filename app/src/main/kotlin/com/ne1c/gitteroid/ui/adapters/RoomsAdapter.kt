package com.ne1c.gitteroid.ui.adapters

import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.support.v4.view.MotionEventCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.ui.activities.MainActivity
import com.ne1c.gitteroid.ui.activities.OverviewRoomActivity
import com.ne1c.gitteroid.ui.adapters.helper.ItemTouchHelperAdapter
import com.ne1c.gitteroid.ui.adapters.helper.OnStartDragListener

import java.util.Collections

class RoomsAdapter(private val mRooms: List<RoomViewModel>, private val mContext: Context, private val mDragStartListener: OnStartDragListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ItemTouchHelperAdapter {
    var isEdit = false
        private set
    private var mIsSearchMode = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder? {
        if (viewType == 0) {
            return ViewHolderConf(LayoutInflater.from(mContext).inflate(R.layout.item_room_conf, parent, false))
        }

        if (viewType == 1) {
            return ViewHolderOne(LayoutInflater.from(mContext).inflate(R.layout.item_room_one, parent, false))
        }

        return null
    }

    override fun getItemViewType(position: Int): Int {
        if (!mRooms[position].oneToOne) {
            return 0
        } else {
            return 1
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val room = mRooms[position]

        if (holder.itemViewType == 0) {
            confHolderBehavior(holder, room)
        } else {
            oneHolderBehavior(holder, room)
        }

    }

    private fun confHolderBehavior(holder: RecyclerView.ViewHolder, room: RoomViewModel) {
        val holderConf = holder as ViewHolderConf
        if (room.hide) {
            holderConf.parentLayout.setBackgroundResource(R.drawable.hide_room_selector)
        } else {
            val attrs = intArrayOf(android.R.attr.selectableItemBackground)
            val ta = mContext.obtainStyledAttributes(attrs)
            val selectable = ta.getDrawable(0)
            ta.recycle()
            holderConf.parentLayout.background = selectable
        }

        holderConf.roomName.text = room.name
        if (room.mention) {
            holderConf.counterMess.setBackgroundResource(R.drawable.rounded_counter_mentions_mess)
            holderConf.counterMess.text = "@"

            holderConf.counterMess.visibility = View.VISIBLE
        } else if (room.unreadItems > 0) {
            holderConf.counterMess.setBackgroundResource(R.drawable.rounded_counter_unread_mess)

            val unreadItems = if (room.unreadItems > 99) "99+" else room.unreadItems.toString()
            holderConf.counterMess.text = unreadItems

            holderConf.counterMess.visibility = View.VISIBLE
        } else {
            holderConf.counterMess.visibility = View.INVISIBLE
        }

        if (isEdit) {
            holderConf.editRoom.visibility = View.VISIBLE
        } else {
            holderConf.editRoom.visibility = View.GONE
        }

        holderConf.totalPeopleRoom.text = String.format("%s %d", mContext.getString(R.string.total_people), room.userCount)

        if (room.topic.isEmpty()) {
            holderConf.descRoom.visibility = View.GONE
        } else {
            holderConf.descRoom.visibility = View.VISIBLE
            holderConf.descRoom.text = room.topic
        }

        holderConf.editRoom.setOnTouchListener { v, event ->
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN && isEdit) {
                mDragStartListener.onStartDrag(holder)
            }
            false
        }
    }

    private fun oneHolderBehavior(holder: RecyclerView.ViewHolder, room: RoomViewModel) {
        val holderOne = holder as ViewHolderOne
        if (room.hide) {
            holderOne.parentLayout.setBackgroundResource(R.drawable.hide_room_selector)
        } else {
            val attrs = intArrayOf(android.R.attr.selectableItemBackground)
            val ta = mContext.obtainStyledAttributes(attrs)
            val selectable = ta.getDrawable(0)
            ta.recycle()
            holderOne.parentLayout.background = selectable
        }

        holderOne.roomName.text = room.name
        if (room.mention) {
            holderOne.counterMess.setBackgroundResource(R.drawable.rounded_counter_mentions_mess)
            holderOne.counterMess.text = "@"

            holderOne.counterMess.visibility = View.VISIBLE
        } else if (room.unreadItems > 0) {
            holderOne.counterMess.setBackgroundResource(R.drawable.rounded_counter_unread_mess)

            val unreadItems = if (room.unreadItems > 99) "99+" else room.unreadItems.toString()
            holderOne.counterMess.text = unreadItems

            holderOne.counterMess.visibility = View.VISIBLE
        } else {
            holderOne.counterMess.visibility = View.INVISIBLE
        }

        if (isEdit) {
            holderOne.editRoom.visibility = View.VISIBLE
        } else {
            holderOne.editRoom.visibility = View.GONE
        }

        holderOne.editRoom.setOnTouchListener { v, event ->
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN && isEdit) {
                mDragStartListener.onStartDrag(holder)
            }
            false
        }

    }

    override fun getItemCount(): Int {
        return mRooms.size
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (isEdit) {
            mRooms[fromPosition].listPosition = toPosition
            mRooms[toPosition].listPosition = fromPosition
            Collections.swap(mRooms, fromPosition, toPosition)
            notifyItemMoved(fromPosition, toPosition)

            return true
        }

        return false
    }

    override fun onItemDismiss(position: Int) {
        mRooms[position].hide = !mRooms[position].hide
        notifyItemChanged(position)
    }

    fun setEditMode(edit: Boolean) {
        isEdit = edit
        notifyDataSetChanged()
    }

    fun setSearchMode(searchMode: Boolean) {
        mIsSearchMode = searchMode
    }

    inner class ViewHolderConf(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var roomName: TextView
        var descRoom: TextView
        var totalPeopleRoom: TextView
        var roomImage: ImageView
        var editRoom: ImageView
        var counterMess: TextView
        var parentLayout: LinearLayout

        init {

            roomName = itemView.findViewById(R.id.room_name) as TextView
            descRoom = itemView.findViewById(R.id.desc_room) as TextView
            totalPeopleRoom = itemView.findViewById(R.id.total_people_room) as TextView
            roomImage = itemView.findViewById(R.id.room_image) as ImageView
            editRoom = itemView.findViewById(R.id.edit_room) as ImageView
            counterMess = itemView.findViewById(R.id.counter_mess) as TextView
            parentLayout = itemView.findViewById(R.id.parent_layout) as LinearLayout

            itemView.setOnClickListener { v ->
                if (!isEdit && !mIsSearchMode) {
                    val intent = Intent(mContext, MainActivity::class.java)
                    intent.putExtra("roomId", mRooms[adapterPosition].id)
                    mContext.startActivity(intent)
                } else if (mIsSearchMode) {
                    val intent = Intent(mContext, OverviewRoomActivity::class.java)
                    intent.putExtra("room", mRooms[adapterPosition])
                    mContext.startActivity(intent)
                }
            }
        }
    }

    inner class ViewHolderOne(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var roomName: TextView
        var roomImage: ImageView
        var editRoom: ImageView
        var counterMess: TextView
        var parentLayout: LinearLayout

        init {

            roomName = itemView.findViewById(R.id.room_name) as TextView
            roomImage = itemView.findViewById(R.id.room_image) as ImageView
            editRoom = itemView.findViewById(R.id.edit_room) as ImageView
            counterMess = itemView.findViewById(R.id.counter_mess) as TextView
            parentLayout = itemView.findViewById(R.id.parent_layout) as LinearLayout

            itemView.setOnClickListener { v ->
                if (!isEdit && !mIsSearchMode) {
                    val intent = Intent(mContext, MainActivity::class.java)
                    intent.putExtra("roomId", mRooms[adapterPosition].id)
                    mContext.startActivity(intent)
                } else if (mIsSearchMode) {
                    val intent = Intent(mContext, OverviewRoomActivity::class.java)
                    intent.putExtra("roomId", mRooms[adapterPosition].id)
                    mContext.startActivity(intent)
                }
            }
        }
    }
}
