package com.ne1c.developerstalk.presenters;

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.services.DataManger;
import com.ne1c.developerstalk.ui.views.RoomsListView;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class RoomsListPresenter extends BasePresenter<RoomsListView> {
    // All rooms for edit
    // Set this list to adapter if user will edit
    private ArrayList<RoomModel> mAllRooms = new ArrayList<>();

    private RoomsListView mView;

    private DataManger mDataManger;

    private CompositeSubscription mSubscriptions;

    @Override
    public void bindView(RoomsListView view) {
        mView = view;
        mDataManger = new DataManger(mView.getAppContext());

        mSubscriptions = new CompositeSubscription();
    }

    @Override
    public void unbindView() {
        mView = null;
        mSubscriptions.unsubscribe();
    }

    public List<RoomModel> getAllRooms() {
        return mAllRooms;
    }

    public void loadRooms() {
        if (!Utils.getInstance().isNetworkConnected()) {
            mView.showError(mView.getAppContext().getString(R.string.no_network));
        }

        @SuppressWarnings("unchecked")
        Subscription sub = mDataManger.getRooms().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(roomModels -> {
                    mAllRooms = (ArrayList<RoomModel>) roomModels.clone();

                    ArrayList<RoomModel> visibleList = new ArrayList<>();
                    for (RoomModel room : roomModels) {
                        if (!room.hide) {
                            visibleList.add(room);
                        }
                    }

                    return visibleList;
                })
                .subscribe(mView::showRooms, throwable -> {
                    mView.showError(throwable.getMessage());
                });

        mSubscriptions.add(sub);
    }

    public void saveRooms(List<RoomModel> rooms) {
        mDataManger.writeRoomsToDb(rooms);
    }

    public ArrayList<RoomModel> getOnlyVisibleRooms(ArrayList<RoomModel> rooms) {
        ArrayList<RoomModel> visibleList = new ArrayList<>();
        for (RoomModel room : rooms) {
            if (!room.hide) {
                visibleList.add(room);
            }
        }

        return visibleList;
    }

    public void loadCachedRooms() {
        mDataManger.getDbRooms().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(roomModels -> {
                    ArrayList<RoomModel> visibleList = new ArrayList<>();
                    for (RoomModel room : roomModels) {
                        if (!room.hide) {
                            visibleList.add(room);
                        }
                    }

                    return visibleList;
                })
                .subscribe(mView::showRooms);
    }
}