package com.ne1c.gitterclient;

import android.content.Intent;
import android.text.TextUtils;

import com.ne1c.gitterclient.Activities.MainActivity;
import com.ne1c.gitterclient.Services.NewMessagesService;
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

        if (!TextUtils.isEmpty(Utils.getInstance().getAccessToken())) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));

            startService(new Intent(getApplicationContext(), NewMessagesService.class));
        }
    }
}
