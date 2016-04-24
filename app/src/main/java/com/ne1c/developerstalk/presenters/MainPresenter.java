package com.ne1c.developerstalk.presenters;

import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.dataproviders.DataManger;
import com.ne1c.developerstalk.models.data.RoomModel;
import com.ne1c.developerstalk.models.data.UserModel;
import com.ne1c.developerstalk.ui.views.MainView;
import com.ne1c.developerstalk.utils.RxSchedulersFactory;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

public class MainPresenter extends BasePresenter<MainView> {
    private MainView mView;
    private CompositeSubscription mSubscriptions;

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

    public void loadCachedRooms() {
        Subscription sub = mDataManger.getDbRooms()
                .map(roomModels -> {
                    ArrayList<RoomModel> visibleList = new ArrayList<>();
                    for (RoomModel room : roomModels) {
                        if (!room.hide) {
                            visibleList.add(room);
                        }
                    }

                    return visibleList;
                })
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(mView::showRooms);

        mSubscriptions.add(sub);
    }

    public void loadProfile() {
        Subscription sub = mDataManger.getProfile()
                .onErrorResumeNext(throwable -> {
                    ArrayList<UserModel> user = new ArrayList<UserModel>();
                    user.add(Utils.getInstance().getUserPref());
                    return Observable.just(user);
                })
                .map(userModels -> {
                    UserModel user = userModels.get(0);
                    Utils.getInstance().writeUserToPref(user);

                    return user;
                })
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(userModel -> {
                    mView.showProfile(userModel);

                    // Update avatar
                    Glide.with(Utils.getInstance().getContext()).load(userModel.avatarUrlMedium).asBitmap()
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    if (mView != null) {
                                        mView.updatePhoto(resource);
                                    }
                                }
                            });
                }, throwable -> {
                    mView.showProfile(Utils.getInstance().getUserPref());
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
                .subscribe(response -> {
                    if (response.success) {
                        mView.leavedFromRoom();
                    }
                }, (throwable -> {
                    mView.showError(R.string.error);
                }));

        mSubscriptions.add(sub);
    }

    public void loadRooms() {
        if (!Utils.getInstance().isNetworkConnected()) {
            mView.showError(R.string.no_network);
            return;
        }

        @SuppressWarnings("unchecked")
        Subscription sub = mDataManger.getRooms()
                .subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .map(roomModels -> {
                    ArrayList<RoomModel> visibleList = new ArrayList<>();
                    for (RoomModel room : roomModels) {
                        if (!room.hide) {
                            visibleList.add(room);
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
