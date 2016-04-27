package com.ne1c.gitteroid;

import com.crashlytics.android.Crashlytics;
import com.ne1c.gitteroid.api.GitterStreamer;
import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.di.HasComponent;
import com.ne1c.gitteroid.di.components.ApplicationComponent;
import com.ne1c.gitteroid.di.components.DaggerApplicationComponent;
import com.ne1c.gitteroid.di.modules.ApplicationModule;
import com.ne1c.gitteroid.utils.Utils;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;

public class Application extends android.app.Application implements HasComponent<ApplicationComponent>{
    private ApplicationComponent mApplicationComponent;

    @Inject
    protected DataManger mDataManager;

    @Inject
    protected GitterStreamer mStreamer;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        Utils.init(getApplicationContext());

        mApplicationComponent = createComponent();
        mApplicationComponent.inject(this);
    }

    private ApplicationComponent createComponent() {
        return DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
    }

    public ApplicationComponent getComponent() {
        return mApplicationComponent;
    }

    public DataManger getDataManager() {
        return mDataManager;
    }

    public GitterStreamer getStreamer() {
        return mStreamer;
    }
}
