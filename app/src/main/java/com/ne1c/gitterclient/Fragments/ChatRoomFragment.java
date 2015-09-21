package com.ne1c.gitterclient.Fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ne1c.gitterclient.Activities.MainActivity;
import com.ne1c.gitterclient.Adapters.MessagesAdapter;
import com.ne1c.gitterclient.Database.ClientDatabase;
import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.Models.StatusMessage;
import com.ne1c.gitterclient.Models.UserModel;
import com.ne1c.gitterclient.R;
import com.ne1c.gitterclient.RetrofitServices.IApiMethods;
import com.ne1c.gitterclient.Services.NewMessagesService;
import com.ne1c.gitterclient.UpdateMessageEventBus;
import com.ne1c.gitterclient.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

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
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
//                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                    int first = mListLayoutManager.findFirstCompletelyVisibleItemPosition();
//                    int last = mListLayoutManager.findLastCompletelyVisibleItemPosition();
//
//                    for (int i = first; i <= last; i++) {
//                        if (mMessagesArr.get(i).unread) {
//                            mMessagesArr.get(i).unread = false;
//                            mMessagesAdapter.read(((MessagesAdapter.ViewHolder) recyclerView.findViewHolderForAdapterPosition(i))
//                                    .newMessageIndicator, i);
//                        }
//                    }
//                }
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
                .setEndpoint(Utils.getInstance().GITTER_API_URL)
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
                            mMessagesList.scrollToPosition(mMessagesArr.size() - 1);
                        }

                        getActivity().sendBroadcast(new Intent(NewMessagesService.BROADCAST_SEND_MESSAGE)
                                .putExtra(NewMessagesService.SEND_MESSAGE_EXTRA_KEY, mMessageEditText.getText().toString())
                                .putExtra(NewMessagesService.TO_ROOM_MESSAGE_EXTRA_KEY, mRoom.id));
                    } else {
                        if (getView() != null) {
                            Snackbar.make(getView(), R.string.no_network, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    if (getView() != null) {
                        Snackbar.make(getView(), R.string.message_empty, Snackbar.LENGTH_SHORT).show();
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
                            mRoom.id, countLoadMessages, mMessagesArr.get(mMessagesArr.size() - 1).id,
                            new Callback<ArrayList<MessageModel>>() {
                                @Override
                                public void success(ArrayList<MessageModel> messageModels, Response response) {
                                    mMessagesArr.addAll(mMessagesArr.size() - 1, messageModels);
                                    mMessagesAdapter.notifyDataSetChanged();
                                    mPtrFrameLayout.refreshComplete();
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

                    if (mListLayoutManager.findLastCompletelyVisibleItemPosition() != mMessagesArr.size() - 1) {
                        mMessagesList.scrollToPosition(mMessagesArr.size() - 1);
                    }
                }

                if (showProgressBar) {
                    mPtrFrameLayout.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.GONE);
                }

                isRefreshing = false;
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

                if (model.equals(item)) {
                    return;
                }

                if (!item.sent.equals(StatusMessage.NO_SEND.name())
                        && !item.sent.equals(StatusMessage.SENDING.name())
                        && item.id.equals(model.id)) {
                    mMessagesArr.set(i, model);
                    mMessagesAdapter.notifyItemChanged(i);
                    return;
                }
            }

            mMessagesArr.add(model);
            mMessagesAdapter.notifyItemInserted(mMessagesArr.size() - 1);

            if (mListLayoutManager.findLastVisibleItemPosition() == mMessagesArr.size() - 2) {
                mMessagesList.smoothScrollToPosition(mMessagesArr.size() - 1);
            }

            if (model.text.equals(mMessageEditText.getText().toString()) &&
                    model.fromUser.username.equals(Utils.getInstance().getUserPref().username)) { // If user send message
                mMessageEditText.setText("");
            }
        }
    }

    @Override
    public void messageDelivered(MessageModel model) {
        for (int i = 0; i < mMessagesArr.size(); i++) {
            if (mMessagesArr.get(i).sent.equals(StatusMessage.SENDING.name()) &&
                    mMessagesArr.get(i).text.equals(model.text)) {
                mMessagesArr.set(i, model);
                mMessageEditText.setText("");
                mMessagesAdapter.notifyItemChanged(i);
                if (mListLayoutManager.findLastVisibleItemPosition() == mMessagesArr.size() - 2) {
                    mMessagesList.scrollToPosition(mMessagesArr.size() - 1);
                }
            }
        }
    }

    @Override
    public void onRefreshRoom() {
        if (Utils.getInstance().isNetworkConnected()) {
            loadMessageRoomServer(mRoom, true, true);
        } else if (getView() != null && mMessagesArr.size() > 0) {
            Snackbar.make(getView(), R.string.no_network, Snackbar.LENGTH_SHORT).show();
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
                            Toast.makeText(getActivity(), "Updated", Toast.LENGTH_SHORT).show();
                            mClientDatabase.insertMessage(model, mRoom.id);
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Toast.makeText(getActivity(), "Updated error", Toast.LENGTH_SHORT).show();
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

    public interface ReadMessageCallback {
        void read(ImageView indicator, int position);
    }
}
