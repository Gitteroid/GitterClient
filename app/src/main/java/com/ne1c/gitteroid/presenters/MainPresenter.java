package com.ne1c.gitteroid.presenters;

import com.ne1c.gitteroid.R;
import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.models.RoomMapper;
import com.ne1c.gitteroid.models.view.RoomViewModel;
import com.ne1c.gitteroid.ui.views.MainView;
import com.ne1c.gitteroid.utils.RxSchedulersFactory;
import com.ne1c.gitteroid.utils.Utils;

import java.util.ArrayList;

import javax.inject.Inject;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class MainPresenter extends BasePresenter<MainView> {
    private MainView mView;
    private CompositeSubscription mSubscriptions = new CompositeSubscription();

    private DataManger mDataManger;
    private RxSchedulersFactory mSchedulersFactory;

    private ArrayList<RoomViewModel> mCachedAllRooms = new ArrayList<>();

    @Inject
    public MainPresenter(RxSchedulersFactory factory, DataManger dataManger) {
        mSchedulersFactory = factory;
        mDataManger = dataManger;
    }

    @Override
    public void bindView(MainView view) {
        mView = view;
        mSubscriptions = new CompositeSubscription();
    }

    @Override
    public void unbindView() {
        mView = null;
    }

    @Override
    public void onDestroy() {
        mSubscriptions.unsubscribe();
    }

    public void loadProfile() {
        Subscription sub = mDataManger.getProfile()
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .filter(userModel -> mView != null)
                .subscribe(userModel -> {
                    mView.showProfile(userModel);
                }, throwable -> {
                    mView.showError(R.string.error);
                });

        mSubscriptions.add(sub);
    }

    public void leaveFromRoom(String roomId) {
        if (!Utils.getInstance().isNetworkConnected()) {
            mView.showError(R.string.no_network);
            return;
        }

        Subscription sub = mDataManger.leaveFromRoom(roomId)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .filter(response -> mView != null)
                .subscribe(response -> {
                    if (response) {
                        mView.leavedFromRoom();
                    }
                }, (throwable -> {
                    mView.showError(R.string.error);
                }));

        mSubscriptions.add(sub);
    }

    public void loadRooms(boolean fresh) {
        if (!Utils.getInstance().isNetworkConnected()) {
            mView.showError(R.string.no_network);
        }

        @SuppressWarnings("unchecked")
        Subscription sub = mDataManger.getRooms(fresh)
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .filter(roomModels -> mView != null)
                .map(roomModels -> {
                    mCachedAllRooms.clear();
                    mCachedAllRooms.addAll(RoomMapper.mapToView(roomModels));

                    ArrayList<RoomViewModel> rooms = new ArrayList<>();

                    // Add rooms with unread messages
                    for (RoomViewModel model : mCachedAllRooms) {
                        if (model.unreadItems > 0) {
                            rooms.add(model);
                        }
                    }

                    // Collect minimum 4 rooms with no oneToOne rooms
                    if (rooms.size() < 4) {
                        for (RoomViewModel model : mCachedAllRooms) {
                            if (model.unreadItems <= 0 && !model.oneToOne && rooms.size() < 4) {
                                rooms.add(model);
                            }
                        }
                    }

                    // Collect minimum any rooms
                    if (rooms.size() < 4) {
                        for (RoomViewModel model : mCachedAllRooms) {
                            if (model.unreadItems <= 0 && model.oneToOne && rooms.size() < 4) {
                                rooms.add(model);
                            }
                        }
                    }

                    return rooms;
                })
                .subscribe(rooms -> {
                    mView.showRooms(rooms);
                    mView.saveAllRooms(mCachedAllRooms);
                }, throwable -> {
                    mView.showError(R.string.error_load_rooms);
                });

        mSubscriptions.add(sub);
    }
}
