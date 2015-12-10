package com.ne1c.developerstalk.presenters;

import com.ne1c.developerstalk.api.GitterApi;
import com.ne1c.developerstalk.database.ClientDatabase;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.ui.views.RoomsListView;
import com.ne1c.developerstalk.utils.Utils;

import java.util.Collections;
import java.util.List;

import retrofit.RestAdapter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class RoomsListPresenter extends BasePresenter<RoomsListView> {
    // All rooms for edit
    // Set this list to adapter if user will edit
    private List<RoomModel> mAllRooms = Collections.emptyList();

    private RoomsListView mView;

    private GitterApi mApi;
    private ClientDatabase mClientDatabase;

    private CompositeSubscription mSubscriptions;

    @Override
    public void bindView(RoomsListView view) {
        mView = view;
        mApi = new RestAdapter.Builder()
                .setEndpoint(Utils.GITTER_API_URL)
                .build()
                .create(GitterApi.class);
        mClientDatabase = new ClientDatabase(mView.getAppContext());

        mSubscriptions = new CompositeSubscription();
    }

    @Override
    public void unbindView() {
        mView = null;
        mSubscriptions.unsubscribe();
    }

    public void loadCachedRooms() {
        Subscription sub = mClientDatabase.getRooms()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(roomModels -> {
                    Collections.sort(roomModels, new RoomModel.SortedByPosition());
                    mView.showRooms(roomModels);
                });

        mSubscriptions.add(sub);
    }


    public List<RoomModel> getAllRooms() {
        return mAllRooms;
    }

    public void refreshRooms() {
        Observable<List<RoomModel>> serverRooms = mApi.getCurrentUserRooms(Utils.getInstance().getBearer());
        Observable<List<RoomModel>> dbRooms = mClientDatabase.getRooms();

        Subscription sub = Observable.combineLatest(serverRooms, dbRooms, (server, db) -> {
            for (RoomModel r1 : db) {
                for (RoomModel r2 : server) {
                    if (r1.id.equals(r2.id)) {
                        r2.hide = r1.hide;
                        r2.listPosition = r1.listPosition;
                    }
                }
            }

            mAllRooms = server;
            mClientDatabase.insertRooms(server);
            Collections.sort(server, new RoomModel.SortedByPosition());

            return server;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mView::showRooms);

        mSubscriptions.add(sub);
    }

    public void saveRooms(List<RoomModel> rooms) {
        mView.showDialog();

        Subscription sub = Observable.create(subscriber -> {
            mClientDatabase.insertRooms(rooms);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(roomModels -> {
                    mView.dismissDialog();
                });

        mSubscriptions.add(sub);
    }
}