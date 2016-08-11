package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.base.ExecutorService
import com.ne1c.gitteroid.di.base.NetworkService
import com.ne1c.gitteroid.models.RoomMapper
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.ui.views.MainView
import com.ne1c.rainbowmvp.base.BasePresenter
import rx.subscriptions.CompositeSubscription
import java.util.*

class MainPresenter(private val executor: ExecutorService,
                    private val dataManager: DataManger,
                    private val networkService: NetworkService) : BasePresenter<MainView>() {
    companion object {
        val TAG = MainPresenter::class.java.simpleName
    }

    private var mSubscriptions = CompositeSubscription()

    private val mCachedAllRooms = ArrayList<RoomViewModel>()

    override fun bindView(view: MainView) {
        super.bindView(view)
        mSubscriptions = CompositeSubscription()
    }

    override fun unbindView() {
        mView = null
    }

    override fun onDestroy() {
        mSubscriptions.unsubscribe()
    }

    fun loadProfile() {
        val sub = dataManager.getProfile()
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .subscribe({ mView?.showProfile(it) },
                        { mView?.showError(R.string.error) })

        mSubscriptions.add(sub)
    }

    fun leaveFromRoom(roomId: String) {
        if (!networkService.isConnected()) {
            mView!!.showError(R.string.no_network)
            return
        }

        val sub = dataManager.leaveFromRoom(roomId)
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .subscribe({ response ->
                    if (response) {
                        mView?.leavedFromRoom()
                    }
                }, { throwable -> mView?.showError(R.string.error) })

        mSubscriptions.add(sub)
    }

    fun loadRooms(fresh: Boolean) {
        if (!networkService.isConnected()) {
            mView!!.showError(R.string.no_network)
        }

        @SuppressWarnings("unchecked")
        val sub = dataManager.getRooms(fresh)
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .map({ roomModels ->
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
                }).subscribe({
            mView?.showRooms(it)
            mView?.saveAllRooms(mCachedAllRooms)
        }, { throwable -> mView?.showError(R.string.error_load_rooms) })

        mSubscriptions.add(sub)
    }
}
