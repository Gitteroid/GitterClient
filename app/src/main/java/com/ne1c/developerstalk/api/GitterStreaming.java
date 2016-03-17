package com.ne1c.developerstalk.api;

import com.google.gson.Gson;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.observables.SyncOnSubscribe;

public class GitterStreaming {
    private GitterApi mApiMethods;

    public GitterStreaming() {
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

        mApiMethods = adapter.create(GitterApi.class);
    }

    public Observable<MessageModel> getMessageStream(String roomId) {
        return mApiMethods.getRoomStream(Utils.getInstance().getBearer(), roomId)
                .flatMap(response -> {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.byteStream()));
                    return Observable.create(new OnSubscribeBufferedReader(bufferedReader));
                }).filter(s -> s != null && !s.trim().isEmpty())
                .map(s -> new Gson().fromJson(s, MessageModel.class));
    }

    public class OnSubscribeBufferedReader extends SyncOnSubscribe<BufferedReader, String> {
        private final BufferedReader reader;

        public OnSubscribeBufferedReader(BufferedReader reader) {
            this.reader = reader;
        }


        @Override
        protected BufferedReader generateState() {
            return reader;
        }

        @Override
        protected BufferedReader next(BufferedReader state, Observer<? super String> observer) {
            BufferedReader reader = state;
            String line;
            try {
                line = reader.readLine();
                if (line == null) {
                    observer.onCompleted();
                } else {
                    observer.onNext(line);
                }
            } catch (IOException e) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                observer.onError(e);
            }

            return reader;
        }
    }
}
