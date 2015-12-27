package com.ne1c.developerstalk.di.components;

import com.ne1c.developerstalk.di.annotations.PerFragment;
import com.ne1c.developerstalk.di.modules.RoomsListPresenterModule;
import com.ne1c.developerstalk.di.HasPresenter;
import com.ne1c.developerstalk.presenters.RoomsListPresenter;
import com.ne1c.developerstalk.ui.fragments.RoomsListFragment;

import dagger.Component;

@PerFragment
@Component(modules = RoomsListPresenterModule.class, dependencies = ApplicationComponent.class)
public interface RoomsListComponent extends HasPresenter<RoomsListPresenter> {
    void inject(RoomsListFragment fragment);
}
