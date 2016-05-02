package com.ne1c.gitteroid.utils;

import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class RxSchedulersFactory {
    public Scheduler io() {
        return Schedulers.io();
    }

    public Scheduler androidMainThread() {
        return AndroidSchedulers.mainThread();
    }
}
