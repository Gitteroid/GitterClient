package com.ne1c.developerstalk.di.modules;

import com.ne1c.developerstalk.di.annotations.PerActivity;
import com.ne1c.developerstalk.presenters.MainPresenter;
import com.ne1c.developerstalk.dataproviders.DataManger;
import com.ne1c.developerstalk.utils.RxSchedulersFactory;

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
