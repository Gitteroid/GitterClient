package com.ne1c.gitteroid.di.modules;

import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.di.annotations.PerActivity;
import com.ne1c.gitteroid.presenters.LoginPresenter;
import com.ne1c.gitteroid.utils.RxSchedulersFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class LoginPresenterModule {
    @PerActivity
    @Provides
    public LoginPresenter provideLoginPresenter(RxSchedulersFactory factory, DataManger dataManger) {
        return new LoginPresenter(factory, dataManger);
    }
}
