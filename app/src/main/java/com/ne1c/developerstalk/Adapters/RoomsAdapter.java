package com.ne1c.developerstalk.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ne1c.developerstalk.Activities.MainActivity;
import com.ne1c.developerstalk.Adapters.helper.ItemTouchHelperAdapter;
import com.ne1c.developerstalk.Adapters.helper.OnStartDragListener;
import com.ne1c.developerstalk.Models.RoomModel;
import com.ne1c.developerstalk.R;

import java.util.ArrayList;
import java.util.Collections;

public class RoomsAdapter extends RecyclerView.Adapter<RoomsAdapter.ViewHolder> implements ItemTouchHelperAdapter {
    private ArrayList<RoomModel> mRooms;
    private Context mContext;
    private boolean mIsEdit = false;

    private OnStartDragListener mDragStartListener;

    public RoomsAdapter(ArrayList<RoomModel> rooms, Context context) {
        this.mRooms = rooms;
        this.mContext = context;
    }

    public RoomsAdapter(ArrayList<RoomModel> rooms, Context context, OnStartDragListener dragStartListener) {
        this.mRooms = rooms;
        this.mContext = context;

        mDragStartListener = dragStartListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_room, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final RoomModel room = mRooms.get(position);

        if (mRooms.get(position).hide) {
            holder.parentLayout.setBackgroundResource(R.drawable.hide_room_selector);
        } else {
            int[] attrs = new int[] { android.R.attr.selectableItemBackground };
            TypedArray ta = mContext.obtainStyledAttributes(attrs);
            Drawable selectable = ta.getDrawable(0);
            ta.recycle();
            holder.parentLayout.setBackground(selectable);
        }

        holder.roomName.setText(room.name);
        if (room.mentions > 0) {
            holder.counterMess.setBackgroundResource(R.drawable.rounded_counter_mentions_mess);
            holder.counterMess.setText("@");

            holder.counterMess.setVisibility(View.VISIBLE);
        } else if (room.unreadItems > 0) {
            holder.counterMess.setBackgroundResource(R.drawable.rounded_counter_unread_mess);
            holder.counterMess.setText(String.valueOf(room.unreadItems));

            holder.counterMess.setVisibility(View.VISIBLE);
        } else {
            holder.counterMess.setVisibility(View.GONE);
        }

        if (mIsEdit) {
            holder.editRoom.setVisibility(View.VISIBLE);
        } else {
            holder.editRoom.setVisibility(View.GONE);
        }

        if (room.oneToOne) {
            holder.roomImage.setImageResource(R.mipmap.ic_room_onetoone_item);
        } else {
            holder.roomImage.setImageResource(R.mipmap.ic_room_item);
        }

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEdit()) {
                    Intent intent = new Intent(mContext, MainActivity.class);
                    intent.putExtra("roomId", room.id);
                    mContext.startActivity(intent);
                }
            }
        });

        holder.editRoom.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    if (isEdit()) {
                        mDragStartListener.onStartDrag(holder);
                    }
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mRooms.size();
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        mRooms.get(fromPosition).listPosition = toPosition;
        mRooms.get(toPosition).listPosition = fromPosition;
        Collections.swap(mRooms, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);

        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        mRooms.get(position).hide = !mRooms.get(position).hide;
        notifyItemChanged(position);
    }

    public void setEdit(boolean edit) {
        mIsEdit = edit;
        notifyDataSetChanged();
    }

    public boolean isEdit() {
        return mIsEdit;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView roomName;
        public ImageView roomImage;
        public ImageView editRoom;
        public TextView counterMess;
        public LinearLayout parentLayout;

        public ViewHolder(View itemView) {
            super(itemView);

            roomName = (TextView) itemView.findViewById(R.id.room_name);
            roomImage = (ImageView) itemView.findViewById(R.id.room_image);
            editRoom = (ImageView) itemView.findViewById(R.id.edit_room);
            counterMess = (TextView) itemView.findViewById(R.id.counter_mess);
            parentLayout = (LinearLayout) itemView.findViewById(R.id.parent_layout);

        }
    }
}
