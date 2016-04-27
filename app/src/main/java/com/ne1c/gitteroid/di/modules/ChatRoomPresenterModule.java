package com.ne1c.gitteroid.di.modules;

import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.di.annotations.PerFragment;
import com.ne1c.gitteroid.presenters.ChatRoomPresenter;
import com.ne1c.gitteroid.utils.RxSchedulersFactory;

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
