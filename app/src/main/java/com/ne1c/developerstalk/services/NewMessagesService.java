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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;

import com.google.gson.Gson;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.api.GitterApi;
import com.ne1c.developerstalk.database.ClientDatabase;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.ui.activities.MainActivity;
import com.ne1c.developerstalk.utils.Utils;
import com.squareup.okhttp.OkHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.observables.AbstractOnSubscribe;
import rx.subscriptions.CompositeSubscription;

public class NewMessagesService extends Service {
    public static final int NOTIF_REQUEST_CODE = 1000;
    public static final int NOTIF_CODE = 101;

    public static final String BROADCAST_SEND_MESSAGE = "sendMessageBroadcast";
    public static final String FROM_ROOM_EXTRA_KEY = "fromRoom";
    public static final String NEW_MESSAGE_EXTRA_KEY = "newMessage";
    public static final String TO_ROOM_MESSAGE_EXTRA_KEY = "toRoom";
    public static final String SEND_MESSAGE_EXTRA_KEY = "sendMessageToRoom";

    private List<RoomModel> mRoomsList;
    private CompositeSubscription mMessagesSubscrptions;

    private GitterApi mApiMethods;

    private boolean mEnableNotif;
    private boolean mSound;
    private boolean mVibro;
    // Get messages only with user name
    private boolean mWithUserName;

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

        OkHttpClient client = new OkHttpClient();
        client.setReadTimeout(7, TimeUnit.DAYS);
        client.setConnectTimeout(7, TimeUnit.DAYS);

        RestAdapter adapter = new RestAdapter.Builder()
                .setClient(new OkClient(client))
                .setEndpoint("https://stream.gitter.im")
                .build();
        mApiMethods = adapter.create(GitterApi.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mEnableNotif = prefs.getBoolean("enable_notif", true);
        mSound = prefs.getBoolean("notif_sound", true);
        mVibro = prefs.getBoolean("notif_vibro", true);
        mWithUserName = prefs.getBoolean("notif_username", false);

        mRoomsList = new ClientDatabase(getApplicationContext()).getRooms();
        createSubscribers();

        return START_STICKY;
    }

    private void createSubscribers() {
        mMessagesSubscrptions = new CompositeSubscription();

        for (final RoomModel room : mRoomsList) {
            Subscription sub = getMessageStream(room.id).subscribe(message -> {
                Intent intent = new Intent();
                intent.setAction(MainActivity.BROADCAST_NEW_MESSAGE);
                intent.putExtra(FROM_ROOM_EXTRA_KEY, room);

                intent.putExtra(NEW_MESSAGE_EXTRA_KEY, message);
                if (message.text != null) {
                    sendBroadcast(intent);

                    if (mEnableNotif && !message.fromUser.id.equals(Utils.getInstance().getUserPref().id)) {
                        final String username = Utils.getInstance().getUserPref().username;

                        if (mWithUserName && message.text.contains(username)) {
                            sendNotificationMessage(room, message);
                        } else if (!mWithUserName) {
                            sendNotificationMessage(room, message);
                        }
                    }
                }
            });

            mMessagesSubscrptions.add(sub);
        }
    }

    @Override
    public void onDestroy() {
        mMessagesSubscrptions.unsubscribe();
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
                if (mMessagesSubscrptions.isUnsubscribed()) {
                    createSubscribers();
                }
            } else {
                mMessagesSubscrptions.unsubscribe();
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

    private Observable<MessageModel> getMessageStream(String roomId) {
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