package com.ne1c.developerstalk;

import com.ne1c.developerstalk.utils.Utils;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Utils.init(getApplicationContext());
    }
}
