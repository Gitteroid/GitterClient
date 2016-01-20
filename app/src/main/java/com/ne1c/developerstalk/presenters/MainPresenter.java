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
import com.ne1c.developerstalk.utils.RxSchedulersFactory;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;

import javax.inject.Inject;

import rx.Subscription;
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
        Subscription sub = mDataManger.getDbRooms().subscribeOn(mSchedulersFactory.io())
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
                .subscribe(mView::showRooms);

        mSubscriptions.add(sub);
    }

    public void loadProfile() {
        Subscription sub = mDataManger.getProfile()
                .subscribeOn(mSchedulersFactory.io())
                .map(userModels -> {
                    UserModel user = userModels.get(0);
                    if (Utils.getInstance().getUserPref().id.isEmpty()) {
                        Utils.getInstance().writeUserToPref(user);
                    } else {
                        Utils.getInstance().writeUserToPref(user);
                    }

                    return user;
                })
                .observeOn(mSchedulersFactory.androidMainThread())
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
                    if (!throwable.getMessage().contains("Unable to resolve") &&
                            !throwable.getMessage().contains("timeout")) {
                        mView.showError(mView.getAppContext().getString(R.string.error));
                    } else {
                        mView.showError(mView.getAppContext().getString(R.string.no_network));
                    }

                    mView.showProfile(Utils.getInstance().getUserPref());

                });

        mSubscriptions.add(sub);
    }

    public void leaveFromRoom(String roomId) {
        Subscription sub = mDataManger.leaveFromRoom(roomId).subscribeOn(mSchedulersFactory.io())
                .observeOn(mSchedulersFactory.androidMainThread())
                .subscribe(response -> {
                    mView.leavedFromRoom();
                }, (throwable -> {
                    if (!throwable.getMessage().contains("Unable to resolve") &&
                            !throwable.getMessage().contains("timeout")) {
                        mView.showError(mView.getAppContext().getString(R.string.error));
                    } else {
                        mView.showError(mView.getAppContext().getString(R.string.no_network));
                    }
                }));

        mSubscriptions.add(sub);
    }

    public void loadRooms() {
        @SuppressWarnings("unchecked")
        Subscription sub = mDataManger.getRooms().subscribeOn(mSchedulersFactory.io())
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
                    if (throwable.getMessage().contains("Unable to resolve") ||
                            throwable.getMessage().contains("timeout")) {
                        mView.showError(mView.getAppContext().getString(R.string.no_network));
                    }
                });

        mSubscriptions.add(sub);
    }
}
