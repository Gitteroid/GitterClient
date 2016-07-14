package com.ne1c.gitteroid.presenters;


import com.ne1c.gitteroid.utils.RxSchedulersFactory;

import rx.Scheduler;
import rx.schedulers.Schedulers;

public class TestRxSchedulerFactory extends RxSchedulersFactory {
    @Override
    public Scheduler io() {
        return Schedulers.immediate();
    }

    @Override
    public Scheduler androidMainThread() {
        return Schedulers.immediate();
    }
}
