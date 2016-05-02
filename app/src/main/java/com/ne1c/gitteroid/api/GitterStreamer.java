package com.ne1c.gitteroid.api;

import com.google.gson.Gson;
import com.ne1c.gitteroid.models.data.MessageModel;
import com.ne1c.gitteroid.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.observables.SyncOnSubscribe;

public class GitterStreamer {
    private final String STREAM_URL = "https://stream.gitter.im/v1/rooms/%s/chatMessages";

    private GitterStreamApi mStreamApi;

    @Inject
    public GitterStreamer(GitterStreamApi api) {
        mStreamApi = api;
    }

    public Observable<MessageModel> getMessageStream(String roomId) {
        String streamUrl = String.format(STREAM_URL, roomId);
        return mStreamApi.getMessagesStream(streamUrl, Utils.getInstance().getBearer())
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
