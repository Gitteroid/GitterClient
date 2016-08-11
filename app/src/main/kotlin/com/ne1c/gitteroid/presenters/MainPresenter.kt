package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.models.RoomMapper
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.ui.views.MainView
import com.ne1c.gitteroid.utils.RxSchedulersFactory
import com.ne1c.gitteroid.utils.Utils

import java.util.ArrayList

import javax.inject.Inject

import rx.Subscription
import rx.subscriptions.CompositeSubscription

class MainPresenter
@Inject
constructor(private val mSchedulersFactory: RxSchedulersFactory, private val mDataManger: DataManger) : BasePresenter<MainView>() {
    private var mView: MainView? = null
    private var mSubscriptions = CompositeSubscription()

    private val mCachedAllRooms = ArrayList<RoomViewModel>()

    override fun bindView(view: MainView) {
        mView = view
        mSubscriptions = CompositeSubscription()
    }

    override fun unbindView() {
        mView = null
    }

    override fun onDestroy() {
        mSubscriptions.unsubscribe()
    }

    fun loadProfile() {
        val sub = mDataManger.profile.subscribeOn(mSchedulersFactory.io()).observeOn(mSchedulersFactory.androidMainThread()).filter { userModel -> mView != null }.subscribe({ userModel -> mView!!.showProfile(userModel) }) { throwable -> mView!!.showError(R.string.error) }

        mSubscriptions.add(sub)
    }

    fun leaveFromRoom(roomId: String) {
        if (!Utils.instance.isNetworkConnected) {
            mView!!.showError(R.string.no_network)
            return
        }

        val sub = mDataManger.leaveFromRoom(roomId).subscribeOn(mSchedulersFactory.io()).observeOn(mSchedulersFactory.androidMainThread()).filter { response -> mView != null }.subscribe({ response ->
            if (response!!) {
                mView!!.leavedFromRoom()
            }
        }) { throwable -> mView!!.showError(R.string.error) }

        mSubscriptions.add(sub)
    }

    fun loadRooms(fresh: Boolean) {
        if (!Utils.instance.isNetworkConnected) {
            mView!!.showError(R.string.no_network)
        }

        @SuppressWarnings("unchecked")
        val sub = mDataManger.getRooms(fresh).subscribeOn(mSchedulersFactory.io()).observeOn(mSchedulersFactory.androidMainThread()).filter { roomModels -> mView != null }.map<ArrayList<RoomViewModel>>({ roomModels ->
            mCachedAllRooms.clear()
            mCachedAllRooms.addAll(RoomMapper.mapToView(roomModels))

            val rooms = ArrayList<RoomViewModel>()

            // Add rooms with unread messages
            for (model in mCachedAllRooms) {
                if (model.unreadItems > 0) {
                    rooms.add(model)
                }
            }

            // Collect minimum 4 rooms with no oneToOne rooms
            if (rooms.size < 4) {
                for (model in mCachedAllRooms) {
                    if (model.unreadItems <= 0 && !model.oneToOne && rooms.size < 4) {
                        rooms.add(model)
                    }
                }
            }

            // Collect minimum any rooms
            if (rooms.size < 4) {
                for (model in mCachedAllRooms) {
                    if (model.unreadItems <= 0 && model.oneToOne && rooms.size < 4) {
                        rooms.add(model)
                    }
                }
            }

            rooms
        }).subscribe({ rooms ->
            mView!!.showRooms(rooms)
            mView!!.saveAllRooms(mCachedAllRooms)
        }) { throwable -> mView!!.showError(R.string.error_load_rooms) }

        mSubscriptions.add(sub)
    }
}
