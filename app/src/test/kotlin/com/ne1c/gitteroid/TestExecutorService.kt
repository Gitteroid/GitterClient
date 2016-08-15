package com.ne1c.gitteroid

import com.ne1c.gitteroid.di.base.ExecutorService
import rx.Scheduler
import rx.schedulers.Schedulers

class TestExecutorService: ExecutorService {
    override fun getSubscribeOn(): Scheduler = Schedulers.immediate()

    override fun getObserveOn(): Scheduler = Schedulers.immediate()
}