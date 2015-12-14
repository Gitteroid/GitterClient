package com.ne1c.developerstalk.api;

import com.google.gson.Gson;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.utils.Utils;
import com.squareup.okhttp.OkHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import retrofit.RestAdapter;
import retrofit.client.OkClient;
import rx.Observable;
import rx.Subscriber;
import rx.observables.AbstractOnSubscribe;

public class GitterStreaming {
    private GitterApi mApiMethods;

    public GitterStreaming() {
        OkHttpClient client = new OkHttpClient();
        client.setReadTimeout(7, TimeUnit.DAYS);
        client.setConnectTimeout(7, TimeUnit.DAYS);

        RestAdapter adapter = new RestAdapter.Builder()
                .setClient(new OkClient(client))
                .setEndpoint("https://stream.gitter.im")
                .build();
        mApiMethods = adapter.create(GitterApi.class);
    }

    public Observable<MessageModel> getMessageStream(String roomId) {
        return mApiMethods.getRoomStream(Utils.getInstance().getBearer(), roomId)
                .flatMap(response -> {
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getBody().in()));
                        return Observable.create(new OnSubscribeBufferedReader(bufferedReader));
                    } catch (IOException e) {
                        return Observable.error(e);
                    }
                }).filter(s -> s != null && !s.trim().isEmpty())
                .map(s -> new Gson().fromJson(s, MessageModel.class));
    }

    public class OnSubscribeBufferedReader extends AbstractOnSubscribe<String, BufferedReader> {
        private final BufferedReader reader;

        public OnSubscribeBufferedReader(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        protected BufferedReader onSubscribe(Subscriber<? super String> subscriber) {
            return reader;
        }

        @Override
        protected void next(SubscriptionState<String, BufferedReader> state) {
            BufferedReader reader = state.state();
            try {
                String line = reader.readLine();
                if (line == null) {
                    state.onCompleted();
                } else {
                    state.onNext(line);
                }
            } catch (IOException e) {
                state.onError(e);
            }
        }

        @Override
        protected void onTerminated(BufferedReader state) {
            super.onTerminated(state);

            if (state != null) {
                try {
                    state.close();
                } catch (IOException e) {
                    // Ignore this exception
                }
            }
        }
    }
}
