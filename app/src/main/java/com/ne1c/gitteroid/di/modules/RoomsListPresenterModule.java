package com.ne1c.gitteroid.di.modules;

import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.di.annotations.PerFragment;
import com.ne1c.gitteroid.presenters.RoomsListPresenter;
import com.ne1c.gitteroid.utils.RxSchedulersFactory;

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
