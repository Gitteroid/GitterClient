package com.ne1c.gitteroid

import com.ne1c.gitteroid.di.base.ExecutorService
import rx.Scheduler
import rx.schedulers.Schedulers

class TestExecutorService: ExecutorService {
    override fun getSubscribeOn(): Scheduler = Schedulers.io()

    override fun getObserveOn(): Scheduler = Schedulers.io()
}