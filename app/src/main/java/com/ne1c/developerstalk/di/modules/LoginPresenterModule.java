package com.ne1c.developerstalk.di.modules;

import com.ne1c.developerstalk.di.annotations.PerFragment;
import com.ne1c.developerstalk.presenters.LoginPresenter;

import dagger.Module;
import dagger.Provides;

@Module
public class LoginPresenterModule {
    @PerFragment
    @Provides
    public LoginPresenter provideLoginPresenter() {
        return new LoginPresenter();
    }
}
