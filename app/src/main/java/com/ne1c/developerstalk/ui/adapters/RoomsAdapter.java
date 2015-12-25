package com.ne1c.developerstalk.ui.adapters;

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

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.ui.activities.MainActivity;
import com.ne1c.developerstalk.ui.adapters.helper.ItemTouchHelperAdapter;
import com.ne1c.developerstalk.ui.adapters.helper.OnStartDragListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemTouchHelperAdapter {
    private List<RoomModel> mRooms;
    private Context mContext;
    private boolean mIsEdit = false;

    private OnStartDragListener mDragStartListener;

    public RoomsAdapter(ArrayList<RoomModel> rooms, Context context) {
        this.mRooms = rooms;
        this.mContext = context;
    }

    public RoomsAdapter(List<RoomModel> rooms, Context context, OnStartDragListener dragStartListener) {
        this.mRooms = rooms;
        this.mContext = context;

        mDragStartListener = dragStartListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            return new ViewHolderConf(LayoutInflater.from(mContext).inflate(R.layout.item_room_conf, parent, false));
        }

        if (viewType == 1) {
            return new ViewHolderOne(LayoutInflater.from(mContext).inflate(R.layout.item_room_one, parent, false));
        }

        return null;
    }

    @Override
    public int getItemViewType(int position) {
        if (!mRooms.get(position).oneToOne) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final RoomModel room = mRooms.get(position);

        if (holder.getItemViewType() == 0) {
            confHolderBehavior(holder, room);
        } else {
            oneHolderBehavior(holder, room);
        }

    }

    private void confHolderBehavior(RecyclerView.ViewHolder holder, RoomModel room) {
        ViewHolderConf holderConf = (ViewHolderConf) holder;
        if (room.hide) {
            holderConf.parentLayout.setBackgroundResource(R.drawable.hide_room_selector);
        } else {
            int[] attrs = new int[]{android.R.attr.selectableItemBackground};
            TypedArray ta = mContext.obtainStyledAttributes(attrs);
            Drawable selectable = ta.getDrawable(0);
            ta.recycle();
            holderConf.parentLayout.setBackground(selectable);
        }

        holderConf.roomName.setText(room.name);
        if (room.mentions > 0) {
            holderConf.counterMess.setBackgroundResource(R.drawable.rounded_counter_mentions_mess);
            holderConf.counterMess.setText("@");

            holderConf.counterMess.setVisibility(View.VISIBLE);
        } else if (room.unreadItems > 0) {
            holderConf.counterMess.setBackgroundResource(R.drawable.rounded_counter_unread_mess);

            String unreadItems = room.unreadItems > 99 ? "99+" : String.valueOf(room.unreadItems);
            holderConf.counterMess.setText(unreadItems);

            holderConf.counterMess.setVisibility(View.VISIBLE);
        } else {
            holderConf.counterMess.setVisibility(View.GONE);
        }

        if (mIsEdit) {
            holderConf.editRoom.setVisibility(View.VISIBLE);
        } else {
            holderConf.editRoom.setVisibility(View.GONE);
        }

        holderConf.totalPeopleRoom.setText(mContext.getString(R.string.total_people) + " " + room.userCount);

        if (room.topic.isEmpty()) {
            holderConf.descRoom.setVisibility(View.GONE);
        } else {
            holderConf.descRoom.setVisibility(View.VISIBLE);
            holderConf.descRoom.setText(room.topic);
        }

        holderConf.editRoom.setOnTouchListener((v, event) -> {
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                mDragStartListener.onStartDrag(holder);
            }
            return false;
        });
    }

    private void oneHolderBehavior(RecyclerView.ViewHolder holder, RoomModel room) {
        ViewHolderOne holderOne = (ViewHolderOne) holder;
        if (room.hide) {
            holderOne.parentLayout.setBackgroundResource(R.drawable.hide_room_selector);
        } else {
            int[] attrs = new int[]{android.R.attr.selectableItemBackground};
            TypedArray ta = mContext.obtainStyledAttributes(attrs);
            Drawable selectable = ta.getDrawable(0);
            ta.recycle();
            holderOne.parentLayout.setBackground(selectable);
        }

        holderOne.roomName.setText(room.name);
        if (room.mentions > 0) {
            holderOne.counterMess.setBackgroundResource(R.drawable.rounded_counter_mentions_mess);
            holderOne.counterMess.setText("@");

            holderOne.counterMess.setVisibility(View.VISIBLE);
        } else if (room.unreadItems > 0) {
            holderOne.counterMess.setBackgroundResource(R.drawable.rounded_counter_unread_mess);

            String unreadItems = room.unreadItems > 99 ? "99+" : String.valueOf(room.unreadItems);
            holderOne.counterMess.setText(unreadItems);

            holderOne.counterMess.setVisibility(View.VISIBLE);
        } else {
            holderOne.counterMess.setVisibility(View.GONE);
        }

        if (mIsEdit) {
            holderOne.editRoom.setVisibility(View.VISIBLE);
        } else {
            holderOne.editRoom.setVisibility(View.GONE);
        }

        holderOne.editRoom.setOnTouchListener((v, event) -> {
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                mDragStartListener.onStartDrag(holder);
            }
            return false;
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

    public class ViewHolderConf extends RecyclerView.ViewHolder {
        public TextView roomName;
        public TextView descRoom;
        public TextView totalPeopleRoom;
        public ImageView roomImage;
        public ImageView editRoom;
        public TextView counterMess;
        public LinearLayout parentLayout;

        public ViewHolderConf(View itemView) {
            super(itemView);

            roomName = (TextView) itemView.findViewById(R.id.room_name);
            descRoom = (TextView) itemView.findViewById(R.id.desc_room);
            totalPeopleRoom = (TextView) itemView.findViewById(R.id.total_people_room);
            roomImage = (ImageView) itemView.findViewById(R.id.room_image);
            editRoom = (ImageView) itemView.findViewById(R.id.edit_room);
            counterMess = (TextView) itemView.findViewById(R.id.counter_mess);
            parentLayout = (LinearLayout) itemView.findViewById(R.id.parent_layout);

            itemView.setOnClickListener(v -> {
                if (!isEdit()) {
                    Intent intent = new Intent(mContext, MainActivity.class);
                    intent.putExtra("roomId", mRooms.get(getAdapterPosition()).id);
                    mContext.startActivity(intent);
                }
            });
        }
    }

    public class ViewHolderOne extends RecyclerView.ViewHolder {
        public TextView roomName;
        public ImageView roomImage;
        public ImageView editRoom;
        public TextView counterMess;
        public LinearLayout parentLayout;

        public ViewHolderOne(View itemView) {
            super(itemView);

            roomName = (TextView) itemView.findViewById(R.id.room_name);
            roomImage = (ImageView) itemView.findViewById(R.id.room_image);
            editRoom = (ImageView) itemView.findViewById(R.id.edit_room);
            counterMess = (TextView) itemView.findViewById(R.id.counter_mess);
            parentLayout = (LinearLayout) itemView.findViewById(R.id.parent_layout);

            itemView.setOnClickListener(v -> {
                if (!isEdit()) {
                    Intent intent = new Intent(mContext, MainActivity.class);
                    intent.putExtra("roomId", mRooms.get(getAdapterPosition()).id);
                    mContext.startActivity(intent);
                }
            });
        }
    }
}
