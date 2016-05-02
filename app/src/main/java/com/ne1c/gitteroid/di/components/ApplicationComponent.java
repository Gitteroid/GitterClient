package com.ne1c.gitteroid.di.components;

import android.content.Context;

import com.ne1c.gitteroid.Application;
import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.di.annotations.PerApplication;
import com.ne1c.gitteroid.di.modules.ApplicationModule;
import com.ne1c.gitteroid.services.NotificationService;
import com.ne1c.gitteroid.utils.RxSchedulersFactory;

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
