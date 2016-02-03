package com.ne1c.developerstalk.di.modules;

import com.ne1c.developerstalk.di.annotations.PerFragment;
import com.ne1c.developerstalk.presenters.ChatRoomPresenter;
import com.ne1c.developerstalk.dataprovides.DataManger;
import com.ne1c.developerstalk.utils.RxSchedulersFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class ChatRoomPresenterModule {
    @PerFragment
    @Provides
    public ChatRoomPresenter provideChatRoomPresenter(RxSchedulersFactory factory, DataManger dataManger) {
        return new ChatRoomPresenter(factory, dataManger);
    }
}
