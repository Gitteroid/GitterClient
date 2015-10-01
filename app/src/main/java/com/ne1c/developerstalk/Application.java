package com.ne1c.developerstalk;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Utils.init(getApplicationContext());
    }
}
