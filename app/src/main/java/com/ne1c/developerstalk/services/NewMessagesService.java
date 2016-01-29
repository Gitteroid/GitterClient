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

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.api.GitterStreaming;
import com.ne1c.developerstalk.dataprovides.ClientDatabase;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.ui.activities.MainActivity;
import com.ne1c.developerstalk.utils.Utils;

import java.util.List;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class NewMessagesService extends Service {
    public static final int NOTIF_REQUEST_CODE = 1000;
    public static final int NOTIF_CODE = 101;

    public static final String BROADCAST_SEND_MESSAGE = "sendMessageBroadcast";
    public static final String FROM_ROOM_EXTRA_KEY = "fromRoom";
    public static final String TO_ROOM_MESSAGE_EXTRA_KEY = "toRoom";
    public static final String SEND_MESSAGE_EXTRA_KEY = "sendMessageToRoom";

    private List<RoomModel> mRooms;
    private CompositeSubscription mMessagesSubscriptions;

    private GitterStreaming mStreaming;

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

        mStreaming = new GitterStreaming();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mEnableNotif = prefs.getBoolean("enable_notif", true);
        mSound = prefs.getBoolean("notif_sound", true);
        mVibro = prefs.getBoolean("notif_vibro", true);
        mWithUserName = prefs.getBoolean("notif_username", false);

        new ClientDatabase(getApplicationContext()).getRooms()
                .subscribe(roomModels -> {
                    mRooms = roomModels;

                    createSubscribers();
                });

        return START_STICKY;
    }

    private void createSubscribers() {
        mMessagesSubscriptions = new CompositeSubscription();

        for (final RoomModel room : mRooms) {
            Subscription sub = mStreaming.getMessageStream(room.id).subscribe(message -> {
                if (message.text != null) {
                    sendBroadcast(new Intent(MainActivity.BROADCAST_NEW_MESSAGE)
                            .putExtra(MainActivity.MESSAGE_INTENT_KEY, message)
                            .putExtra(MainActivity.ROOM_ID_INTENT_KEY, room.id));

                    if (mEnableNotif && !message.fromUser.id.equals(Utils.getInstance().getUserPref().id)) {
                        final String username = Utils.getInstance().getUserPref().username;

                        if (mWithUserName && message.text.contains(username) &&
                                !message.fromUser.username.equals(username)) {
                            sendNotificationMessage(room, message);
                        } else if (!mWithUserName) {
                            sendNotificationMessage(room, message);
                        }
                    }
                }
            });

            mMessagesSubscriptions.add(sub);
        }
    }

    @Override
    public void onDestroy() {
        if (!mMessagesSubscriptions.isUnsubscribed()) {
            mMessagesSubscriptions.unsubscribe();
        }

        unregisterReceiver(networkChangeReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utils.getInstance().isNetworkConnected()) {
                if (mMessagesSubscriptions.isUnsubscribed()) {
                    createSubscribers();
                }
            } else {
                if (!mMessagesSubscriptions.isUnsubscribed()) {
                    mMessagesSubscriptions.unsubscribe();
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
}