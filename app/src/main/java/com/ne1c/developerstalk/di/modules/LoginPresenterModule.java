package com.ne1c.developerstalk.di.modules;

import com.ne1c.developerstalk.di.annotations.PerActivity;
import com.ne1c.developerstalk.presenters.LoginPresenter;

import dagger.Module;
import dagger.Provides;

@Module
public class LoginPresenterModule {
    @PerActivity
    @Provides
    public LoginPresenter provideLoginPresenter() {
        return new LoginPresenter();
    }
}
