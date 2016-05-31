package com.ne1c.gitteroid.presenters;

import com.ne1c.gitteroid.R;
import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.models.RoomMapper;
import com.ne1c.gitteroid.models.data.RoomModel;
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
        mSubscriptions.unsubscribe();
        mView = null;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {

    }

    public void loadProfile() {
        Subscription sub = mDataManger.getProfile()
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(userModel -> {
                    mView.showProfile(userModel);
                }, throwable -> { mView.showError(R.string.error); });

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
                .map(roomModels -> {
                    ArrayList<RoomViewModel> visibleList = new ArrayList<>();
                    for (RoomModel room : roomModels) {
                        if (!room.hide) {
                            visibleList.add(RoomMapper.mapToView(room));
                        }
                    }

                    return visibleList;
                })
                .subscribe(mView::showRooms, throwable -> {
                    mView.showError(R.string.error_load_rooms);
                });

        mSubscriptions.add(sub);
    }
}
