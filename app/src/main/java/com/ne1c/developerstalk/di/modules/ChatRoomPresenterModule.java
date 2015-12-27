package com.ne1c.developerstalk.di.modules;

import com.ne1c.developerstalk.di.annotations.PerFragment;
import com.ne1c.developerstalk.presenters.ChatRoomPresenter;

import dagger.Module;
import dagger.Provides;

@Module
public class ChatRoomPresenterModule {
    @PerFragment
    @Provides
    public ChatRoomPresenter provideChatRoomPresenter() {
        return new ChatRoomPresenter();
    }
}
