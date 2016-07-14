package com.ne1c.gitteroid.di.modules;

import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.di.annotations.PerFragment;
import com.ne1c.gitteroid.presenters.SearchRoomPresenter;
import com.ne1c.gitteroid.utils.RxSchedulersFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class SearchRoomPresenterModule {
    @PerFragment
    @Provides
    public SearchRoomPresenter provideRoomsListPresenter(RxSchedulersFactory factory, DataManger dataManger) {
        return new SearchRoomPresenter(factory, dataManger);
    }
}
