package com.ne1c.gitteroid.di

import android.content.Context
import android.content.SharedPreferences
import com.ne1c.gitteroid.api.GitterApi
import com.ne1c.gitteroid.api.GitterStreamApi
import com.ne1c.gitteroid.api.GitterStreamer
import com.ne1c.gitteroid.dataproviders.ClientDatabase
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

    var dataManager: DataManger? = null
    var clientDatabase: ClientDatabase? = null
    var userPrefs: SharedPreferences? = null
    var networkService: NetworkService? = null
    var executorService: ExecutorService? = null
    var gitterStreamer: GitterStreamer? = null

    fun init(context: Context) {
        if (needInit()) {
            userPrefs = context.getSharedPreferences(DataManger.USERINFO_PREF, Context.MODE_PRIVATE)
            clientDatabase = ClientDatabase(context)
            networkService = AndroidNetworkService(context)
            executorService = RxExecutorService()
            dataManager = DataManger(createGitterApi(), clientDatabase!!, userPrefs!!)
            gitterStreamer = GitterStreamer(createGitterStreamApi())
        }
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

    private fun needInit(): Boolean {
        return dataManager == null || clientDatabase == null ||
                userPrefs == null || networkService == null ||
                executorService == null
    }
}