package com.ne1c.developerstalk.di.modules;

import com.ne1c.developerstalk.di.annotations.PerApplication;
import com.ne1c.developerstalk.utils.RxSchedulersFactory;

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
