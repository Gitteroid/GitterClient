package com.ne1c.developerstalk;

import com.ne1c.developerstalk.utils.RxSchedulersFactory;

import rx.Scheduler;
import rx.schedulers.Schedulers;

public class MockRxSchedulersFactory extends RxSchedulersFactory {
    @Override
    public Scheduler io() {
        return Schedulers.immediate();
    }

    @Override
    public Scheduler androidMainThread() {
        return Schedulers.immediate();
    }
}
