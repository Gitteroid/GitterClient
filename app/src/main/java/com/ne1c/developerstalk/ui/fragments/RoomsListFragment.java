package com.ne1c.developerstalk.ui.fragments;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.presenters.RoomsListPresenter;
import com.ne1c.developerstalk.ui.adapters.RoomsAdapter;
import com.ne1c.developerstalk.ui.adapters.helper.OnStartDragListener;
import com.ne1c.developerstalk.ui.adapters.helper.SimpleItemTouchHelperCallback;
import com.ne1c.developerstalk.ui.views.RoomsListView;

import java.util.ArrayList;
import java.util.List;

public class RoomsListFragment extends Fragment implements OnStartDragListener, RoomsListView {
    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mRoomsList;
    private RoomsAdapter mAdapter;
    private ItemTouchHelper mItemTouchHelper;
    private ProgressDialog mProgressDialog;

    private ArrayList<RoomModel> mRooms = new ArrayList<>();

    private boolean mIsEdit = false;

    private RoomsListPresenter mPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mPresenter = new RoomsListPresenter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_rooms_list, container, false);

        mRoomsList = (RecyclerView) v.findViewById(R.id.rooms_list);
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        mRoomsList.setLayoutManager(manager);
        mRoomsList.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new RoomsAdapter(mRooms, getActivity(), this);
        mRoomsList.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRoomsList);
        mRoomsList.removeItemDecoration(mItemTouchHelper);
        mRoomsList.removeOnChildAttachStateChangeListener(mItemTouchHelper);

        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.refresh_rooms_layout);
        mRefreshLayout.setOnRefreshListener(() -> {
            if (!mIsEdit) {
                mPresenter.loadRooms();
            }
        });

        mPresenter.bindView(this);
        mPresenter.loadCachedRooms();
        mPresenter.loadRooms();

        return v;
    }

    public boolean isEdit() {
        return mIsEdit;
    }

    public void setEdit(boolean edit) {
        mIsEdit = edit;
        if (edit) {
            mRooms.clear();
            mRooms.addAll(mPresenter.getAllRooms());
            mAdapter.setEdit(true);

            mRoomsList.addItemDecoration(mItemTouchHelper);
            mRoomsList.addOnChildAttachStateChangeListener(mItemTouchHelper);
        } else {
            mAdapter.setEdit(false);

            mRoomsList.removeItemDecoration(mItemTouchHelper);
            mRoomsList.removeOnChildAttachStateChangeListener(mItemTouchHelper);

            mPresenter.saveRooms(mRooms);
            ArrayList<RoomModel> visible = mPresenter.getOnlyVisibleRooms(mRooms);
            mRooms.clear();
            mRooms.addAll(visible);
        }
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public void showRooms(List<RoomModel> rooms) {
        if (mRefreshLayout.isRefreshing()) {
            mRefreshLayout.setRefreshing(false);
        }

        mRooms.clear();
        mRooms.addAll(rooms);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void showError(String text) {
        if (mRefreshLayout.isRefreshing()) {
            mRefreshLayout.setRefreshing(false);
        }

        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(getString(R.string.loading));
        }

        mProgressDialog.show();
    }

    @Override
    public void dismissDialog() {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public Context getAppContext() {
        return getActivity().getApplicationContext();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mPresenter.unbindView();
    }
}