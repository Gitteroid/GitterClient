package com.ne1c.gitteroid.di.components;

import com.ne1c.gitteroid.di.HasPresenter;
import com.ne1c.gitteroid.di.annotations.PerFragment;
import com.ne1c.gitteroid.di.modules.RoomsListPresenterModule;
import com.ne1c.gitteroid.presenters.RoomsListPresenter;
import com.ne1c.gitteroid.ui.fragments.RoomsListFragment;

import dagger.Component;

@PerFragment
@Component(modules = RoomsListPresenterModule.class, dependencies = ApplicationComponent.class)
public interface RoomsListComponent extends HasPresenter<RoomsListPresenter> {
    void inject(RoomsListFragment fragment);
}
