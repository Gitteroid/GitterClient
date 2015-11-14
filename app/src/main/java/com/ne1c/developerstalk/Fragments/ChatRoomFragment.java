package com.ne1c.developerstalk.Fragments;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.ne1c.developerstalk.EventBusModels.UpdateMessageEventBus;
import com.ne1c.developerstalk.Models.MessageModel;
import com.ne1c.developerstalk.Models.RoomModel;
import com.ne1c.developerstalk.Models.StatusMessage;
import com.ne1c.developerstalk.Models.UserModel;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.RetrofitServices.IApiMethods;
import com.ne1c.developerstalk.Services.NewMessagesService;
import com.ne1c.developerstalk.Util.MarkdownUtils;
import com.ne1c.developerstalk.Util.Utils;

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

    private int startNumberLoadMessages = 10;
    private int countLoadMessages = 0;
    private boolean isRefreshing = false;

    private ClientDatabase mClientDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        EventBus.getDefault().register(this);

        mClientDatabase = new ClientDatabase(getActivity());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        startNumberLoadMessages = Integer.valueOf(prefs.getString("number_load_mess", "10"));
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
        mMessagesList.setItemViewCacheSize(50);

        // Animation for add new item or change item
        DefaultItemAnimator anim = new DefaultItemAnimator();
        anim.setAddDuration(1000);
        mMessagesList.setItemAnimator(anim);

        setDataToView(savedInstanceState);

        v.findViewById(R.id.markdown_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogMarkdownFragment dialog = new DialogMarkdownFragment();
                dialog.setTargetFragment(ChatRoomFragment.this, DialogMarkdownFragment.REQUEST_CODE);
                dialog.show(getFragmentManager(), "MARKDOWN_DIALOG");
            }
        });
        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DialogMarkdownFragment.REQUEST_CODE) {
            switch (data.getIntExtra("layout_id", -1)) {
                case MarkdownUtils.SINGLELINE_CODE:
                    mMessageEditText.append("``");
                    mMessageEditText.setSelection(mMessageEditText.length() - 1);
                    break;
                case MarkdownUtils.MULTILINE_CODE:
                    if (mMessageEditText.length() > 0) {
                        mMessageEditText.append("``````");
                    } else {
                        mMessageEditText.append("\n``````");
                    }

                    mMessageEditText.setSelection(mMessageEditText.length() - 3);
                    break;
                case MarkdownUtils.BOLD:
                    mMessageEditText.append("****");
                    mMessageEditText.setSelection(mMessageEditText.length() - 2);
                    break;
                case MarkdownUtils.ITALICS:
                    mMessageEditText.append("**");
                    mMessageEditText.setSelection(mMessageEditText.length() - 1);
                    break;
                case MarkdownUtils.STRIKETHROUGH:
                    mMessageEditText.append("~~~~");
                    mMessageEditText.setSelection(mMessageEditText.length() - 2);
                    break;
                case MarkdownUtils.QUOTE:
                    if (mMessageEditText.length() > 0) {
                        mMessageEditText.append("\n>");
                    } else {
                        mMessageEditText.append(">");
                    }

                    mMessageEditText.setSelection(mMessageEditText.length());
                    break;
                case MarkdownUtils.LINK:
                    mMessageEditText.append("[](http://)");
                    mMessageEditText.setSelection(mMessageEditText.length() - 10);
                    break;
                case MarkdownUtils.IMAGE_LINK:
                    mMessageEditText.append("![](http://)");
                    mMessageEditText.setSelection(mMessageEditText.length() - 10);
                    break;
                default: break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
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
                if (!mMessageEditText.getText().toString().isEmpty()) {
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
                if (mMessagesArr.size() > 0 && !mMessagesArr.get(mMessagesArr.size() - 1).id.isEmpty()) {
                    mApiMethods.getMessagesBeforeId(Utils.getInstance().getBearer(),
                            mRoom.id, 10, mMessagesArr.get(0).id,
                            new Callback<ArrayList<MessageModel>>() {
                                @Override
                                public void success(ArrayList<MessageModel> messageModels, Response response) {
                                    if (messageModels.size() > 0) {
                                        mMessagesArr.addAll(0, messageModels);
                                        mMessagesAdapter.notifyItemRangeInserted(0, messageModels.size());

                                        countLoadMessages += messageModels.size();
                                    }

                                    mPtrFrameLayout.refreshComplete();
                                }

                                @Override
                                public void failure(RetrofitError error) {
                                    mPtrFrameLayout.refreshComplete();
                                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    mPtrFrameLayout.refreshComplete();
                }
            }
        });

        mMessageEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int last = mListLayoutManager.findLastVisibleItemPosition();
                if (last != mMessagesArr.size() - 1) {
                    mListLayoutManager.scrollToPosition(mMessagesArr.size() - 1);
                }
            }
        });
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
                                    if (mMessagesAdapter.getItemViewType(i) == 0) {
                                        MessagesAdapter.DynamicViewHolder holder = (MessagesAdapter.DynamicViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                                        if (holder != null) {
                                            mMessagesAdapter.read(holder.newMessageIndicator, i);
                                            mMessagesArr.get(i).unread = false;
                                        }
                                    } else {
                                        MessagesAdapter.StaticViewHolder holder = (MessagesAdapter.StaticViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                                        if (holder != null) {
                                            mMessagesAdapter.read(holder.newMessageIndicator, i);
                                            mMessagesArr.get(i).unread = false;
                                        }
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
        mApiMethods.getMessagesRoom(Utils.getInstance().getBearer(), roomModel.id, startNumberLoadMessages + countLoadMessages, new Callback<ArrayList<MessageModel>>() {
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
        countLoadMessages = 0;

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

                // Send message
                if (model.text.equals(item.text) &&
                        item.sent.equals(StatusMessage.SENDING.name())) {
                    return;
                }

                // Update message
                if (!item.sent.equals(StatusMessage.NO_SEND.name())
                        && !item.sent.equals(StatusMessage.SENDING.name())
                        && item.id.equals(model.id)) {
                    mMessagesArr.set(i, model);
                    mMessagesAdapter.setMessage(i, model);
                    mMessagesAdapter.notifyItemChanged(i);
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
//
//    @Override
//    public void retrySendNotif(MessageModel message, int position) {
//        mMessagesArr.set(position, message);
//        mMessagesAdapter.notifyItemChanged(position);
//    }

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
