package com.ne1c.developerstalk.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.ne1c.developerstalk.ui.activities.MainActivity;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.api.GitterApi;
import com.ne1c.developerstalk.utils.Utils;
import com.squareup.okhttp.OkHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.AbstractOnSubscribe;

public class NewMessagesService extends Service implements FayeClient.UnexpectedSituationCallback {
    public static final int NOTIF_REQUEST_CODE = 1000;
    public static final int NOTIF_CODE = 101;

    public static final String BROADCAST_SEND_MESSAGE = "sendMessageBroadcast";
    public static final String FROM_ROOM_EXTRA_KEY = "fromRoom";
    public static final String NEW_MESSAGE_EXTRA_KEY = "newMessage";
    public static final String TO_ROOM_MESSAGE_EXTRA_KEY = "toRoom";
    public static final String SEND_MESSAGE_EXTRA_KEY = "sendMessageToRoom";

    private FayeClient mFayeClient;
    private ArrayList<RoomModel> mRoomsList;

    private GitterApi mApiMethods;

    private boolean mSound;
    private boolean mVibro;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filterNetwork = new IntentFilter();
        filterNetwork.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filterNetwork);

        IntentFilter filterSendMessage = new IntentFilter();
        filterSendMessage.addAction(BROADCAST_SEND_MESSAGE);
        registerReceiver(sendMessageReceiver, filterSendMessage);
//
//        OkHttpClient okHttpClient = new OkHttpClient();
  //      okHttpClient.setReadTimeout(45, TimeUnit.SECONDS);
