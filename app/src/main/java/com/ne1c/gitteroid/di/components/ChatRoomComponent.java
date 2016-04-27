package com.ne1c.gitteroid.di.components;

import com.ne1c.gitteroid.di.HasPresenter;
import com.ne1c.gitteroid.di.annotations.PerFragment;
import com.ne1c.gitteroid.di.modules.ChatRoomPresenterModule;
import com.ne1c.gitteroid.presenters.ChatRoomPresenter;
import com.ne1c.gitteroid.ui.fragments.ChatRoomFragment;

import dagger.Component;

@PerFragment
@Component(modules = ChatRoomPresenterModule.class, dependencies = ApplicationComponent.class)
public interface ChatRoomComponent extends HasPresenter<ChatRoomPresenter> {
    void inject(ChatRoomFragment fragment);
}
