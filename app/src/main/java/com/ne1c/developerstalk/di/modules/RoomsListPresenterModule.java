package com.ne1c.developerstalk.di.modules;

import com.ne1c.developerstalk.di.annotations.PerFragment;
import com.ne1c.developerstalk.presenters.RoomsListPresenter;
import com.ne1c.developerstalk.dataprovides.DataManger;
import com.ne1c.developerstalk.utils.RxSchedulersFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class RoomsListPresenterModule {
    @PerFragment
    @Provides
    public RoomsListPresenter provideRoomsListPresenter(RxSchedulersFactory factory, DataManger dataManger) {
        return new RoomsListPresenter(factory, dataManger);
    }
}
