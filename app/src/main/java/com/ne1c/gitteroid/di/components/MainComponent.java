package com.ne1c.gitteroid.di.components;

import com.ne1c.gitteroid.di.HasPresenter;
import com.ne1c.gitteroid.di.annotations.PerActivity;
import com.ne1c.gitteroid.di.modules.MainPresenterModule;
import com.ne1c.gitteroid.presenters.MainPresenter;
import com.ne1c.gitteroid.ui.activities.MainActivity;

import dagger.Component;

@PerActivity
@Component(modules = MainPresenterModule.class, dependencies = ApplicationComponent.class)
public interface MainComponent extends HasPresenter<MainPresenter> {
    void inject(MainActivity activity);
}
