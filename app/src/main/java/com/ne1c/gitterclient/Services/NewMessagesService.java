package com.ne1c.gitterclient.Services;

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
import com.ne1c.gitterclient.Activities.MainActivity;
import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.R;
import com.ne1c.gitterclient.RetrofitServices.IApiMethods;
import com.ne1c.gitterclient.Utils;

import java.util.ArrayList;

import retrofit.RestAdapter;
import retrofit.client.Response;
import rx.functions.Action1;


public class NewMessagesService extends Service implements FayeClient.UnexpectedSituationCallback {

    public static final int NOTIF_REQUEST_CODE = 1000;
    public static final int NOTIF_CODE = 101;

    public static final String BROADCAST_SEND_MESSAGE = "sendMessageBroadcast";
    public static final String FROM_ROOM_EXTRA_KEY = "fromRoom";
    public static final String NEW_MESSAGE_EXTRA_KEY = "newMessage";
    public static final String CHANGED_ROOMS_EXTRA_KEY = "newRoomsList";
    public static final String TO_ROOM_MESSAGE_EXTRA_KEY = "toRoom";
    public static final String SEND_MESSAGE_EXTRA_KEY = "sendMessageToRoom";

    private FayeClient mFayeClient;
    private ArrayList<RoomModel> mRoomsList;

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Update rooms list, and update subscribers
        if (flags == START_FLAG_RETRY) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mFayeClient.connect(Utils.getInstance().GITTER_FAYE_URL, Utils.getInstance().getAccessToken());
                    mFayeClient.accessClientIdSubscriber().subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            RestAdapter adapter = new RestAdapter.Builder()
                                    .setEndpoint(Utils.getInstance().GITTER_API_URL)
                                    .build();
                            final IApiMethods methods = adapter.create(IApiMethods.class);

                            mRoomsList = methods.getCurrentUserRooms(Utils.getInstance().getBearer());
                            createSubribers();
                        }
                    });
                }
            });

            thread.start();
        }

        return START_STICKY;
    }

    private void createSubribers() {
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
                            MessageModel message = null;
                            if (response.getAsJsonObject("data") != null) {
                                message = gson.fromJson(response.getAsJsonObject("data").getAsJsonObject("model"), MessageModel.class);
                            }

                            intent.putExtra(NEW_MESSAGE_EXTRA_KEY, message);
                            if (message.text != null) {
                                sendBroadcast(intent);

                                if (!message.fromUser.username.equals(Utils.getInstance().getUserPref().username) &&
                                        PreferenceManager.getDefaultSharedPreferences(NewMessagesService.this).getBoolean("enable_notif", true)) {
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
            // 403 error
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        mFayeClient.sendMessageInRoom(intent.getStringExtra(SEND_MESSAGE_EXTRA_KEY),
//                                intent.getStringExtra(TO_ROOM_MESSAGE_EXTRA_KEY));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }).start();

            RestAdapter adapter = new RestAdapter.Builder()
                    .setEndpoint(Utils.getInstance().GITTER_API_URL)
                    .build();
            final IApiMethods methods = adapter.create(IApiMethods.class);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    methods.sendMessage(Utils.getInstance().getBearer(),
                            intent.getStringExtra(TO_ROOM_MESSAGE_EXTRA_KEY),
                            intent.getStringExtra(SEND_MESSAGE_EXTRA_KEY));

                }
            }).start();
        }
    };

    private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mFayeClient == null) {
                mFayeClient = new FayeClient(NewMessagesService.this);
            }

            if (Utils.getInstance().isNetworkConnected()) {
                mFayeClient.connect(Utils.getInstance().GITTER_FAYE_URL, Utils.getInstance().getAccessToken());
                mFayeClient.accessClientIdSubscriber().subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        RestAdapter adapter = new RestAdapter.Builder()
                                .setEndpoint(Utils.getInstance().GITTER_API_URL)
                                .build();
                        final IApiMethods methods = adapter.create(IApiMethods.class);

                        mRoomsList = methods.getCurrentUserRooms(Utils.getInstance().getBearer());
                        createSubribers();
                    }
                });
            } else {
                mFayeClient.disconnect();
            }
        }
    };

    private void sendNotificationMessage(RoomModel room, MessageModel message) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean sound = prefs.getBoolean("notif_sound", true);
        boolean vibro = prefs.getBoolean("notif_vibro", true);


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
                        .setSmallIcon(R.mipmap.ic_message_white_24dp)
                        .setTicker(text)
                        .setContentText(text)
                        .setContentTitle(room.name);

        NotificationManagerCompat notifMgr = NotificationManagerCompat.from(getApplicationContext());
        Notification notification = builder.build();

        notification.defaults = Notification.DEFAULT_LIGHTS;

        if (vibro) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }

        if (sound) {
            notification.defaults |= Notification.DEFAULT_SOUND;
        }

        notification.flags |= NotificationCompat.FLAG_AUTO_CANCEL;

        notifMgr.notify(NOTIF_CODE, notification);
    }

    @Override
    public void disconnect() {
        Log.d("MYTAG", "reconnect");
        mFayeClient = new FayeClient(this);
        mFayeClient.connect(Utils.getInstance().GITTER_FAYE_URL, Utils.getInstance().getAccessToken());
        mFayeClient.accessClientIdSubscriber().subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                RestAdapter adapter = new RestAdapter.Builder()
                        .setEndpoint(Utils.getInstance().GITTER_API_URL)
                        .build();
                final IApiMethods methods = adapter.create(IApiMethods.class);

                mRoomsList = methods.getCurrentUserRooms(Utils.getInstance().getBearer());
                createSubribers();
            }
        });
    }
}
