package com.ne1c.gitteroid.di.modules;

import com.ne1c.gitteroid.di.annotations.PerApplication;
import com.ne1c.gitteroid.utils.RxSchedulersFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class RxSchedulersModule {
    @PerApplication
    @Provides
    RxSchedulersFactory provideRxSchedulers() {
        return new RxSchedulersFactory();
    }
}
