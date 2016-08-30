package com.ne1c.gitteroid.di

import com.ne1c.gitteroid.di.base.ExecutorService
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class RxExecutorService : ExecutorService {
    override fun getObserveOn(): Scheduler = AndroidSchedulers.mainThread()

    override fun getSubscribeOn(): Scheduler = Schedulers.io()
}