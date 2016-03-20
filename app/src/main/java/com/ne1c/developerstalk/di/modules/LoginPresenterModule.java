package com.ne1c.developerstalk.di.modules;

import com.ne1c.developerstalk.dataprovides.DataManger;
import com.ne1c.developerstalk.di.annotations.PerActivity;
import com.ne1c.developerstalk.presenters.LoginPresenter;
import com.ne1c.developerstalk.utils.RxSchedulersFactory;

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
