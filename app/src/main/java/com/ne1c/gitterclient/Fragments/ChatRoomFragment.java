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

import com.ne1c.gitterclient.Adapters.MessagesAdapter;
import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.R;
import com.ne1c.gitterclient.RetrofitServices.IApiMethods;
import com.ne1c.gitterclient.Utils;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ChatRoomFragment extends Fragment {

    private EditText mMessageEditText;
    private ImageButton mSendButton;
    private RecyclerView mMessagesList;
    private MessagesAdapter mMessagesAdapter;

    private ArrayList<MessageModel> mMessagesArr = new ArrayList<>();
    private RoomModel mRoom;

    private RestAdapter mRestApiAdapter;
    private RestAdapter mRestStreamAdapter;

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
        mMessagesList.setLayoutManager(new LinearLayoutManager(getActivity()));
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

        mRestStreamAdapter = new RestAdapter.Builder()
                .setEndpoint(Utils.getInstance().GITTER_STREAM_URL)
                .build();

        final LinearLayoutManager layoutManager = (LinearLayoutManager) mMessagesList.getLayoutManager();
        mMessagesList.addOnScrollListener(new RecyclerView.OnScrollListener() {

            int firstId;

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Помичает сообщение как прочитанное при скроле
                firstId = layoutManager.findFirstVisibleItemPosition();
                int lastId = layoutManager.findLastCompletelyVisibleItemPosition();

                // Диапазон видимых сообщений на экране
                int range = lastId - firstId;

                for (int i = firstId; i < range; i++) {
                    if (mMessagesArr.get(i).unread) {
                        mMessagesArr.get(i).unread = false;
                        mMessagesAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // При скролле вверх будет подругражть новые сообщения
//                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && firstId == 0) {
//                    countLoadMessage += 10;
//                    loadMessageRoom(mRoom);
//                }
            }
        });

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

    private void connectMessagesStream() {
        IApiMethods methods = mRestStreamAdapter.create(IApiMethods.class);
        methods.messageStream(Utils.getInstance().getBearer(), mRoom.id, new Callback<Response>() {
            @Override
            public void success(Response result, Response response) {
                Log.d("STREAM", "new message succes");
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d("STREAM", "new message failure");
            }
        });

    }

    // Event from MainActivity
    public void onEvent(RoomModel model) {
        mRoom = model;
        countLoadMessage = 10;
        connectMessagesStream();

        loadMessageRoom(mRoom);
    }
}
