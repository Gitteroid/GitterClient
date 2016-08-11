package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.models.RoomMapper
import com.ne1c.gitteroid.models.data.RoomModel
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.ui.views.RoomsListView
import com.ne1c.gitteroid.utils.RxSchedulersFactory

import java.util.ArrayList
import java.util.concurrent.TimeUnit

import javax.inject.Inject

import rx.Subscription
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription

class SearchRoomPresenter
@Inject
constructor(private val mSchedulersFactory: RxSchedulersFactory, private val mDataManger: DataManger) : BasePresenter<RoomsListView>() {
    // All rooms for edit
    // Set this list to adapter if user will edit
    private val mAllRooms = ArrayList<RoomViewModel>()

    private var mView: RoomsListView? = null

    private var mSubscriptions: CompositeSubscription? = null
    private val mSearchRoomSubject = PublishSubject.create<String>()

    override fun bindView(view: RoomsListView) {
        mView = view
        mSubscriptions = CompositeSubscription()

        val sub = mSearchRoomSubject.asObservable().throttleLast(1, TimeUnit.SECONDS).flatMap<SearchRoomsResponse>({ query -> mDataManger.searchRooms(query) }).map<ArrayList<RoomViewModel>>({ response ->
            // Show without exist in db
            val responseRooms = response.result
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

            filterRooms
        }).subscribeOn(mSchedulersFactory.io()).observeOn(mSchedulersFactory.androidMainThread()).subscribe({ response ->
            mView!!.dismissDialog()
            mView!!.resultSearch(response)
        }) { throwable -> mView!!.errorSearch() }

        mSubscriptions!!.add(sub)
    }

    override fun unbindView() {
        mSubscriptions!!.unsubscribe()

        mView = null
    }

    override fun onDestroy() {

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