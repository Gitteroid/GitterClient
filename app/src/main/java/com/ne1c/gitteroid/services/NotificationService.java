package com.ne1c.gitteroid.services;

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
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;

import com.ne1c.gitteroid.Application;
import com.ne1c.gitteroid.R;
import com.ne1c.gitteroid.api.GitterStreamer;
import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.models.data.MessageModel;
import com.ne1c.gitteroid.models.data.RoomModel;
import com.ne1c.gitteroid.ui.activities.MainActivity;
import com.ne1c.gitteroid.utils.Utils;

import java.util.List;

import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class NotificationService extends Service {
    public static final String BROADCAST_SEND_MESSAGE = "sendMessageBroadcast";
    public static final String FROM_ROOM_EXTRA_KEY = "fromRoom";
    public static final String TO_ROOM_MESSAGE_EXTRA_KEY = "toRoom";
    public static final String SEND_MESSAGE_EXTRA_KEY = "sendMessageToRoom";

    private static final int NOTIF_REQUEST_CODE = 1000;
    private static final int NOTIF_CODE = 101;

    private List<RoomModel> mRooms;
    private CompositeSubscription mSubscriptions = new CompositeSubscription();

    private GitterStreamer mStreamer;

    private boolean mEnableNotif;
    private boolean mSound;
    private boolean mVibrate;

    // Get messages only with user name
    private boolean mWithUserName;

    private DataManger mDataManger;

    @Override
    public void onCreate() {
        super.onCreate();

        mDataManger = ((Application) getApplication()).getDataManager();
        mStreamer = ((Application) getApplication()).getStreamer();

        createNetworkReceiver();
    }

    private void createNetworkReceiver() {
        IntentFilter filterNetwork = new IntentFilter();
        filterNetwork.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filterNetwork);
    }

    private void loadFlagsFromPrefs() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mEnableNotif = prefs.getBoolean("enable_notif", true);
        mSound = prefs.getBoolean("notif_sound", true);
        mVibrate = prefs.getBoolean("notif_vibro", true);
        mWithUserName = prefs.getBoolean("notif_username", false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        loadFlagsFromPrefs();

        return START_STICKY;
    }

    private void createRoomsSubscribers() {
        Subscription sub = mDataManger.getRooms(false)
                .subscribeOn(Schedulers.io())
                .subscribe(roomModels -> {
                    mRooms = roomModels;

                    for (RoomModel room : mRooms) {
                        Subscription sub1 = mStreamer.getMessageStream(room.id)
                                .subscribeOn(Schedulers.io())
                                .filter(message -> message.text != null)
                                .subscribe(message -> {
                                    notifyAboutMessage(room, message);
                                }, throwable -> {});

                        mSubscriptions.add(sub1);
                    }
                }, throwable -> {
                });

        mSubscriptions.add(sub);
    }


    private void notifyAboutMessage(RoomModel room, MessageModel message) {
        mDataManger.addSingleMessage(room.id, message);

        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(MainActivity.BROADCAST_NEW_MESSAGE)
                        .putExtra(MainActivity.MESSAGE_INTENT_KEY, message)
                        .putExtra(MainActivity.ROOM_ID_INTENT_KEY, room.id));

        boolean isOwnerAccount = message.fromUser.id.equals(Utils.getInstance().getUserPref().id);

        if (mEnableNotif && !isOwnerAccount) {
            final String username = Utils.getInstance().getUserPref().username;

            if (mWithUserName && message.text.contains(username) &&
                    !message.fromUser.username.equals(username)) {
                sendNotificationMessage(room, message);
            } else if (!mWithUserName) {
                sendNotificationMessage(room, message);
            }
        }
    }

    @Override
    public void onDestroy() {
        unsubscribeAll();
        unregisterReceiver(networkChangeReceiver);

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void unsubscribeAll() {
        mSubscriptions.unsubscribe();
        mSubscriptions.clear();
    }

    private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utils.getInstance().isNetworkConnected()) {
                createRoomsSubscribers();
            } else {
                unsubscribeAll();
            }
        }
    };

    private void sendNotificationMessage(RoomModel room, MessageModel message) {
        Intent notifIntent = new Intent(getApplicationContext(), MainActivity.class);
        notifIntent.putExtra(FROM_ROOM_EXTRA_KEY, room);
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

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
                        .setSmallIcon(R.drawable.ic_notif_message)
                        .setTicker(text)
                        .setContentText(text)
                        .setNumber(room.unreadItems)
                        .setContentTitle(room.name);

        NotificationManagerCompat notifMgr = NotificationManagerCompat.from(getApplicationContext());
        Notification notification = builder.build();

        notification.defaults = Notification.DEFAULT_LIGHTS;

        if (mVibrate) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }

        if (mSound) {
            notification.defaults |= Notification.DEFAULT_SOUND;
        }

        notification.flags |= NotificationCompat.FLAG_AUTO_CANCEL;

        notifMgr.notify(NOTIF_CODE, notification);
    }
}