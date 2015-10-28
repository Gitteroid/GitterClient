package com.ne1c.developerstalk.Fragments;


import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.ne1c.developerstalk.Adapters.RoomsAdapter;
import com.ne1c.developerstalk.Database.ClientDatabase;
import com.ne1c.developerstalk.Models.RoomModel;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.RoomAsyncLoader;
import com.ne1c.developerstalk.Util.Utils;

import java.util.ArrayList;

public class RoomsListFragment extends Fragment implements LoaderManager.LoaderCallbacks<ArrayList<RoomModel>> {
    private ProgressBar mProgressBar;

    private RecyclerView mRoomsList;
    private RoomsAdapter mAdapter;
    private ArrayList<RoomModel> mRooms = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_rooms_list, container, false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progress_bar);

        mRoomsList = (RecyclerView) v.findViewById(R.id.rooms_list);
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        mRoomsList.setLayoutManager(manager);
        mAdapter = new RoomsAdapter(mRooms, getActivity());
        mRoomsList.setAdapter(mAdapter);

        if (mRooms.size() == 0) {
            getLoaderManager().initLoader(RoomAsyncLoader.FROM_DATABASE, null, this).forceLoad();

            mProgressBar.setVisibility(View.VISIBLE);
            mRoomsList.setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public Loader<ArrayList<RoomModel>> onCreateLoader(int id, Bundle args) {
        return new RoomAsyncLoader(getActivity().getApplicationContext(), id);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<RoomModel>> loader, ArrayList<RoomModel> data) {
        if (loader.getId() == RoomAsyncLoader.FROM_DATABASE && Utils.getInstance().isNetworkConnected()) {
            getLoaderManager().initLoader(RoomAsyncLoader.FROM_SERVER, null, this).forceLoad();
        } else if (loader.getId() == RoomAsyncLoader.FROM_SERVER) {
            ClientDatabase client = new ClientDatabase(getActivity());
            client.insertRooms(data);
        }

        if (mRoomsList.getVisibility() != View.VISIBLE) {
            mProgressBar.setVisibility(View.GONE);
            mRoomsList.setVisibility(View.VISIBLE);
        }

        if (data.size() > 0) {
            mRooms.clear();
            mRooms.addAll(data);

            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<RoomModel>> loader) {

    }
}
