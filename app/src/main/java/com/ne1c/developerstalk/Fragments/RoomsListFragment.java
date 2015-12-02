package com.ne1c.developerstalk.Fragments;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ne1c.developerstalk.Adapters.RoomsAdapter;
import com.ne1c.developerstalk.Adapters.helper.OnStartDragListener;
import com.ne1c.developerstalk.Adapters.helper.SimpleItemTouchHelperCallback;
import com.ne1c.developerstalk.Database.ClientDatabase;
import com.ne1c.developerstalk.Models.RoomModel;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.RoomAsyncLoader;
import com.ne1c.developerstalk.Util.Utils;

import java.util.ArrayList;
import java.util.Collections;

public class RoomsListFragment extends Fragment implements LoaderManager.LoaderCallbacks<ArrayList<RoomModel>>,
        OnStartDragListener, RoomsAdapter.EditRoomsCallback {
    private RecyclerView mRoomsList;
    private RoomsAdapter mAdapter;
    private SwipeRefreshLayout mRefreshLayout;
    private ItemTouchHelper mItemTouchHelper;

    private ArrayList<RoomModel> mRooms = new ArrayList<>();

    // All rooms for edit
    // Set this list to adapter if user will edit
    private ArrayList<RoomModel> mAllRooms = new ArrayList<>();
    private boolean mIsEditing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        refreshRooms();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_rooms_list, container, false);

        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.refresh_rooms_layout);

        mRoomsList = (RecyclerView) v.findViewById(R.id.rooms_list);
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        mRoomsList.setLayoutManager(manager);
        mRoomsList.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new RoomsAdapter(mRooms, getActivity(), this);
        mRoomsList.setAdapter(mAdapter);

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!isEdit()) {
                    refreshRooms();
                } else {
                    mRefreshLayout.setRefreshing(false);
                }
            }
        });

        return v;
    }

    public boolean isEdit() {
        return mIsEditing;
    }

    public void setEdit(boolean edit) {
        mIsEditing = edit;
        if (edit) {
            RoomsAdapter editableAdapter = new RoomsAdapter(mAllRooms, getActivity(), this);
            editableAdapter.setEditRoomsCallback(this);
            editableAdapter.setEdit(edit);

            ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(editableAdapter);
            mItemTouchHelper = new ItemTouchHelper(callback);

            mRoomsList.setAdapter(editableAdapter);

            mItemTouchHelper.attachToRecyclerView(mRoomsList);
        } else {
            mItemTouchHelper.attachToRecyclerView(null);
            mRoomsList.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
            getLoaderManager().initLoader(RoomAsyncLoader.WRITE_TO_DATABASE, null, this).forceLoad();
        }
    }

    private void refreshRooms() {
        mRefreshLayout.setRefreshing(true);
        getLoaderManager().initLoader(RoomAsyncLoader.FROM_DATABASE, null, this).forceLoad();
    }

    @Override
    public Loader<ArrayList<RoomModel>> onCreateLoader(int id, Bundle args) {
        if (id == RoomAsyncLoader.WRITE_TO_DATABASE) {
            return new RoomAsyncLoader(getActivity().getApplicationContext(), id, mAllRooms);
        } else {
            return new RoomAsyncLoader(getActivity().getApplicationContext(), id);
        }
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<RoomModel>> loader, ArrayList<RoomModel> data) {
        if (loader.getId() == RoomAsyncLoader.FROM_DATABASE && Utils.getInstance().isNetworkConnected()) {
            getLoaderManager().initLoader(RoomAsyncLoader.FROM_SERVER, null, this).forceLoad();
        } else if (loader.getId() == RoomAsyncLoader.FROM_SERVER) {
            ClientDatabase client = new ClientDatabase(getActivity());
            client.insertRooms(RoomModel.margeRooms(mRooms, data));
            mRefreshLayout.setRefreshing(false);
        } else if (loader.getId() == RoomAsyncLoader.FROM_DATABASE && !Utils.getInstance().isNetworkConnected()) {
            mRefreshLayout.setRefreshing(false);
        }

        if (data != null && data.size() > 0) {
            mRooms.clear();
            mAllRooms.clear();
            mAllRooms.addAll(data);

            for (RoomModel model : data) {
                if (!model.hide) {
                    mRooms.add(model);
                }
            }

            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<RoomModel>> loader) {

    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public void hideRoom(int position) {
        mAllRooms.get(position).hide = true;
    }

    @Override
    public void changeRoomPosition(int oldPos, int newPos) {
        if (isEdit()) {
            Collections.swap(mAllRooms, oldPos, newPos);
        }
    }
}
