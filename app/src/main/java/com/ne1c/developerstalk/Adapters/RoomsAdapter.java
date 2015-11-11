package com.ne1c.developerstalk.Adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ne1c.developerstalk.Activities.MainActivity;
import com.ne1c.developerstalk.Models.RoomModel;
import com.ne1c.developerstalk.R;

import java.util.ArrayList;

public class RoomsAdapter extends RecyclerView.Adapter<RoomsAdapter.ViewHolder> {
    private ArrayList<RoomModel> mRooms;
    private Context mContext;

    public RoomsAdapter(ArrayList<RoomModel> rooms, Context context) {
        this.mRooms = rooms;
        this.mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_room, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final RoomModel room = mRooms.get(position);

        holder.roomName.setText(room.name);
        if (room.mentions > 0) {
            holder.counterMess.setBackgroundResource(R.drawable.rounded_counter_unread_mess);
            holder.counterMess.setText("@");

            holder.counterMess.setVisibility(View.VISIBLE);
        } else if (room.unreadItems > 0) {
            holder.counterMess.setBackgroundResource(R.drawable.rounded_counter_unread_mess);
            holder.counterMess.setText(String.valueOf(room.unreadItems));

            holder.counterMess.setVisibility(View.VISIBLE);
        } else {
            holder.counterMess.setVisibility(View.GONE);
        }

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.putExtra("roomId", room.id);

                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mRooms.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView roomName;
        public TextView counterMess;
        public LinearLayout parentLayout;

        public ViewHolder(View itemView) {
            super(itemView);

            roomName = (TextView) itemView.findViewById(R.id.room_name);
            counterMess = (TextView) itemView.findViewById(R.id.counter_mess);
            parentLayout = (LinearLayout) itemView.findViewById(R.id.parent_layout);
        }
    }
}
