package com.ne1c.gitterclient.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.ne1c.gitterclient.Activities.MainActivity;
import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.RetrofitServices.IApiMethods;
import com.ne1c.gitterclient.Utils;

import java.util.ArrayList;

import retrofit.RestAdapter;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;


public class NewMessagesService extends Service {

    public static final String BROADCAST_SEND_MESSAGE = "sendMessage";
    public static final String FROM_ROOM_EXTRA_KEY = "fromRoom";
    public static final String NEW_MESSAGE_EXTRA_KEY = "newMessage";

    private FayeClient mFayeClient;
    private ArrayList<RoomModel> mRoomsList;

    private Observable<Void> mObservableFaye;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mFayeClient = new FayeClient(getApplicationContext());
        mObservableFaye = mFayeClient.connect(Utils.getInstance().GITTER_FAYE_URL, Utils.getInstance().getAccessToken());

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_NOT_STICKY;
    }

    private void createSubribers() {
        String channelEndpoint = "/api/v1/rooms/%s/chatMessages";
        String roomsUserEndpoint = "/api/v1/user/%s/rooms";
        String roomEventEndpoint = "/api/v1/rooms/%s/events";

        for (final RoomModel room : mRoomsList) {
            mFayeClient.subscribeChannel(String.format(channelEndpoint, room.id))
                    .subscribe(new Action1<JsonObject>() {
                        @Override
                        public void call(JsonObject response) {
                            Intent intent = new Intent();
                            intent.setAction(MainActivity.BROADCAST_NEW_MESSAGE);
                            intent.putExtra(FROM_ROOM_EXTRA_KEY, room);

                            Gson gson = new GsonBuilder().create();
                            MessageModel model = gson.fromJson(response.getAsJsonObject("data").getAsJsonObject("model"), MessageModel.class);

                            intent.putExtra(NEW_MESSAGE_EXTRA_KEY, model);
                            if (model.text != null) {
                                sendBroadcast(intent);
                            }
                        }
                    });
        }
    }

    @Override
    public void onDestroy() {
        mFayeClient.disconnect();
        super.onDestroy();
    }

    private BroadcastReceiver sendMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };
}
