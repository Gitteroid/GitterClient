package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.base.ExecutorService
import com.ne1c.gitteroid.di.base.NetworkService
import com.ne1c.gitteroid.models.RoomMapper
import com.ne1c.gitteroid.models.data.UserModel
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.ui.views.MainView
import com.ne1c.rainbowmvp.base.BasePresenter
import rx.subscriptions.CompositeSubscription
import java.util.*

class MainPresenter : BasePresenter<MainView> {
    companion object {
        val TAG = MainPresenter::class.java.simpleName
    }

    private val executor: ExecutorService
    private val dataManager: DataManger
    private val networkService: NetworkService

    private var subscriptions = CompositeSubscription()

    private val cachedAllRooms = ArrayList<RoomViewModel>()
    private var profileModel: UserModel? = null

    constructor(executor: ExecutorService, dataManger: DataManger,
                networkService: NetworkService) : super() {
        this.executor = executor
        this.dataManager = dataManger
        this.networkService = networkService

        subscriptions = CompositeSubscription()
    }

    override fun onDestroy() {
        subscriptions.unsubscribe()
    }

    fun loadProfile() {
        if (profileModel != null) {
            mView?.showProfile(profileModel!!)
        }

        val sub = dataManager.getProfile()
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .map {
                    profileModel = it
                    return@map it
                }
                .subscribe({ mView?.showProfile(it) }, { mView?.showError(R.string.error) })

        subscriptions.add(sub)
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

        subscriptions.add(sub)
    }

    fun loadRooms() {
        if (!networkService.isConnected()) {
            mView!!.showError(R.string.no_network)
        }

        @SuppressWarnings("unchecked")
        val sub = dataManager.getRooms(networkService.isConnected())
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .map({ roomModels ->
                    cachedAllRooms.clear()
                    cachedAllRooms.addAll(RoomMapper.mapToView(roomModels))

                    val rooms = ArrayList<RoomViewModel>()

                    // Add rooms with unread messages
                    for (model in cachedAllRooms) {
                        if (model.unreadItems > 0) {
                            rooms.add(model)
                        }
                    }

                    // Collect minimum 4 rooms with no oneToOne rooms
                    if (rooms.size < 4) {
                        for (model in cachedAllRooms) {
                            if (model.unreadItems <= 0 && !model.oneToOne && rooms.size < 4) {
                                rooms.add(model)
                            }
                        }
                    }

                    // Collect minimum any rooms
                    if (rooms.size < 4) {
                        for (model in cachedAllRooms) {
                            if (model.unreadItems <= 0 && model.oneToOne && rooms.size < 4) {
                                rooms.add(model)
                            }
                        }
                    }

                    rooms
                }).subscribe({
            mView?.showRooms(it)
            mView?.saveAllRooms(cachedAllRooms)
        }, {
            mView?.showError(R.string.error_load_rooms)
            mView?.errorLoadRooms()
        })

        subscriptions.add(sub)
    }
}
