package com.ne1c.gitteroid.di

import android.content.Context
import android.content.SharedPreferences
import com.ne1c.gitteroid.api.GitterApi
import com.ne1c.gitteroid.api.GitterStreamApi
import com.ne1c.gitteroid.api.GitterStreamer
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.base.ExecutorService
import com.ne1c.gitteroid.di.base.NetworkService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

enum class DependencyManager {
    INSTANCE;

    lateinit var dataManager: DataManger
        private set
    lateinit var userPrefs: SharedPreferences
        private set
    lateinit var networkService: NetworkService
        private set
    lateinit var executorService: ExecutorService
        private set
    lateinit var gitterStreamer: GitterStreamer
        private set

    fun init(context: Context) {
        userPrefs = context.getSharedPreferences(DataManger.USERINFO_PREF, Context.MODE_PRIVATE)
        networkService = AndroidNetworkService(context)
        executorService = RxExecutorService()
        dataManager = DataManger(createGitterApi(), userPrefs)
        gitterStreamer = GitterStreamer(createGitterStreamApi(), dataManager.bearer)
    }

    private fun createGitterApi(): GitterApi {
        val logger = HttpLoggingInterceptor()
        logger.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
                .addInterceptor(logger).build()

        return Retrofit.Builder()
                .baseUrl(DataManger.GITTER_API_URL)
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GitterApi::class.java)
    }

    private fun createGitterStreamApi(): GitterStreamApi {
        val client = OkHttpClient.Builder()
                .readTimeout(7, TimeUnit.DAYS)
                .connectTimeout(7, TimeUnit.DAYS)
                .build()

        return Retrofit.Builder()
                .baseUrl(DataManger.GITTER_API_URL)
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GitterStreamApi::class.java)
    }
}