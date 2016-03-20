package com.ne1c.developerstalk.di.components;

import android.content.Context;

import com.ne1c.developerstalk.Application;
import com.ne1c.developerstalk.api.GitterStreamApi;
import com.ne1c.developerstalk.api.GitterStreamer;
import com.ne1c.developerstalk.di.annotations.PerApplication;
import com.ne1c.developerstalk.di.modules.ApplicationModule;
import com.ne1c.developerstalk.dataprovides.DataManger;
import com.ne1c.developerstalk.di.modules.NetworkModule;
import com.ne1c.developerstalk.services.NotificationService;
import com.ne1c.developerstalk.utils.RxSchedulersFactory;

import dagger.Component;

@PerApplication
@Component(modules = {ApplicationModule.class})
public interface ApplicationComponent {
    void inject(Application app);

    void inject(NotificationService service);

    Context getContext();

    DataManger getDataManager();

    RxSchedulersFactory getRxSchedulersFactory();
}
