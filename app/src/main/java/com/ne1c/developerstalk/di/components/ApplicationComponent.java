package com.ne1c.developerstalk.di.components;

import com.ne1c.developerstalk.Application;
import com.ne1c.developerstalk.di.annotations.PerApplication;
import com.ne1c.developerstalk.di.modules.ApplicationModule;

import dagger.Component;

@PerApplication
@Component(modules = ApplicationModule.class)
public interface ApplicationComponent {
    void inject(Application app);
}
