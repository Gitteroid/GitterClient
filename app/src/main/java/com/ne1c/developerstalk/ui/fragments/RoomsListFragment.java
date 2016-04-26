package com.ne1c.developerstalk.ui.fragments;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.di.components.DaggerRoomsListComponent;
import com.ne1c.developerstalk.di.components.RoomsListComponent;
import com.ne1c.developerstalk.di.modules.RoomsListPresenterModule;
import com.ne1c.developerstalk.models.view.RoomViewModel;
import com.ne1c.developerstalk.presenters.RoomsListPresenter;
import com.ne1c.developerstalk.ui.adapters.RoomsAdapter;
import com.ne1c.developerstalk.ui.adapters.helper.OnStartDragListener;
import com.ne1c.developerstalk.ui.adapters.helper.SimpleItemTouchHelperCallback;
import com.ne1c.developerstalk.ui.views.RoomsListView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class RoomsListFragment extends BaseFragment implements OnStartDragListener, RoomsListView {
    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mRoomsList;
    private RoomsAdapter mAdapter;
    private RoomsAdapter mSearchableAdapter;
    private ItemTouchHelper mItemTouchHelper;
    private ProgressBar mProgressBar;

    private ArrayList<RoomViewModel> mRooms = new ArrayList<>();
    private ArrayList<RoomViewModel> mSearchedRooms = new ArrayList<>();

    private boolean mIsEdit = false;
    private boolean mIsSearchMode = false;

    private RoomsListComponent mComponent;

    @Inject
    RoomsListPresenter mPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mPresenter.onCreate();
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
        mSearchableAdapter = new RoomsAdapter(mSearchedRooms, getActivity(), this);

        if (mIsSearchMode) {
            mRoomsList.setAdapter(mSearchableAdapter);
        } else {
            mRoomsList.setAdapter(mAdapter);
        }

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);

        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.refresh_rooms_layout);
        mRefreshLayout.setOnRefreshListener(() -> {
            if (!mIsEdit) {
                mPresenter.loadRooms(true);
            } else {
                mRefreshLayout.setRefreshing(false);
            }
        });

        mProgressBar = (ProgressBar) v.findViewById(R.id.progress_bar);
        mProgressBar.setIndeterminate(true);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        mPresenter.bindView(this);

        if (mRooms.size() == 0) {
            mPresenter.loadRooms(false);
        }

        if (!mIsEdit && !mRefreshLayout.isRefreshing()) {
            mRefreshLayout.setRefreshing(true);
            mPresenter.loadRooms(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        mPresenter.unbindView();
    }

    @Override
    public void onDestroy() {
        mComponent = null;
        mPresenter.onDestroy();

        super.onDestroy();
    }

    public boolean isEdit() {
        return mIsEdit;
    }

    public void setEdit(boolean edit) {
        mIsEdit = edit;
        if (edit) {
            mRooms.clear();
            mRooms.addAll(mPresenter.getAllRooms());
            mAdapter.setEditMode(true);

            mItemTouchHelper.attachToRecyclerView(mRoomsList);
        } else {
            mAdapter.setEditMode(false);

            mPresenter.saveRooms(mRooms);
            ArrayList<RoomViewModel> visible = mPresenter.getOnlyVisibleRooms(mRooms);
            mRooms.clear();
            mRooms.addAll(visible);

            mItemTouchHelper.attachToRecyclerView(null);
        }
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        if (mIsEdit) {
            mItemTouchHelper.startDrag(viewHolder);
        }
    }

    @Override
    public void showRooms(List<RoomViewModel> rooms, boolean fresh) {
        if (mRefreshLayout.isRefreshing() && fresh) {
            mRefreshLayout.setRefreshing(false);
        }

        if (!mIsEdit) {
            mRooms.clear();
            mRooms.addAll(rooms);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void showError(int resId) {
        if (mRefreshLayout.isRefreshing()) {
            mRefreshLayout.setRefreshing(false);
        }

        Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showDialog() {
        if (mProgressBar.getVisibility() == View.GONE) {
            mRefreshLayout.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void dismissDialog() {
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mRefreshLayout.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void errorSearch() {
        mSearchedRooms.clear();
        mSearchableAdapter.notifyDataSetChanged();

        Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void resultSearch(ArrayList<RoomViewModel> rooms) {
        mSearchedRooms.clear();
        mSearchedRooms.addAll(rooms);
        mSearchableAdapter.notifyDataSetChanged();
    }

    @Override
    public void resultSearchWithOffset(ArrayList<RoomViewModel> rooms) {
        if (mIsSearchMode) {
            int startPosition = mSearchedRooms.size();
            mSearchedRooms.addAll(rooms);
            mSearchableAdapter.notifyItemRangeInserted(startPosition, 10);

            //mSearchOffset += 10; // Increment offset, after get result of previous offset request
        }
    }

    public void searchRoomsQuery(String text) {
        mPresenter.searchRooms(text);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mPresenter.unbindView();
    }

    @Override
    protected void initDiComponent() {
        mComponent = DaggerRoomsListComponent.builder()
                .applicationComponent(getAppComponent())
                .roomsListPresenterModule(new RoomsListPresenterModule())
                .build();

        mComponent.inject(this);
    }

    public void startSearch() {
        mIsSearchMode = true;
        mRoomsList.setAdapter(mSearchableAdapter);
        mSearchableAdapter.setSearchMode(true);
    }

    public void endSearch() {
        dismissDialog();
        mRoomsList.setAdapter(mAdapter);
        mSearchableAdapter.setSearchMode(false);

        mIsSearchMode = false;
    }
}