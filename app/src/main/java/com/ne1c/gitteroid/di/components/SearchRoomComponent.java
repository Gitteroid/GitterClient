package com.ne1c.gitteroid.di.components;

import com.ne1c.gitteroid.di.HasPresenter;
import com.ne1c.gitteroid.di.annotations.PerFragment;
import com.ne1c.gitteroid.di.modules.SearchRoomPresenterModule;
import com.ne1c.gitteroid.presenters.SearchRoomPresenter;
import com.ne1c.gitteroid.ui.activities.SearchRoomActivity;

import dagger.Component;

@PerFragment
@Component(modules = SearchRoomPresenterModule.class, dependencies = ApplicationComponent.class)
public interface SearchRoomComponent extends HasPresenter<SearchRoomPresenter> {
    void inject(SearchRoomActivity activity);
}
