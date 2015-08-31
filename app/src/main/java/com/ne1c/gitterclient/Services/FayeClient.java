package com.ne1c.gitterclient.Services;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ws.WebSocket;
import com.squareup.okhttp.ws.WebSocketCall;
import com.squareup.okhttp.ws.WebSocketListener;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import okio.Buffer;
import okio.BufferedSource;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

public class FayeClient {

    private static final String HANDSHAKE_CHANNEL = "/meta/handshake";
    private static final String CONNECT_CHANNEL = "/meta/connect";
    private static final String DISCONNECT_CHANNEL = "/meta/disconnect";
    private static final String SUBSCRIBE_CHANNEL = "/meta/subscribe";
    private static final String UNSUBSCRIBE_CHANNEL = "/meta/unsubscribe";


    private static final String KEY_CHANNEL = "channel";
    private static final String KEY_SUCCESS = "successful";
    private static final String KEY_CLIENT_ID = "clientId";
    private static final String KEY_VERSION = "version";
    private static final String KEY_MIN_VERSION = "minimumVersion";
    private static final String KEY_SUBSCRIPTION = "subscription";
    private static final String KEY_SUP_CONN_TYPES = "supportedConnectionTypes";
    private static final String KEY_CONN_TYPE = "connectionType";
    private static final String KEY_DATA = "data";
    private static final String KEY_ID = "id";
    private static final String KEY_EXT = "ext";
    private static final String KEY_ERROR = "error";

    private static final String VALUE_VERSION = "1.0";
    private static final String VALUE_MIN_VERSION = "1.0beta";
    private static final String VALUE_CONN_TYPE = "websocket";

    private String mAcessToken;
    private String mClientId;
    private String mUserId;

    private WebSocket mWebSocket;
    private BehaviorSubject<Void> mConnectObservable;
    private Context mContext;

    private Map<String, Subscriber<? super JsonObject>> mSubscriberMap = new HashMap<>();

    private Subscriber<Boolean> mAccessClientIdSub; // Access to client id

    public FayeClient(Context context) {
        mContext = context;
    }

    private Thread pintThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                while (mWebSocket != null) {
                    mWebSocket.sendPing(new Buffer());
                    Thread.sleep(3000);
                }
            } catch (IOException | InterruptedException e) {
                mWebSocket = null;
                Log.d("MYTAG", "WEB SOCKET NULL");
            }
        }
    });

    private WebSocketListener mWsListner = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            mWebSocket = webSocket;
            pintThread.start();
            initMetaChannels();

            try {
                doHandshake();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IOException e, Response response) {
        }

        @Override
        public void onMessage(BufferedSource payload, WebSocket.PayloadType type) throws IOException {
            Buffer buffer = new Buffer();
            payload.readAll(buffer);
            payload.close();
            FayeClient.this.onMessage(buffer.readString(Charset.defaultCharset()));
        }

        @Override
        public void onPong(Buffer payload) {

        }

        @Override
        public void onClose(int code, String reason) {
            mWebSocket = null;
        }
    };

    private void onMessage(String message) {
        for (JsonElement element : new Gson().fromJson(message, JsonArray.class)) {
            if (element instanceof JsonObject) {
                JsonObject object = (JsonObject) element;
                String channel = object.get("channel").getAsString();
                if (mSubscriberMap.containsKey(channel)) {
                    mSubscriberMap.get(channel).onNext(object);
                }
            }
        }

    }

    public Observable<Void> connect(String url, String token) {
        mAcessToken = token;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        WebSocketCall.create(client, request).enqueue(mWsListner);
        mConnectObservable = BehaviorSubject.create();

        return mConnectObservable;
    }

    public Observable<Void> disconnect() {
        pintThread.interrupt();
        return null;
    }

    private void initMetaChannels() {
        subscribeChannel(HANDSHAKE_CHANNEL, true).subscribe(new Action1<JsonObject>() {
            @Override
            public void call(JsonObject message) {
                mClientId = message.get(KEY_CLIENT_ID).getAsString();
                mUserId = message.getAsJsonObject("ext").get("userId").getAsString();

                mAccessClientIdSub.onNext(true);
                mAccessClientIdSub.onCompleted();
                try {
                    doConnect();
                } catch (IOException e) {
                    mConnectObservable.onError(e);
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                mConnectObservable.onError(throwable);
            }
        });

        subscribeChannel(CONNECT_CHANNEL, true).subscribe(new Action1<JsonObject>() {
            @Override
            public void call(JsonObject o) {
                mConnectObservable.onCompleted();
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                mConnectObservable.onError(throwable);
            }
        });

        subscribeChannel(DISCONNECT_CHANNEL, true).subscribe(new Action1<JsonObject>() {
            @Override
            public void call(JsonObject o) {

            }
        });

        subscribeChannel(SUBSCRIBE_CHANNEL, true).subscribe(new Action1<JsonObject>() {
            @Override
            public void call(JsonObject o) {

            }
        });

        subscribeChannel(UNSUBSCRIBE_CHANNEL, true).subscribe(new Action1<JsonObject>() {
            @Override
            public void call(JsonObject o) {

            }
        });
    }

    public Observable<JsonObject> subscribeChannel(String channel) {
        return subscribeChannel(channel, false);
    }

    private Observable<JsonObject> subscribeChannel(final String channel, Boolean meta) {
        if (!meta) {
            JsonObject json = new JsonObject();
            json.addProperty(KEY_CHANNEL, SUBSCRIBE_CHANNEL);
            json.addProperty(KEY_CLIENT_ID, mClientId);
            json.addProperty(KEY_SUBSCRIPTION, channel);

            try {
                sendMessage(json);
            } catch (IOException e) {
                return Observable.error(e);
            }
        }

        return Observable.create(new Observable.OnSubscribe<JsonObject>() {
            @Override
            public void call(Subscriber<? super JsonObject> subscriber) {
                mSubscriberMap.put(channel, subscriber);
            }
        });
    }

    public Observable<Boolean> accessClientIdSubscriber() {
            return Observable.create(new Observable.OnSubscribe<Boolean>() {
                @Override
                public void call(Subscriber<? super Boolean> subscriber) {
                    mAccessClientIdSub = (Subscriber<Boolean>) subscriber;
                }
            });
    }

    private void sendMessage(JsonObject message) throws IOException {
        JsonObject auth = new JsonObject();
        auth.addProperty("token", mAcessToken);
        message.add(KEY_EXT, auth);
        sendMessage(message.toString());
    }

    private void sendMessage(String message) throws IOException {
        mWebSocket.sendMessage(WebSocket.PayloadType.TEXT, new Buffer().writeString(message, Charset.defaultCharset()));
    }

    private void doHandshake() throws IOException {
        JsonObject json = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(new JsonPrimitive("websocket"));
        json.add(KEY_SUP_CONN_TYPES, jsonArray);
        json.addProperty(KEY_CHANNEL, HANDSHAKE_CHANNEL);
        json.addProperty(KEY_VERSION, VALUE_VERSION);
        json.addProperty(KEY_MIN_VERSION, VALUE_MIN_VERSION);

        sendMessage(json);
    }

    private void doConnect() throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty(KEY_CHANNEL, CONNECT_CHANNEL);
        json.addProperty(KEY_CLIENT_ID, mClientId);
        json.addProperty(KEY_CONN_TYPE, VALUE_CONN_TYPE);

        sendMessage(json);
    }
}
