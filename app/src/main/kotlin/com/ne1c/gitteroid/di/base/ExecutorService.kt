package com.ne1c.gitteroid.di.base

import rx.Scheduler

interface ExecutorService {
    fun getSubscribeOn(): Scheduler

    fun getObserveOn(): Scheduler
}