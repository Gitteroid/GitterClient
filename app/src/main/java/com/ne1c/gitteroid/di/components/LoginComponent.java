package com.ne1c.gitteroid.di.components;

import com.ne1c.gitteroid.di.HasPresenter;
import com.ne1c.gitteroid.di.annotations.PerActivity;
import com.ne1c.gitteroid.di.modules.LoginPresenterModule;
import com.ne1c.gitteroid.presenters.LoginPresenter;
import com.ne1c.gitteroid.ui.activities.LoginActivity;

import dagger.Component;

@PerActivity
@Component(modules = LoginPresenterModule.class, dependencies = ApplicationComponent.class)
public interface LoginComponent extends HasPresenter<LoginPresenter> {
    void inject(LoginActivity activity);
}
