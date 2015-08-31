package com.ne1c.gitterclient.Services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.net.ConnectivityManagerCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.Toast;

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
import rx.Observable;
import rx.functions.Action1;


public class NewMessagesService extends Service {

    public static final int NOTIF_REQUEST_CODE = 1000;
    public static final int NOTIF_CODE = 101;

    public static final String BROADCAST_SEND_MESSAGE = "sendMessage";
    public static final String FROM_ROOM_EXTRA_KEY = "fromRoom";
    public static final String NEW_MESSAGE_EXTRA_KEY = "newMessage";
    public static final String CHANGED_ROOMS_EXTRA_KEY = "newRoomsList";

    private FayeClient mFayeClient;
    private ArrayList<RoomModel> mRoomsList;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filter);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Update rooms list, and update subscribers
        if (flags == START_FLAG_RETRY) {
            RestAdapter adapter = new RestAdapter.Builder()
                    .setEndpoint(Utils.getInstance().GITTER_API_URL)
                    .build();
            final IApiMethods methods = adapter.create(IApiMethods.class);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mRoomsList = methods.getCurrentUserRooms(Utils.getInstance().getBearer());
                    createSubribers();
                }
            });

            thread.start();
        }

        return START_NOT_STICKY;
    }

    private void createSubribers() {
        String channelEndpoint = "/api/v1/rooms/%s/chatMessages";
        String roomsUserEndpoint = "/api/v1/user/%s/rooms";
        String roomEventEndpoint = "/api/v1/rooms/%s/events";
        String userEventsEndpoint = "/api/v1/user/%s";

        for (final RoomModel room : mRoomsList) {
            mFayeClient.subscribeChannel(String.format(channelEndpoint, room.id))
                    .subscribe(new Action1<JsonObject>() {
                        @Override
                        public void call(JsonObject response) {
                            Intent intent = new Intent();
                            intent.setAction(MainActivity.BROADCAST_NEW_MESSAGE);
                            intent.putExtra(FROM_ROOM_EXTRA_KEY, room);

                            Gson gson = new GsonBuilder().create();
                            MessageModel message = gson.fromJson(response.getAsJsonObject("data").getAsJsonObject("model"), MessageModel.class);

                            intent.putExtra(NEW_MESSAGE_EXTRA_KEY, message);
                            if (message.text != null) {
                                sendBroadcast(intent);
                                sendNotificationMessage(room, message);
                            }
                        }
                    });
        }
    }

    @Override
    public void onDestroy() {
        mFayeClient.disconnect();
        unregisterReceiver(networkChangeReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver sendMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mFayeClient == null) {
                mFayeClient = new FayeClient(getApplicationContext());
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
        notification.defaults = Notification.DEFAULT_LIGHTS |
                Notification.DEFAULT_VIBRATE;
        notification.flags |= NotificationCompat.FLAG_AUTO_CANCEL;

        notifMgr.notify(NOTIF_CODE, notification);
    }
}
