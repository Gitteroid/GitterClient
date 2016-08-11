package com.ne1c.gitteroid.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.LocalBroadcastManager
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.api.GitterStreamer
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.DependencyManager
import com.ne1c.gitteroid.di.base.NetworkService
import com.ne1c.gitteroid.models.data.MessageModel
import com.ne1c.gitteroid.models.data.RoomModel
import com.ne1c.gitteroid.ui.activities.MainActivity
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription

class NotificationService : Service() {
    private var mRooms: List<RoomModel>? = null
    private val mSubscriptions = CompositeSubscription()

    private var mStreamer: GitterStreamer? = null

    private var mEnableNotif: Boolean = false
    private var mSound: Boolean = false
    private var mVibrate: Boolean = false

    // Get messages only with user name
    private var mWithUserName: Boolean = false

    private var mDataManger: DataManger? = null
    private var mNetworkService: NetworkService? = null

    override fun onCreate() {
        super.onCreate()

        mDataManger = DependencyManager.INSTANCE.dataManager
        mStreamer = DependencyManager.INSTANCE.gitterStreamer
        mNetworkService = DependencyManager.INSTANCE.networkService

        createNetworkReceiver()
    }

    private fun createNetworkReceiver() {
        val filterNetwork = IntentFilter()
        filterNetwork.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, filterNetwork)
    }

    private fun loadFlagsFromPrefs() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        mEnableNotif = prefs.getBoolean("enable_notif", true)
        mSound = prefs.getBoolean("notif_sound", true)
        mVibrate = prefs.getBoolean("notif_vibro", true)
        mWithUserName = prefs.getBoolean("notif_username", false)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        loadFlagsFromPrefs()

        return Service.START_STICKY
    }

    private fun createRoomsSubscribers() {
        val sub = mDataManger!!.getRooms(false).subscribeOn(Schedulers.io()).subscribe({ roomModels ->
            mRooms = roomModels

            for (room in mRooms!!) {
                val sub1 = mStreamer!!.getMessageStream(room.id).subscribeOn(Schedulers.io()).filter { message -> message.text != null }.subscribe({ message -> notifyAboutMessage(room, message) }) { throwable -> }

                mSubscriptions.add(sub1)
            }
        }) { throwable -> }

        mSubscriptions.add(sub)
    }


    private fun notifyAboutMessage(room: RoomModel, message: MessageModel) {
        mDataManger!!.addSingleMessage(room.id, message)

        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(MainActivity.BROADCAST_NEW_MESSAGE).putExtra(MainActivity.MESSAGE_INTENT_KEY, message).putExtra(MainActivity.ROOM_ID_INTENT_KEY, room.id))

        val isOwnerAccount = message.fromUser.id == mDataManger?.getUser()?.id

        if (mEnableNotif && !isOwnerAccount) {
            val username = mDataManger?.getUser()?.username

            if (mWithUserName && message.text.contains(username!!) &&
                    message.fromUser.username != username) {
                sendNotificationMessage(room, message)
            } else if (!mWithUserName) {
                sendNotificationMessage(room, message)
            }
        }
    }

    override fun onDestroy() {
        unsubscribeAll()
        unregisterReceiver(networkChangeReceiver)

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun unsubscribeAll() {
        mSubscriptions.unsubscribe()
        mSubscriptions.clear()
    }

    private val networkChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mNetworkService!!.isConnected()) {
                createRoomsSubscribers()
            } else {
                unsubscribeAll()
            }
        }
    }

    private fun sendNotificationMessage(room: RoomModel, message: MessageModel) {
        val notifIntent = Intent(applicationContext, MainActivity::class.java)
        notifIntent.putExtra(FROM_ROOM_EXTRA_KEY, room)
        notifIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                NOTIF_REQUEST_CODE,
                notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val username = message.fromUser.username
        val text = SpannableString(username + ": " + message.text)
        text.setSpan(StyleSpan(Typeface.BOLD), 0, username.length + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(StyleSpan(Typeface.ITALIC), username.length + 1, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val builder = NotificationCompat.Builder(this).setContentIntent(pendingIntent).setSmallIcon(R.drawable.ic_notif_message).setTicker(text).setContentText(text).setNumber(room.unreadItems).setContentTitle(room.name)

        val notifMgr = NotificationManagerCompat.from(applicationContext)
        val notification = builder.build()

        notification.defaults = Notification.DEFAULT_LIGHTS

        if (mVibrate) {
            notification.defaults = notification.defaults or Notification.DEFAULT_VIBRATE
        }

        if (mSound) {
            notification.defaults = notification.defaults or Notification.DEFAULT_SOUND
        }

        notification.flags = notification.flags or NotificationCompat.FLAG_AUTO_CANCEL

        notifMgr.notify(NOTIF_CODE, notification)
    }

    companion object {
        val BROADCAST_SEND_MESSAGE = "sendMessageBroadcast"
        val FROM_ROOM_EXTRA_KEY = "fromRoom"
        val TO_ROOM_MESSAGE_EXTRA_KEY = "toRoom"
        val SEND_MESSAGE_EXTRA_KEY = "sendMessageToRoom"

        private val NOTIF_REQUEST_CODE = 1000
        private val NOTIF_CODE = 101
    }
}