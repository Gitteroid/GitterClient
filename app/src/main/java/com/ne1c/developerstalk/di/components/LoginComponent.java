package com.ne1c.developerstalk.di.components;

import com.ne1c.developerstalk.di.annotations.PerActivity;
import com.ne1c.developerstalk.di.modules.LoginPresenterModule;
import com.ne1c.developerstalk.di.HasPresenter;
import com.ne1c.developerstalk.presenters.LoginPresenter;
import com.ne1c.developerstalk.ui.activities.LoginActivity;

import dagger.Component;

@PerActivity
@Component(modules = LoginPresenterModule.class, dependencies = ApplicationComponent.class)
public interface LoginComponent extends HasPresenter<LoginPresenter> {
    void inject(LoginActivity activity);
}
