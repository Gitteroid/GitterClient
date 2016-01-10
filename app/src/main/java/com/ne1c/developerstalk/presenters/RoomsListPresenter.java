package com.ne1c.developerstalk.presenters;

import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.services.DataManger;
import com.ne1c.developerstalk.ui.views.RoomsListView;
import com.ne1c.developerstalk.utils.RxSchedulersFactory;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class RoomsListPresenter extends BasePresenter<RoomsListView> {
    // All rooms for edit
    // Set this list to adapter if user will edit
    private ArrayList<RoomModel> mAllRooms = new ArrayList<>();

    private RoomsListView mView;

    private DataManger mDataManger;
    private RxSchedulersFactory mSchedulersFactory;

    private CompositeSubscription mSubscriptions;

    @Inject
    public RoomsListPresenter(RxSchedulersFactory factory, DataManger dataManger) {
        mSchedulersFactory = factory;
        mDataManger = dataManger;
    }

    @Override
    public void bindView(RoomsListView view) {
        mView = view;

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
        @SuppressWarnings("unchecked")
        Subscription sub = mDataManger.getRooms().subscribeOn(mSchedulersFactory.io())
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
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(mView::showRooms, throwable -> {
                    if (!throwable.getMessage().contains("Unable to resolve") &&
                            !throwable.getMessage().contains("timeout")) {
                        mView.showError(throwable.getMessage());
                    }
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
        mDataManger.getDbRooms().subscribeOn(mSchedulersFactory.io())
                .map(roomModels -> {
                    ArrayList<RoomModel> visibleList = new ArrayList<>();
                    for (RoomModel room : roomModels) {
                        if (!room.hide) {
                            visibleList.add(room);
                        }
                    }

                    return visibleList;
                })
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(mView::showRooms, throwable -> {
                    if (!throwable.getMessage().contains("Unable to resolve") &&
                            !throwable.getMessage().contains("timeout")) {
                        mView.showError(throwable.getMessage());
                    }
                });
    }
}