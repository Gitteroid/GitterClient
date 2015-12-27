package com.ne1c.developerstalk.di.components;

import com.ne1c.developerstalk.di.annotations.PerFragment;
import com.ne1c.developerstalk.di.modules.ChatRoomPresenterModule;
import com.ne1c.developerstalk.presenters.ChatRoomPresenter;
import com.ne1c.developerstalk.di.HasPresenter;
import com.ne1c.developerstalk.ui.fragments.ChatRoomFragment;

import dagger.Component;

@PerFragment
@Component(modules = ChatRoomPresenterModule.class, dependencies = ApplicationComponent.class)
public interface ChatRoomComponent extends HasPresenter<ChatRoomPresenter> {
    void inject(ChatRoomFragment fragment);
}
