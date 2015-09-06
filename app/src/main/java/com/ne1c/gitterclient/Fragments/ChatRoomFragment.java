package com.ne1c.gitterclient.Fragments;


import android.content.Intent;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ne1c.gitterclient.Activities.MainActivity;
import com.ne1c.gitterclient.Adapters.MessagesAdapter;
import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.R;
import com.ne1c.gitterclient.RetrofitServices.IApiMethods;
import com.ne1c.gitterclient.Services.NewMessagesService;
import com.ne1c.gitterclient.UpdateMessageEventBus;
import com.ne1c.gitterclient.Utils;

import java.util.ArrayList;

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

        if (savedInstanceState != null) {
            mMessagesList.getLayoutManager().onRestoreInstanceState(savedInstanceState.getParcelable("scrollPosition"));
        }

        mRestApiAdapter = new RestAdapter.Builder()
                .setEndpoint(Utils.getInstance().GITTER_API_URL)
                .build();

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(mMessageEditText.getText().toString())) {
                    if (Utils.getInstance().isNetworkConnected()) {
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
                countLoadMessages += 10;
                loadMessageRoom(mRoom, false, true);
            }
        });
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void loadMessageRoom(final RoomModel roomModel, final boolean showProgressBar, final boolean refresh) {
        mMessagesAdapter.setRoom(roomModel);

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
            }

            @Override
            public void failure(RetrofitError error) {
                if (showProgressBar) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                } else if (mPtrFrameLayout.isRefreshing()) {
                    mPtrFrameLayout.refreshComplete();
                }

                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Event from MainActivity or notification
    public void onEvent(RoomModel model) {
        mRoom = model;
        countLoadMessages = 10;

        if (mPtrFrameLayout.isRefreshing()) {
            mPtrFrameLayout.refreshComplete();
        }
        loadMessageRoom(mRoom, true, false);
    }

    @Override
    public void newMessage(MessageModel model) {
        if (mMessagesAdapter != null) {
            for (int i = 0; i < mMessagesArr.size(); i++) { // If updated message
                if (mMessagesArr.get(i).id.equals(model.id)) {
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
    public void onRefreshRoom() {
        if (Utils.getInstance().isNetworkConnected()) {
            loadMessageRoom(mRoom, true, true);
        } else if (getView() != null) {
            Snackbar.make(getView(), R.string.no_network, Snackbar.LENGTH_SHORT).show();
        }
    }

    public void onEvent(UpdateMessageEventBus message) {
        MessageModel newMessage = message.getMessageModel();

        if (newMessage != null) {
            mApiMethods.updateMessage(Utils.getInstance().getBearer(),
                    mRoom.id, newMessage.id, newMessage.text,
                    new Callback<MessageModel>() {
                        @Override
                        public void success(MessageModel model, Response response) {
                            Toast.makeText(getActivity(), "Updated", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Toast.makeText(getActivity(), "Updated error", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
