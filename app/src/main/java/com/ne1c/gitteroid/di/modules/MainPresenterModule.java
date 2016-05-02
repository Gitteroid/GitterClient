package com.ne1c.gitteroid.di.modules;

import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.di.annotations.PerActivity;
import com.ne1c.gitteroid.presenters.MainPresenter;
import com.ne1c.gitteroid.utils.RxSchedulersFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class MainPresenterModule {
    @PerActivity
    @Provides
    public MainPresenter provideMainPresenter(RxSchedulersFactory factory, DataManger dataManger) {
        return new MainPresenter(factory, dataManger);
    }
}
