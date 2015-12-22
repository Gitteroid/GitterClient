package com.ne1c.developerstalk.presenters;

import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.models.UserModel;
import com.ne1c.developerstalk.services.DataManger;
import com.ne1c.developerstalk.ui.views.MainView;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainPresenter extends BasePresenter<MainView> {
    private MainView mView;
    private CompositeSubscription mSubscriptions;

    private DataManger mDataManger;

    @Override
    public void bindView(MainView view) {
        mView = view;

        mDataManger = new DataManger(mView.getAppContext());
        mSubscriptions = new CompositeSubscription();
    }

    @Override
    public void unbindView() {
        mView = null;
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

    public void loadProfile() {
        mDataManger.getProfile()
                .subscribeOn(Schedulers.io())
                .map(userModels -> {
                    UserModel user = userModels.get(0);
                    if (Utils.getInstance().getUserPref().id.isEmpty()) {
                        Utils.getInstance().writeUserToPref(user);
                    } else {
                        Utils.getInstance().writeUserToPref(user);
                    }

                    return user;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userModel -> {

                    mView.showProfile(userModel);

                    // Update avatar
                    Glide.with(mView.getAppContext()).load(userModel.avatarUrlMedium).asBitmap()
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    mView.updatePhoto(resource);
                                }
                            });
                }, throwable -> {
                    mView.showError(throwable.getMessage());
                    mView.showProfile(Utils.getInstance().getUserPref());

                });
    }

    public void leaveFromRoom(String roomId) {
        mDataManger.leaveFromRoom(roomId).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    mView.leavedFromRoom();
                }, (throwable -> {
                    mView.showError(throwable.getMessage());
                }));
    }

    public void loadRooms() {
        if (!Utils.getInstance().isNetworkConnected()) {
            mView.showError(mView.getAppContext().getString(R.string.no_network));
        }

        @SuppressWarnings("unchecked")
        Subscription sub = mDataManger.getRooms().subscribeOn(Schedulers.io())
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
                .subscribe(mView::showRooms, throwable -> {
                    mView.showError(throwable.getMessage());
                });

        mSubscriptions.add(sub);
    }
}
