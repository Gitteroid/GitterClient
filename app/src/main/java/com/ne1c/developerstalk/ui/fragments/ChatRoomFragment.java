package com.ne1c.developerstalk.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aspsine.swipetoloadlayout.OnRefreshListener;
import com.aspsine.swipetoloadlayout.SwipeToLoadLayout;
import com.ne1c.developerstalk.Application;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.di.components.ChatRoomComponent;
import com.ne1c.developerstalk.di.components.DaggerChatRoomComponent;
import com.ne1c.developerstalk.di.modules.ChatRoomPresenterModule;
import com.ne1c.developerstalk.events.NewMessageEvent;
import com.ne1c.developerstalk.events.ReadMessagesEvent;
import com.ne1c.developerstalk.events.UpdateMessageEvent;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.models.StatusMessage;
import com.ne1c.developerstalk.presenters.ChatRoomPresenter;
import com.ne1c.developerstalk.ui.activities.MainActivity;
import com.ne1c.developerstalk.ui.adapters.MessagesAdapter;
import com.ne1c.developerstalk.ui.views.ChatView;
import com.ne1c.developerstalk.utils.MarkdownUtils;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class ChatRoomFragment extends BaseFragment implements ChatView, OnRefreshListener {
    private EditText mMessageEditText;
    private ImageButton mSendButton;
    private RecyclerView mMessagesList;
    private LinearLayoutManager mListLayoutManager;
    private MessagesAdapter mMessagesAdapter;
    private ProgressBar mProgressBar;

    private SwipeToLoadLayout mSwipeLoadLayout;

    private ArrayList<MessageModel> mMessagesArr = new ArrayList<>();
    private RoomModel mRoom;

    private Parcelable mMessageListSavedState;

    private ChatRoomComponent mComponent;

    @Inject
    ChatRoomPresenter mPresenter;

    private int startNumberLoadMessages = 10;
    private int countLoadMessages = 0;
    private boolean isRefreshing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        startNumberLoadMessages = Integer.valueOf(prefs.getString("number_load_mess", "10"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat_room, container, false);

        //mPtrFrameLayout = (PtrClassicFrameLayout) v.findViewById(R.id.ptr_framelayout);
        mSwipeLoadLayout = (SwipeToLoadLayout) v.findViewById(R.id.refresh_messages_layout);
        mSwipeLoadLayout.setOnRefreshListener(this);
        mSwipeLoadLayout.setRefreshHeaderView(v.findViewById(R.id.swipe_refresh_header));

        mMessageEditText = (EditText) v.findViewById(R.id.message_edit_text);
        mSendButton = (ImageButton) v.findViewById(R.id.send_button);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progress_bar);
        mProgressBar.setIndeterminate(true);

        mMessagesList = (RecyclerView) v.findViewById(R.id.swipe_target);
        mListLayoutManager = new LinearLayoutManager(getActivity());
        mMessagesList.setLayoutManager(mListLayoutManager);
        mMessagesList.setItemViewCacheSize(50);
        mMessagesList.setScrollContainer(true);

        // Animation for add new item or change item
//        ScaleInBottomAnimator anim = new ScaleInBottomAnimator();
//        anim.setAddDuration(500);
//        anim.setChangeDuration(0);
//

//        mMessagesList.setItemAnimator(anim);

        setDataToView(savedInstanceState);

        v.findViewById(R.id.markdown_button).setOnClickListener(v1 -> {
            DialogMarkdownFragment dialog = new DialogMarkdownFragment();
            dialog.setTargetFragment(ChatRoomFragment.this, DialogMarkdownFragment.REQUEST_CODE);
            dialog.show(getFragmentManager(), "MARKDOWN_DIALOG");
        });

        mPresenter.bindView(this);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
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
                case MarkdownUtils.GITTER_LINK:
                    mMessageEditText.append("[](http://)");
                    mMessageEditText.setSelection(mMessageEditText.length() - 10);
                    break;
                case MarkdownUtils.IMAGE_LINK:
                    mMessageEditText.append("![](http://)");
                    mMessageEditText.setSelection(mMessageEditText.length() - 10);
                    break;
                default:
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("scrollPosition", mMessagesList.getLayoutManager().onSaveInstanceState());
        outState.putParcelable("active_room", mRoom);
        outState.putParcelableArrayList("messages", mMessagesArr);
    }

    private void setDataToView(Bundle savedInstanceState) {
        mMessagesAdapter = new MessagesAdapter(((Application) getActivity().getApplication()).getDataManager(),
                getActivity(),
                mMessagesArr,
                mMessageEditText);

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
            mMessageListSavedState = savedInstanceState.getParcelable("scrollPosition");
            savedInstanceState.remove("scrollPosition");

            RoomModel room = savedInstanceState.getParcelable("active_room");
            savedInstanceState.remove("active_room");

            if (room != null) {
                mRoom = room;
                mMessagesAdapter.setRoom(mRoom);
            }

            ArrayList<MessageModel> messages = savedInstanceState.getParcelableArrayList("messages");
            savedInstanceState.remove("messages");

            if (messages != null && messages.size() > 0) {
                showMessages((ArrayList<MessageModel>) messages.clone());
            }
        }

        if (isRefreshing) {
            isRefreshing = false;
            mSwipeLoadLayout.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        mSwipeLoadLayout.setSaveEnabled(true);

        mSendButton.setOnClickListener(v -> {
            if (!mMessageEditText.getText().toString().isEmpty()) {
                if (Utils.getInstance().isNetworkConnected()) {
                    MessageModel model = mPresenter.createSendMessage(mMessageEditText.getText().toString());

                    mMessagesArr.add(model);
                    mMessagesAdapter.notifyItemInserted(mMessagesArr.size() - 1);

                    if (mListLayoutManager.findLastCompletelyVisibleItemPosition() != mMessagesArr.size() - 2) {
                        mMessagesList.smoothScrollToPosition(mMessagesArr.size() - 1);
                    }

                    mPresenter.sendMessage(mRoom.id, model.text);
                    mMessageEditText.setText("");
                } else {
                    Toast.makeText(getActivity(), R.string.no_network, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), R.string.message_empty, Toast.LENGTH_SHORT).show();
            }
        });

        mMessageEditText.setOnClickListener(v -> {
            int last = mListLayoutManager.findLastVisibleItemPosition();
            if (last != mMessagesArr.size() - 1) {
                mListLayoutManager.scrollToPosition(mMessagesArr.size() - 1);
            }
        });
    }

    @Override
    public void onDestroy() {
        mPresenter.unbindView();
        mComponent = null;

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
                listUnreadIds.add(""); // If listUnreadIds have one item

                final String roomId = mRoom.id;

                mPresenter.markMessageAsRead(first, last, roomId,
                        listUnreadIds.toArray(new String[listUnreadIds.size()]));
            }
        }
    }

    private void loadMessageRoomServer(final RoomModel roomModel) {
        mMessagesAdapter.setRoom(roomModel);

        mPresenter.loadMessages(roomModel.id, startNumberLoadMessages + countLoadMessages);
    }

    // Event from MainActivity or notification
    // Load messages of room
    public void onEvent(RoomModel model) {
        mRoom = model;
        countLoadMessages = 0;

        if (mSwipeLoadLayout.isRefreshing()) {
            mSwipeLoadLayout.setRefreshing(false);
        }

        mMessagesAdapter.setRoom(model);
        mPresenter.loadCachedMessages(model.id);

        if (Utils.getInstance().isNetworkConnected()) {
            loadMessageRoomServer(mRoom);
        } else if (getView() != null && mMessagesArr.size() > 0) {
            Toast.makeText(getActivity(), R.string.no_network, Toast.LENGTH_SHORT).show();
        }
    }

    public void onEvent(UpdateMessageEvent message) {
        MessageModel newMessage = message.getMessageModel();

        if (newMessage != null) {
            mPresenter.updateMessages(mRoom.id, newMessage.id, newMessage.text);
        }
    }

    public void onEvent(NewMessageEvent message) {
        mPresenter.insertMessageToDb(message.getMessage(), message.getRoom().id);

        if (!message.getRoom().id.equals(mRoom.id)) {
            return;
        }

        if (mMessagesAdapter != null) {
            for (int i = 0; i < mMessagesArr.size(); i++) { // If updated message or send message
                MessageModel item = mMessagesArr.get(i);

                // Send message
                if (message.getMessage().text.equals(item.text) &&
                        item.sent.equals(StatusMessage.SENDING.name())) {
                    return;
                }

                // Update message
                if (!item.sent.equals(StatusMessage.NO_SEND.name())
                        && !item.sent.equals(StatusMessage.SENDING.name())
                        && item.id.equals(message.getMessage().id)) {
                    return;
                }
            }

            mMessagesArr.add(message.getMessage());
            mMessagesAdapter.notifyItemInserted(mMessagesArr.size() - 1);

            if (mListLayoutManager.findLastVisibleItemPosition() == mMessagesArr.size() - 2) {
                mMessagesList.smoothScrollToPosition(mMessagesArr.size() - 1);
            }
        }
    }

    @Override
    public void showMessages(ArrayList<MessageModel> messages) {
        mMessagesArr.clear();
        mMessagesArr.addAll(messages);

        if (mSwipeLoadLayout.isRefreshing()) {
            mMessagesAdapter.notifyDataSetChanged();
            mSwipeLoadLayout.setRefreshing(false);
        } else {
            mMessagesAdapter.notifyDataSetChanged();

            if (mMessageListSavedState != null) {
                mMessagesList.getLayoutManager().onRestoreInstanceState(mMessageListSavedState);
                mMessageListSavedState = null;
            } else if (mListLayoutManager.findLastCompletelyVisibleItemPosition() != mMessagesArr.size() - 1) { // If room just was loaded
                mMessagesList.scrollToPosition(mMessagesArr.size() - 1);
            }
        }
    }

    @Override
    public void showError(String error) {
        hideListProgress();

        if (mSwipeLoadLayout.isRefreshing()) {
            mSwipeLoadLayout.setRefreshing(false);
        }

        isRefreshing = false;

        if (error.contains("401")) {
            getActivity().sendBroadcast(new Intent(MainActivity.BROADCAST_UNAUTHORIZED));
        }

        Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void successUpdate(MessageModel message) {
        for (int i = 0; i < mMessagesArr.size(); i++) {
            MessageModel item = mMessagesArr.get(i);
            // Update message
            if (!item.sent.equals(StatusMessage.NO_SEND.name())
                    && !item.sent.equals(StatusMessage.SENDING.name())
                    && item.id.equals(message.id)) {
                mMessagesArr.set(i, message);
                mMessagesAdapter.setMessage(i, message);
                mMessagesAdapter.notifyItemChanged(i);
            }
        }

        Toast.makeText(getActivity(), R.string.updated, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void successRead(int first, int last, String roomId, int countRead) {
        if (roomId.equals(mRoom.id)) {
            for (int i = first; i <= last; i++) {
                if (mMessagesAdapter.getItemViewType(i) == 0) {
                    MessagesAdapter.DynamicViewHolder holder =
                            (MessagesAdapter.DynamicViewHolder) mMessagesList.findViewHolderForAdapterPosition(i);
                    if (holder != null) {
                        mMessagesAdapter.read(holder.newMessageIndicator, i);
                        mMessagesArr.get(i).unread = false;
                    }
                } else {
                    MessagesAdapter.StaticViewHolder holder =
                            (MessagesAdapter.StaticViewHolder) mMessagesList.findViewHolderForAdapterPosition(i);
                    if (holder != null) {
                        mMessagesAdapter.read(holder.newMessageIndicator, i);
                        mMessagesArr.get(i).unread = false;
                    }
                }
            }
        }

        // Send notification to MainActivity
        ReadMessagesEvent readMessagesEventBus = new ReadMessagesEvent();

        // -1 but last item equals to empty string
        readMessagesEventBus.setCountRead(countRead);
        EventBus.getDefault().post(readMessagesEventBus);
    }

    @Override
    public void successLoadBeforeId(ArrayList<MessageModel> messages) {
        if (messages.size() > 0) {
            mMessagesArr.addAll(0, messages);
            mMessagesAdapter.notifyItemRangeInserted(0, messages.size());

            countLoadMessages += messages.size();
        }

        mSwipeLoadLayout.setRefreshing(false);
    }

    @Override
    public void deliveredMessage(MessageModel message) {
        for (int i = 0; i < mMessagesArr.size(); i++) {
            if (mMessagesArr.get(i).sent.equals(StatusMessage.SENDING.name()) &&
                    mMessagesArr.get(i).text.equals(message.text)) {
                mMessagesArr.set(i, message);
                mMessagesAdapter.notifyItemChanged(i);

                if (mListLayoutManager.findLastVisibleItemPosition() == mMessagesArr.size() - 2) {
                    mMessagesList.scrollToPosition(mMessagesArr.size() - 1);
                }
            }
        }
    }

    @Override
    public void errorDeliveredMessage() {
        Toast.makeText(getActivity(), R.string.error_send, Toast.LENGTH_SHORT).show();

        for (int i = 0; i < mMessagesArr.size(); i++) {
            if (mMessagesArr.get(i).sent.equals(StatusMessage.SENDING.name())) {
                mMessagesArr.get(i).sent = StatusMessage.NO_SEND.name();
                mMessagesAdapter.notifyItemChanged(i);
            }
        }
    }

    @Override
    public void showListProgress() {
        mSwipeLoadLayout.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);

        isRefreshing = true;
    }

    @Override
    public void hideListProgress() {
        if (mSwipeLoadLayout.getVisibility() != View.VISIBLE) {
            mSwipeLoadLayout.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
        }

        isRefreshing = false;
    }

    @Override
    public void onDestroyView() {
        mPresenter.unbindView();
        super.onDestroyView();
    }

    @Override
    protected void initDiComponent() {
        mComponent = DaggerChatRoomComponent.builder()
                .applicationComponent(getAppComponent())
                .chatRoomPresenterModule(new ChatRoomPresenterModule())
                .build();

        mComponent.inject(this);
    }

    @Override
    public Context getAppContext() {
        return getActivity();
    }

    @Override
    public void onRefresh() {
        mSwipeLoadLayout.postDelayed(() -> {
            // Message not sent or sending, it hasn't id
            if (mMessagesArr.size() > 0 && !mMessagesArr.get(mMessagesArr.size() - 1).id.isEmpty()) {
                mPresenter.loadMessagesBeforeId(mRoom.id, 10, mMessagesArr.get(0).id);
            } else {
                mSwipeLoadLayout.setRefreshing(false);
            }
        }, 2000);
    }

    // Callback for adapter
    public interface ReadMessageCallback {
        void read(ImageView indicator, int position);
    }
}
