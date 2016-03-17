package com.ne1c.developerstalk.di.modules;

import android.content.Context;

import com.ne1c.developerstalk.Application;
import com.ne1c.developerstalk.api.GitterApi;
import com.ne1c.developerstalk.dataprovides.ClientDatabase;
import com.ne1c.developerstalk.di.annotations.PerApplication;
import com.ne1c.developerstalk.dataprovides.DataManger;
import com.ne1c.developerstalk.utils.Utils;

import dagger.Module;
import dagger.Provides;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Module(includes = {DatabaseModule.class, RxSchedulersModule.class})
public class ApplicationModule {
    private Application mApp;

    public ApplicationModule(Application mApp) {
        this.mApp = mApp;
    }

    @PerApplication
    @Provides
    public DataManger provideDataManager(ClientDatabase database) {
        return new DataManger(database);
    }

    @PerApplication
    @Provides
    public GitterApi provideApi() {
        return new Retrofit.Builder()
                .baseUrl(Utils.GITTER_API_URL)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GitterApi.class);
    }

    @PerApplication
    @Provides
    public Context provideContext() {
        return mApp;
    }
}
