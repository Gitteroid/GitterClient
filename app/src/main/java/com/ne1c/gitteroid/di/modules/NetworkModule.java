package com.ne1c.gitteroid.di.modules;

import com.ne1c.gitteroid.api.GitterApi;
import com.ne1c.gitteroid.api.GitterStreamApi;
import com.ne1c.gitteroid.api.GitterStreamer;
import com.ne1c.gitteroid.di.annotations.PerApplication;
import com.ne1c.gitteroid.utils.Utils;

import java.util.concurrent.TimeUnit;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public class NetworkModule {
    @PerApplication
    @Provides
    public GitterApi provideApiClient() {
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
        logger.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(7, TimeUnit.DAYS)
                .connectTimeout(7, TimeUnit.DAYS)
                .addInterceptor(logger)
                .build();

        Retrofit adapter = new Retrofit.Builder()
                .baseUrl(Utils.GITTER_API_URL)
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return adapter.create(GitterApi.class);
    }

    @PerApplication
    @Provides
    public GitterStreamApi provideStreamApi() {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(7, TimeUnit.DAYS)
                .connectTimeout(7, TimeUnit.DAYS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(Utils.GITTER_API_URL)
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GitterStreamApi.class);
    }

    @PerApplication
    @Provides
    public GitterStreamer provideGitterStreamer(GitterStreamApi api) {
        return new GitterStreamer(api);
    }
}
