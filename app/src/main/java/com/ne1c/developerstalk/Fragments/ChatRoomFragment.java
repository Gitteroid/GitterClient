package com.ne1c.developerstalk.Fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ne1c.developerstalk.Activities.MainActivity;
import com.ne1c.developerstalk.Adapters.MessagesAdapter;
import com.ne1c.developerstalk.Database.ClientDatabase;
import com.ne1c.developerstalk.EventBusModels.ReadMessagesEventBus;
import com.ne1c.developerstalk.Models.MessageModel;
import com.ne1c.developerstalk.Models.RoomModel;
import com.ne1c.developerstalk.Models.StatusMessage;
import com.ne1c.developerstalk.Models.UserModel;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.RetrofitServices.IApiMethods;
import com.ne1c.developerstalk.Services.NewMessagesService;
import com.ne1c.developerstalk.EventBusModels.UpdateMessageEventBus;
import com.ne1c.developerstalk.Utils;

import java.util.ArrayList;
import java.util.Collections;

import de.greenrobot.event.EventBus;
import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ChatRoomFragment extends Fragment implements MainActivity.NewMessageFragmentCallback,
        MainActivity.RefreshRoomCallback {
    private EditText mMessageEditText;
    private ImageButton mSendButton;
    private RecyclerView mMessagesList;
    private LinearLayoutManager mListLayoutManager;
    private MessagesAdapter mMessagesAdapter;
    private ProgressBar mProgressBar;

    private PtrClassicFrameLayout mPtrFrameLayout;

    private ArrayList<MessageModel> mMessagesArr = new ArrayList<>();
    private RoomModel mRoom;

    private RestAdapter mRestApiAdapter;
    private IApiMethods mApiMethods;

    private int countLoadMessages = 10;
    private boolean isRefreshing = false;

    private ClientDatabase mClientDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        EventBus.getDefault().register(this);

        mClientDatabase = new ClientDatabase(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat_room, container, false);

        mPtrFrameLayout = (PtrClassicFrameLayout) v.findViewById(R.id.ptr_framelayout);

        mMessageEditText = (EditText) v.findViewById(R.id.message_edit_text);
        mSendButton = (ImageButton) v.findViewById(R.id.send_button);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progress_bar);
        mProgressBar.setIndeterminate(true);

        mMessagesList = (RecyclerView) v.findViewById(R.id.messages_list);
        mListLayoutManager = new LinearLayoutManager(getActivity());
        mMessagesList.setLayoutManager(mListLayoutManager);
        // Animation for add new item or change item
        DefaultItemAnimator anim = new DefaultItemAnimator();
        anim.setAddDuration(1000);
        mMessagesList.setItemAnimator(anim);

        setDataToView(savedInstanceState);

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("scrollPosition", mMessagesList.getLayoutManager().onSaveInstanceState());
    }

    private void setDataToView(Bundle savedInstanceState) {
        mMessagesAdapter = new MessagesAdapter(getActivity(), mMessagesArr, mMessageEditText);
        mMessagesList.setAdapter(mMessagesAdapter);
        mMessagesList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(final RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    markMessagesAsRead(recyclerView);
                }
            }
        });

        if (savedInstanceState != null) {
            mMessagesList.getLayoutManager().onRestoreInstanceState(savedInstanceState.getParcelable("scrollPosition"));
        }

        if (isRefreshing) {
            isRefreshing = false;
            mPtrFrameLayout.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        mPtrFrameLayout.setSaveEnabled(true);

        mRestApiAdapter = new RestAdapter.Builder()
                .setEndpoint(Utils.GITTER_API_URL)
                .build();

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(mMessageEditText.getText().toString())) {
                    if (Utils.getInstance().isNetworkConnected()) {
                        MessageModel model = new MessageModel();
                        UserModel user = Utils.getInstance().getUserPref();
                        model.sent = StatusMessage.SENDING.name();
                        model.fromUser = user;
                        model.text = mMessageEditText.getText().toString();
                        model.urls = Collections.EMPTY_LIST;

                        mMessagesArr.add(model);
                        mMessagesAdapter.notifyItemInserted(mMessagesArr.size() - 1);

                        if (mListLayoutManager.findLastCompletelyVisibleItemPosition() != mMessagesArr.size() - 1) {
                            mMessagesList.smoothScrollToPosition(mMessagesArr.size() - 1);
                        }

                        getActivity().sendBroadcast(new Intent(NewMessagesService.BROADCAST_SEND_MESSAGE)
                                .putExtra(NewMessagesService.SEND_MESSAGE_EXTRA_KEY, mMessageEditText.getText().toString())
                                .putExtra(NewMessagesService.TO_ROOM_MESSAGE_EXTRA_KEY, mRoom.id));

                        mMessageEditText.setText("");
                    } else {
                        if (getView() != null) {
                            Toast.makeText(getActivity(), R.string.no_network, Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    if (getView() != null) {
                        Toast.makeText(getActivity(), R.string.message_empty, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        mPtrFrameLayout.setPtrHandler(new PtrHandler() {
            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout ptrFrameLayout, View view, View view1) {
                return PtrDefaultHandler.checkContentCanBePulledDown(ptrFrameLayout, view, view1);
            }

            @Override
            public void onRefreshBegin(PtrFrameLayout ptrFrameLayout) {
                // Message not sent or sending, it hasn't id
                if (!mMessagesArr.get(mMessagesArr.size() - 1).id.isEmpty()) {
                    mApiMethods.getMessagesBeforeId(Utils.getInstance().getBearer(),
                            mRoom.id, 10, mMessagesArr.get(0).id,
                            new Callback<ArrayList<MessageModel>>() {
                                @Override
                                public void success(ArrayList<MessageModel> messageModels, Response response) {
                                    mMessagesArr.addAll(0, messageModels);
                                    mMessagesAdapter.notifyItemRangeInserted(0, 10);
                                    mPtrFrameLayout.refreshComplete();

                                    countLoadMessages += 10;
                                }

                                @Override
                                public void failure(RetrofitError error) {
                                    mPtrFrameLayout.refreshComplete();
                                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
                //loadMessageRoomServer(mRoom, false, true);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);

        mClientDatabase.close();
        super.onDestroy();
    }

    private void markMessagesAsRead(final RecyclerView recyclerView) {
        final int first = mListLayoutManager.findFirstVisibleItemPosition();
        final int last = mListLayoutManager.findLastVisibleItemPosition();

        if (Utils.getInstance().isNetworkConnected()) {
            final ArrayList<String> listUnreadIds = new ArrayList<>();
            if (first > -1 && last > -1) {
                for (int i = first; i <= last; i++) {
                    if (mMessagesArr.get(i).unread) {
                        listUnreadIds.add(mMessagesArr.get(i).id);
                    }
                }
            }

            if (listUnreadIds.size() > 0) {
                listUnreadIds.add(""); // If single item
                mApiMethods.readMessages(Utils.getInstance().getBearer(),
                        Utils.getInstance().getUserPref().id,
                        mRoom.id,
                        listUnreadIds.toArray(new String[listUnreadIds.size()]),
                        new Callback<Response>() {
                            @Override
                            public void success(Response response, Response response2) {
                                for (int i = first; i <= last; i++) {
                                    MessagesAdapter.ViewHolder holder = (MessagesAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                                    if (holder != null) {
                                        mMessagesAdapter.read(holder.newMessageIndicator, i);
                                        mMessagesArr.get(i).unread = false;
                                    }
                                }

                                // Send notification to MainActivity
                                ReadMessagesEventBus readMessagesEventBus = new ReadMessagesEventBus();

                                // -1 but last item equals to empty string
                                readMessagesEventBus.setCountRead(listUnreadIds.size() - 1);
                                EventBus.getDefault().post(readMessagesEventBus);
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                Log.d("readmess", error.getMessage());
                                // If error read messages
                            }
                        });
            }
        }
    }

    private void loadMessageRoomServer(final RoomModel roomModel, final boolean showProgressBar, final boolean refresh) {
        mMessagesAdapter.setRoom(roomModel);

        isRefreshing = showProgressBar;
        if (showProgressBar) {
            mPtrFrameLayout.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        mApiMethods = mRestApiAdapter.create(IApiMethods.class);
        mApiMethods.getMessagesRoom(Utils.getInstance().getBearer(), roomModel.id, countLoadMessages, new Callback<ArrayList<MessageModel>>() {
            @Override
            public void success(ArrayList<MessageModel> messageModels, Response response) {
                mMessagesArr.clear();
                mMessagesArr.addAll(messageModels);

                mClientDatabase.insertMessages(messageModels, mRoom.id);

                if (refresh) {
                    mMessagesAdapter.notifyDataSetChanged();
                    mPtrFrameLayout.refreshComplete();
                } else {
                    mMessagesAdapter.notifyDataSetChanged();

                    // If room just was loaded
                    if (mListLayoutManager.findLastCompletelyVisibleItemPosition() != mMessagesArr.size() - 1) {
                        mMessagesList.scrollToPosition(mMessagesArr.size() - 1);
                    }
                }

                if (showProgressBar) {
                    mPtrFrameLayout.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.GONE);
                }

                isRefreshing = false;

                // Set this state for call process reading messages
                //markMessagesAsRead(mMessagesList);
            }

            @Override
            public void failure(RetrofitError error) {
                if (showProgressBar) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                } else if (mPtrFrameLayout.isRefreshing()) {
                    mPtrFrameLayout.refreshComplete();
                }
                isRefreshing = false;
                mPtrFrameLayout.setVisibility(View.VISIBLE);
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();

                if (error.getMessage().contains("401")) {
                    getActivity().sendBroadcast(new Intent(MainActivity.BROADCAST_UNAUTHORIZED));
                }
            }
        });
    }

    // Event from MainActivity or notification
    // Load messages of room
    public void onEvent(RoomModel model) {
        mRoom = model;
        countLoadMessages = 10;

        if (mPtrFrameLayout.isRefreshing()) {
            mPtrFrameLayout.refreshComplete();
        }

        mMessagesAdapter.setRoom(model);
        new LoadMessageDatabaseAsync(model, new LoadCallback() {
            @Override
            public void finish() {
                loadMessageRoomServer(mRoom, true, false);
            }
        }).execute();
    }

    @Override
    public void newMessage(MessageModel model) {
        mClientDatabase.insertMessage(model, mRoom.id);

        if (mMessagesAdapter != null) {
            for (int i = 0; i < mMessagesArr.size(); i++) { // If updated message or send message
                MessageModel item = mMessagesArr.get(i);

                if (model.text.equals(item.text) &&
                        item.sent.equals(StatusMessage.SENDING.name())) {
                    return;
                }

                if (!item.sent.equals(StatusMessage.NO_SEND.name())
                        && !item.sent.equals(StatusMessage.SENDING.name())
                        && item.id.equals(model.id)) {
                    mMessagesArr.set(i, model);
                    mMessagesAdapter.setMessage(i, model);
                    return;
                }
            }

            mMessagesArr.add(model);
            mMessagesAdapter.notifyItemInserted(mMessagesArr.size() - 1);

            if (mListLayoutManager.findLastVisibleItemPosition() == mMessagesArr.size() - 2) {
                mMessagesList.smoothScrollToPosition(mMessagesArr.size() - 1);
            }

            markMessagesAsRead(mMessagesList);
        }
    }

    @Override
    public void messageDelivered(MessageModel model) {
        for (int i = 0; i < mMessagesArr.size(); i++) {
            if (mMessagesArr.get(i).sent.equals(StatusMessage.SENDING.name()) &&
                    mMessagesArr.get(i).text.equals(model.text)) {
                mMessagesArr.set(i, model);
                mMessagesAdapter.notifyItemChanged(i);

                if (mListLayoutManager.findLastVisibleItemPosition() == mMessagesArr.size() - 2) {
                    mMessagesList.scrollToPosition(mMessagesArr.size() - 1);
                }
            }
        }
    }

    @Override
    public void messageErrorDelivered(MessageModel model) {
        Toast.makeText(getActivity(), R.string.error_send, Toast.LENGTH_SHORT).show();

        for (int i = 0; i < mMessagesArr.size(); i++) {
            if (mMessagesArr.get(i).sent.equals(StatusMessage.SENDING.name())) {
                mMessagesArr.get(i).sent = StatusMessage.NO_SEND.name();
                mMessagesAdapter.notifyItemChanged(i);
            }
        }
    }

    @Override
    public void onRefreshRoom() {
        if (Utils.getInstance().isNetworkConnected()) {
            loadMessageRoomServer(mRoom, true, true);
        } else if (getView() != null && mMessagesArr.size() > 0) {
            Toast.makeText(getActivity(), R.string.no_network, Toast.LENGTH_SHORT).show();
        }
    }

    // Event from MainActivity
    public void onEvent(UpdateMessageEventBus message) {
        MessageModel newMessage = message.getMessageModel();

        if (newMessage != null) {
            mApiMethods.updateMessage(Utils.getInstance().getBearer(),
                    mRoom.id, newMessage.id, newMessage.text,
                    new Callback<MessageModel>() {
                        @Override
                        public void success(MessageModel model, Response response) {
                            Toast.makeText(getActivity(), R.string.updated, Toast.LENGTH_SHORT).show();
                            mClientDatabase.insertMessage(model, mRoom.id);
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Toast.makeText(getActivity(), R.string.updated_error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private class LoadMessageDatabaseAsync extends AsyncTask<String, Void, ArrayList<MessageModel>> {
        private RoomModel mRoom;
        private ClientDatabase mClientDatabase;
        private LoadCallback mCallback;

        private LoadMessageDatabaseAsync(RoomModel room, LoadCallback callback) {
            mRoom = room;
            mClientDatabase = new ClientDatabase(getActivity());
            mCallback = callback;
        }

        private LoadMessageDatabaseAsync(RoomModel room) {
            mRoom = room;
            mClientDatabase = new ClientDatabase(getActivity());
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mPtrFrameLayout.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<MessageModel> doInBackground(String... params) {
            return mClientDatabase.getMessages(mRoom.id);
        }

        @Override
        protected void onPostExecute(ArrayList<MessageModel> messageModels) {
            super.onPostExecute(messageModels);

            mMessagesArr.clear();
            mMessagesArr.addAll(messageModels);

            mMessagesAdapter.setRoom(mRoom);
            mMessagesAdapter.notifyDataSetChanged();

            mMessagesList.scrollToPosition(mMessagesArr.size() - 1);

            mPtrFrameLayout.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);

            if (mCallback != null) {
                mCallback.finish();
            }
        }
    }

    // For load callback messages from database
    private interface LoadCallback {
        void finish();
    }

    // Callback for adapter
    public interface ReadMessageCallback {
        void read(ImageView indicator, int position);
    }
}
