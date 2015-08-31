package com.ne1c.gitterclient.Fragments;


import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ne1c.gitterclient.Activities.MainActivity;
import com.ne1c.gitterclient.Adapters.MessagesAdapter;
import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.R;
import com.ne1c.gitterclient.RetrofitServices.IApiMethods;
import com.ne1c.gitterclient.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.ws.WebSocketCall;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;

public class ChatRoomFragment extends Fragment implements MainActivity.NewMessageFragmentCallback {

    private EditText mMessageEditText;
    private ImageButton mSendButton;
    private RecyclerView mMessagesList;
    private LinearLayoutManager mListLayoutManager;
    private MessagesAdapter mMessagesAdapter;

    private ArrayList<MessageModel> mMessagesArr = new ArrayList<>();
    private RoomModel mRoom;

    private RestAdapter mRestApiAdapter;

    private int countLoadMessage = 10;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat_room, container, false);

        mMessageEditText = (EditText) v.findViewById(R.id.message_edit_text);
        mSendButton = (ImageButton) v.findViewById(R.id.send_button);

        mMessagesList = (RecyclerView) v.findViewById(R.id.messages_list);
        mListLayoutManager = new LinearLayoutManager(getActivity());
        mMessagesList.setLayoutManager(mListLayoutManager);
        mMessagesList.setItemAnimator(new DefaultItemAnimator());
        mMessagesList.setItemViewCacheSize(120);

        setDataToView();

        return v;
    }

    private void setDataToView() {
        mMessagesAdapter = new MessagesAdapter(getActivity().getApplicationContext(), mMessagesArr, mMessageEditText);
        mMessagesList.setAdapter(mMessagesAdapter);

        mRestApiAdapter = new RestAdapter.Builder()
                .setEndpoint(Utils.getInstance().GITTER_API_URL)
                .build();

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void loadMessageRoom(final RoomModel roomModel) {
        IApiMethods methods = mRestApiAdapter.create(IApiMethods.class);
        methods.getMessagesRoom(Utils.getInstance().getBearer(), roomModel.id, countLoadMessage, new Callback<ArrayList<MessageModel>>() {
            @Override
            public void success(ArrayList<MessageModel> messageModels, Response response) {
                mMessagesArr.clear();
                mMessagesArr.addAll(messageModels);

                if (countLoadMessage > 10) {
                    mMessagesAdapter.notifyItemRangeInserted(0, countLoadMessage - 10);
                } else {
                    mMessagesAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Event from MainActivity or notification
    public void onEvent(RoomModel model) {
        mRoom = model;
        countLoadMessage = 10;

        loadMessageRoom(mRoom);
    }

    @Override
    public void newMessage(MessageModel model) {
        if (mMessagesAdapter != null) {
            if (mListLayoutManager.findLastVisibleItemPosition() == mMessagesArr.size() - 1) {
                mMessagesArr.add(model);
                mMessagesAdapter.notifyItemInserted(mMessagesArr.size() - 1);
                mMessagesList.smoothScrollToPosition(mMessagesArr.size() - 1);
            }
        }
    }
}
