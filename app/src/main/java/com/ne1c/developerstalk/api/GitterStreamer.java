package com.ne1c.developerstalk.api;

import com.google.gson.Gson;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Observer;
import rx.observables.SyncOnSubscribe;

public class GitterStreamer {
    private GitterStreamApi mApiMethods;

    public GitterStreamer() {
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
        logger.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(7, TimeUnit.DAYS)
                .connectTimeout(7, TimeUnit.DAYS)
                .build();

        Retrofit adapter = new Retrofit.Builder()
                .baseUrl(Utils.GITTER_API_URL)
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mApiMethods = adapter.create(GitterStreamApi.class);
    }

    public Observable<MessageModel> getMessageStream(String roomId) {
        String streamUrl = String.format("https://stream.gitter.im/v1/rooms/%s/chatMessages", roomId);
        return mApiMethods.getMessagesStream(streamUrl, Utils.getInstance().getBearer())
                .flatMap(response -> Observable.create(new OnSubscribeBufferedReader(new BufferedReader(response.charStream())))).filter(s -> s != null && !s.trim().isEmpty())
                .map(s -> new Gson().fromJson(s, MessageModel.class));
    }

    public class OnSubscribeBufferedReader extends SyncOnSubscribe<BufferedReader, String> {
        private final BufferedReader mReader;

        public OnSubscribeBufferedReader(BufferedReader reader) {
            mReader = reader;
        }


        @Override
        protected BufferedReader generateState() {
            return mReader;
        }

        @Override
        protected BufferedReader next(BufferedReader state, Observer<? super String> observer) {
            String line;
            try {
                line = state.readLine();
                if (line == null) {
                    observer.onCompleted();
                } else {
                    observer.onNext(line);
                }
            } catch (IOException e) {
                try {
                    state.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                observer.onError(e);
            }

            return state;
        }
    }
}
