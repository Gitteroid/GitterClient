package com.ne1c.developerstalk.di.modules;

import android.content.Context;

import com.ne1c.developerstalk.di.annotations.PerFragment;
import com.ne1c.developerstalk.presenters.RoomsListPresenter;

import dagger.Module;
import dagger.Provides;

@Module
public class RoomsListPresenterModule {
    @PerFragment
    @Provides
    public RoomsListPresenter provideRoomsListPresenter(Context context) {
        return new RoomsListPresenter(context);
    }
}
