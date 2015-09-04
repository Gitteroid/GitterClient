package com.ne1c.gitterclient;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Utils.init(getApplicationContext());

        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .defaultDisplayImageOptions(options)
                .memoryCacheSize(TRIM_MEMORY_BACKGROUND)
                .diskCacheFileCount(150)
                .build();

        ImageLoader.getInstance().init(config);
    }
}
