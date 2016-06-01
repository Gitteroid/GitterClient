package com.ne1c.gitteroid.ui.fragments;


import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.ne1c.gitteroid.R;
import com.ne1c.gitteroid.models.view.RoomViewModel;

import java.util.ArrayList;

public class PickRoomsDialogFragment extends DialogFragment {
    public static final int PICKED_ROOM_DIALOG_REQUEST_CODE = 99;

    private ArrayList<RoomViewModel> mRooms = new ArrayList<>();
    private OnPickedRoom mOnPickedRoomCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_pick_rooms_dialog, container, false);

        ListView roomsListView = (ListView) v.findViewById(R.id.rooms_listView);
        roomsListView.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.item_room) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);

                ((TextView) itemView.findViewById(R.id.name_textView)).setText(mRooms.get(position).name);

                Glide.with(getActivity())
                        .load(mRooms.get(position).getAvatarUrl())
                        .error(R.drawable.ic_room)
                        .into((ImageView) itemView.findViewById(R.id.avatar_imageView));

                return itemView;
            }

            @Override
            public int getCount() {
                return mRooms.size();
            }
        });

        roomsListView.setOnItemClickListener((parent, view, position, id) -> {
            if (mOnPickedRoomCallback != null) {
                mOnPickedRoomCallback.result(mRooms.get(position));
            }

            getDialog().dismiss();
        });

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return v;
    }

    public void show(FragmentManager fm, ArrayList<RoomViewModel> list, OnPickedRoom onPickedRoomCallback) {
        super.show(fm, PickRoomsDialogFragment.class.getName());
        mRooms = list;
        mOnPickedRoomCallback = onPickedRoomCallback;
    }

    public interface OnPickedRoom {
        void result(RoomViewModel model);
    }
}
