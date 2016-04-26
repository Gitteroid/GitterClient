package com.ne1c.developerstalk.presenters;

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.dataproviders.DataManger;
import com.ne1c.developerstalk.models.RoomMapper;
import com.ne1c.developerstalk.models.data.RoomModel;
import com.ne1c.developerstalk.models.view.RoomViewModel;
import com.ne1c.developerstalk.ui.views.RoomsListView;
import com.ne1c.developerstalk.utils.RxSchedulersFactory;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

public class RoomsListPresenter extends BasePresenter<RoomsListView> {
    // All rooms for edit
    // Set this list to adapter if user will edit
    private ArrayList<RoomViewModel> mAllRooms = new ArrayList<>();

    private RoomsListView mView;

    private DataManger mDataManger;
    private RxSchedulersFactory mSchedulersFactory;

    private CompositeSubscription mSubscriptions;
    private PublishSubject<String> mSearchRoomSubject = PublishSubject.create();

    @Inject
    public RoomsListPresenter(RxSchedulersFactory factory, DataManger dataManger) {
        mSchedulersFactory = factory;
        mDataManger = dataManger;
    }

    @Override
    public void bindView(RoomsListView view) {
        mView = view;

        Subscription sub = mSearchRoomSubject
                .asObservable()
                .throttleLast(1, TimeUnit.SECONDS)
                .flatMap(query -> mDataManger.searchRooms(query))
                .map(response -> {
                    // Show without exist in db
                    ArrayList<RoomModel> responseRooms = response.getResult();
                    ArrayList<RoomViewModel> filterRooms = new ArrayList<>();

                    boolean existInDb;

                    for (RoomModel responseRoom : responseRooms) {
                        existInDb = false;

                        for (RoomViewModel dbRoom : mAllRooms) {
                            if (responseRoom.id.equals(dbRoom.id)) {
                                existInDb = true;
                                break;
                            }
                        }

                        if (!existInDb) {
                            filterRooms.add(RoomMapper.mapToView(responseRoom));
                        }
                    }

                    return filterRooms;
                })
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(response -> {
                    mView.dismissDialog();
                    mView.resultSearch(response);
                }, throwable -> {
                    mView.errorSearch();
                });

        mSubscriptions.add(sub);
    }

    @Override
    public void unbindView() {
        mView = null;
    }

    @Override
    public void onCreate() {
        mSubscriptions = new CompositeSubscription();
    }

    @Override
    public void onDestroy() {
        mSubscriptions.unsubscribe();
    }

    public List<RoomViewModel> getAllRooms() {
        return mAllRooms;
    }

    public void loadRooms(boolean fresh) {
        if (!Utils.getInstance().isNetworkConnected()) {
            mView.showError(R.string.no_network);
        }

        @SuppressWarnings("unchecked")
        Subscription sub = mDataManger.getRooms(fresh)
                .map(roomModels -> {
                    mAllRooms = (ArrayList<RoomViewModel>) roomModels.clone();

                    ArrayList<RoomViewModel> visibleList = new ArrayList<>();

                    for (RoomModel room : roomModels) {
                        if (!room.hide) {
                            visibleList.add(RoomMapper.mapToView(room));
                        }
                    }

                    return visibleList;
                })
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(mView::showRooms, throwable -> {
                    mView.showError(R.string.error);
                });

        mSubscriptions.add(sub);
    }

    public void saveRooms(ArrayList<RoomViewModel> rooms) {
        mDataManger.updateRooms(rooms, true);
    }

    public ArrayList<RoomViewModel> getOnlyVisibleRooms(ArrayList<RoomViewModel> rooms) {
        ArrayList<RoomViewModel> visibleList = new ArrayList<>();
        for (RoomViewModel room : rooms) {
            if (!room.hide) {
                visibleList.add(room);
            }
        }

        return visibleList;
    }

    public void searchRooms(String query) {
        if (!query.isEmpty()) {
            mView.showDialog();
            mSearchRoomSubject.onNext(query);
        } else {
            mView.resultSearch(new ArrayList<>());
            mView.dismissDialog();
        }
    }

    private class SearchModel {
        String query;
        int offset;

        public SearchModel(String query, int offset) {
            this.query = query;
            this.offset = offset;
        }
    }
}