package com.ne1c.developerstalk.di.modules;

import android.content.Context;

import com.ne1c.developerstalk.Application;
import com.ne1c.developerstalk.api.GitterApi;
import com.ne1c.developerstalk.dataproviders.ClientDatabase;
import com.ne1c.developerstalk.di.annotations.PerApplication;
import com.ne1c.developerstalk.dataproviders.DataManger;

import dagger.Module;
import dagger.Provides;

@Module(includes = {DatabaseModule.class, NetworkModule.class, RxSchedulersModule.class})
public class ApplicationModule {
    private Application mApp;

    public ApplicationModule(Application mApp) {
        this.mApp = mApp;
    }

    @PerApplication
    @Provides
    public DataManger provideDataManager(GitterApi api, ClientDatabase database) {
        return new DataManger(api, database);
    }

    @PerApplication
    @Provides
    public Context provideContext() {
        return mApp;
    }
}