//        okHttpClient.setFollowSslRedirects(true);
//        okHttpClient.setRetryOnConnectionFailure(true);
//        okHttpClient.setProtocols(Arrays.asList(Protocol.HTTP_1_1));
//
        OkHttpClient client = new OkHttpClient();
        client.setReadTimeout(20, TimeUnit.DAYS);

        RestAdapter adapter = new RestAdapter.Builder()
                .setClient(new OkClient(client))
                .setEndpoint("https://stream.gitter.im")
                .build();
        mApiMethods = adapter.create(GitterApi.class);
        mFayeClient = new FayeClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mSound = prefs.getBoolean("notif_sound", true);
        mVibro = prefs.getBoolean("notif_vibro", true);

        // Reconnect if service start retry and update rooms
        if (flags == START_FLAG_RETRY) {
            disconnect();
        }

        return START_STICKY;
    }

    private void createSubscribers() {
        String channelEndpoint = "/api/v1/rooms/%s/chatMessages";

        for (final RoomModel room : mRoomsList) {
            mFayeClient.subscribeChannel(String.format(channelEndpoint, room.id))
                    .subscribe(new Action1<JsonObject>() {
                        @Override
                        public void call(JsonObject response) {
                            Intent intent = new Intent();
                            intent.setAction(MainActivity.BROADCAST_NEW_MESSAGE);
                            intent.putExtra(FROM_ROOM_EXTRA_KEY, room);

                            Gson gson = new GsonBuilder().create();
                            MessageModel message = new MessageModel();
                            if (response.getAsJsonObject("data") != null) {
                                message = gson.fromJson(
                                        response.getAsJsonObject("data").getAsJsonObject("model"),
                                        MessageModel.class);
                            }

                            intent.putExtra(NEW_MESSAGE_EXTRA_KEY, message);
                            if (message.text != null) {
                                sendBroadcast(intent);

                                boolean enableNotif = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                                        .getBoolean("enable_notif", true);
                                if (!message.fromUser.id.equals(Utils.getInstance().getUserPref().id) &&
                                        enableNotif) {
                                    sendNotificationMessage(room, message);
                                }
                            }
                        }
                    });
        }
    }

    @Override
    public void onDestroy() {
        mFayeClient.disconnect();
        unregisterReceiver(networkChangeReceiver);
        unregisterReceiver(sendMessageReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver sendMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    MessageModel message = new MessageModel();
                    Intent sendIntent;
                    try {
                        message = mApiMethods.sendMessage(Utils.getInstance().getBearer(),
                                intent.getStringExtra(TO_ROOM_MESSAGE_EXTRA_KEY),
                                intent.getStringExtra(SEND_MESSAGE_EXTRA_KEY));
                        sendIntent = new Intent(MainActivity.BROADCAST_MESSAGE_DELIVERED)
                                .putExtra(NewMessagesService.NEW_MESSAGE_EXTRA_KEY, message);
                    } catch (RetrofitError e) {
                        message.id = "";
                        sendIntent = new Intent(MainActivity.BROADCAST_MESSAGE_DELIVERED)
                                .putExtra(NewMessagesService.NEW_MESSAGE_EXTRA_KEY, message);
                    }

                    sendBroadcast(sendIntent);

                }
            }).start();
        }
    };

    private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utils.getInstance().isNetworkConnected()) {
                getMessageStream("55e448fa0fc9f982beaf298c").subscribe(new Action1<MessageModel>() {
                    @Override
                    public void call(MessageModel messageModel) {
                        Log.d("MESSAGE", messageModel.text);
                        //Toast.makeText(getApplicationContext(), messageModel.text, Toast.LENGTH_SHORT).show();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d("ERROR", throwable.getMessage());
                    }
                });
//                try {
//                    mFayeClient.connect(Utils.GITTER_FAYE_URL, Utils.getInstance().getAccessToken());
//                    mFayeClient.accessClientIdSubscriber().subscribe(new Action1<Boolean>() {
//                        @Override
//                        public void call(Boolean aBoolean) {
//                            try {
//                                mRoomsList = mApiMethods.getCurrentUserRooms(Utils.getInstance().getBearer());
//                                createSubscribers();
//                            } catch (RetrofitError e) {
//                                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
//                            }
//
//                        }
//                    });
//                } catch (OnErrorNotImplementedException | RetrofitError e) {
//                    if (e.getMessage().contains("401")) {
//                            sendBroadcast(new Intent(MainActivity.BROADCAST_UNAUTHORIZED));
//                        }
//                    disconnect();
//                }
            } else {
                if (mFayeClient != null) {
                    mFayeClient.disconnect();
                }
            }
        }
    };

    private void sendNotificationMessage(RoomModel room, MessageModel message) {
        Intent notifIntent = new Intent(getApplicationContext(), MainActivity.class);
        notifIntent.putExtra(FROM_ROOM_EXTRA_KEY, room);
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                NOTIF_REQUEST_CODE,
                notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        String username = message.fromUser.username;
        Spannable text = new SpannableString(username + ": " + message.text);
        text.setSpan(new StyleSpan(Typeface.BOLD), 0, username.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new StyleSpan(Typeface.ITALIC), username.length() + 1, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentIntent(pendingIntent)
                        .setSmallIcon(R.mipmap.ic_notif_message)
                        .setTicker(text)
                        .setContentText(text)
                        .setContentTitle(room.name);

        NotificationManagerCompat notifMgr = NotificationManagerCompat.from(getApplicationContext());
        Notification notification = builder.build();

        notification.defaults = Notification.DEFAULT_LIGHTS;

        if (mVibro) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }

        if (mSound) {
            notification.defaults |= Notification.DEFAULT_SOUND;
        }

        notification.flags |= NotificationCompat.FLAG_AUTO_CANCEL;

        notifMgr.notify(NOTIF_CODE, notification);
    }

    @Override
    public void disconnect() {
//        mFayeClient.reconnect(Utils.getInstance().getAccessToken(), new Action1<Boolean>() {
//            @Override
//            public void call(Boolean aBoolean) {
//                mRoomsList = mApiMethods.getCurrentUserRooms(Utils.getInstance().getBearer());
//                createSubscribers();
//            }
//        });
    }

    private Observable<MessageModel> getMessageStream(String roomId) {
        return mApiMethods.getRoomStream(Utils.getInstance().getBearer(), roomId)
                .flatMap(new Func1<Response, Observable<String>>() {
                    @Override
                    public Observable<String> call(Response response) {
                        try {
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getBody().in()));
                            return Observable.create(new OnSubscribeBufferedReader(bufferedReader));
                        } catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                }).filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return s != null && !s.trim().isEmpty();
                    }
                }).map(new Func1<String, MessageModel>() {
                    @Override
                    public MessageModel call(String s) {
                        return new Gson().fromJson(s, MessageModel.class);
                    }
                });
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
//        Ignore this exception
                }
            }
        }
    }
}

