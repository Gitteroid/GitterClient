package com.ne1c.gitteroid.di.modules;

import android.content.Context;

import com.ne1c.gitteroid.Application;
import com.ne1c.gitteroid.api.GitterApi;
import com.ne1c.gitteroid.dataproviders.ClientDatabase;
import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.di.annotations.PerApplication;

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
