package com.ne1c.developerstalk.di.modules;

import android.content.Context;

import com.ne1c.developerstalk.dataproviders.ClientDatabase;
import com.ne1c.developerstalk.di.annotations.PerApplication;

import dagger.Module;
import dagger.Provides;

@Module
public class DatabaseModule {
    @PerApplication
    @Provides
    public ClientDatabase provideClientDatabase(Context context) {
        return new ClientDatabase(context);
    }
}
