package com.ne1c.gitteroid.utils

import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

open class RxSchedulersFactory {
    open fun io(): Scheduler {
        return Schedulers.io()
    }

    open fun androidMainThread(): Scheduler {
        return AndroidSchedulers.mainThread()
    }
}
