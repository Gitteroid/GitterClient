package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.base.ExecutorService
import com.ne1c.gitteroid.models.RoomMapper
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.ui.views.RoomsListView
import com.ne1c.rainbowmvp.base.BasePresenter
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription
import java.util.*
import java.util.concurrent.TimeUnit

class SearchRoomPresenter(private val executor: ExecutorService,
                          private val dataManager: DataManger) : BasePresenter<RoomsListView>() {
    companion object {
        val TAG = SearchRoomPresenter::class.java.simpleName
    }

    // All rooms for edit
    // Set this list to adapter if user will edit
    private val mAllRooms = ArrayList<RoomViewModel>()

    private var mSubscriptions: CompositeSubscription? = null
    private val mSearchRoomSubject = PublishSubject.create<String>()

    override fun bindView(view: RoomsListView) {
        super.bindView(view)
        mSubscriptions = CompositeSubscription()

        val sub = mSearchRoomSubject
                .asObservable()
                .throttleLast(1, TimeUnit.SECONDS)
                .flatMap({ dataManager.searchRooms(it) })
                .map({ response ->
                    // Show without exist in db
                    val responseRooms = response.result ?: ArrayList()
                    val filterRooms = ArrayList<RoomViewModel>()

                    var existInDb: Boolean

                    for (responseRoom in responseRooms) {
                        existInDb = false

                        for (dbRoom in mAllRooms) {
                            if (responseRoom.id == dbRoom.id) {
                                existInDb = true
                                break
                            }
                        }

                        if (!existInDb) {
                            filterRooms.add(RoomMapper.mapToView(responseRoom))
                        }
                    }

                    return@map filterRooms
                })
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .subscribe({ response ->
                    mView?.dismissDialog()
                    mView?.resultSearch(response)
                }) { throwable -> mView?.errorSearch() }

        mSubscriptions?.add(sub)
    }

    override fun onDestroy() {
        mSubscriptions?.unsubscribe()
    }

    val allRooms: List<RoomViewModel>
        get() = mAllRooms

    fun searchRooms(query: String) {
        if (!query.isEmpty()) {
            mView!!.showDialog()
            mSearchRoomSubject.onNext(query)
        } else {
            mView!!.resultSearch(ArrayList<RoomViewModel>())
            mView!!.dismissDialog()
        }
    }
}