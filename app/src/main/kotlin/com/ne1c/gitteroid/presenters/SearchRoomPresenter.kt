package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.base.ExecutorService
import com.ne1c.gitteroid.models.RoomMapper
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.ui.views.SearchRoomsView
import com.ne1c.rainbowmvp.base.BasePresenter
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription
import java.util.*
import java.util.concurrent.TimeUnit

class SearchRoomPresenter(private val executor: ExecutorService,
                          private val dataManager: DataManger) : BasePresenter<SearchRoomsView>() {
    companion object {
        val TAG: String = SearchRoomPresenter::class.java.simpleName
    }

    private var mSubscriptions: CompositeSubscription? = null
    private val mSearchRoomSubject = PublishSubject.create<QueryModel>()

    private var lastQueryModel: QueryModel? = null

    override fun bindView(view: SearchRoomsView) {
        super.bindView(view)
        mSubscriptions = CompositeSubscription()

        val sub = mSearchRoomSubject
                .asObservable()
                .throttleLast(1, TimeUnit.SECONDS)
                .flatMap { dataManager.searchRooms(it.query, it.offset) }
                .map { return@map RoomMapper.mapToView(it.result!!) }
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .subscribe({ response ->
                    if (lastQueryModel!!.offset > 0) {
                        mView.paginationResultSearch(response)
                    } else {
                        mView?.dismissDialog()
                        mView?.resultSearch(response)
                    }
                }) { throwable -> mView?.errorSearch() }

        mSubscriptions?.add(sub)
    }

    override fun onDestroy() {
        mSubscriptions?.unsubscribe()
    }

    fun searchRooms(query: String, offset: Int = 0) {
        if (!query.isEmpty()) {
            if (offset == 0) {
                mView?.showDialog()
            }

            lastQueryModel = QueryModel(query, offset)
            mSearchRoomSubject.onNext(lastQueryModel)
        } else {
            mView?.resultSearch(ArrayList<RoomViewModel>())
            mView?.dismissDialog()
        }
    }

    private class QueryModel(val query: String, val offset: Int)
}