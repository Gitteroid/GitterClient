package com.ne1c.developerstalk.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.di.components.ChatRoomComponent;
import com.ne1c.developerstalk.di.components.DaggerChatRoomComponent;
import com.ne1c.developerstalk.di.modules.ChatRoomPresenterModule;
import com.ne1c.developerstalk.events.NewMessageEvent;
import com.ne1c.developerstalk.events.ReadMessagesEvent;
import com.ne1c.developerstalk.events.RefreshMessagesRoomEvent;
import com.ne1c.developerstalk.events.UpdateMessageEvent;
import com.ne1c.developerstalk.models.data.MessageModel;
import com.ne1c.developerstalk.models.data.RoomModel;
import com.ne1c.developerstalk.models.data.StatusMessage;
import com.ne1c.developerstalk.models.view.MessageViewModel;
import com.ne1c.developerstalk.models.view.RoomViewModel;
import com.ne1c.developerstalk.presenters.ChatRoomPresenter;
import com.ne1c.developerstalk.ui.adapters.MessagesAdapter;
import com.ne1c.developerstalk.ui.views.ChatView;
import com.ne1c.developerstalk.utils.MarkdownUtils;
import com.ne1c.developerstalk.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class ChatRoomFragment extends BaseFragment implements ChatView {
    private EditText mMessageEditText;
    private ImageButton mSendButton;
    private RecyclerView mMessagesList;
    private LinearLayoutManager mListLayoutManager;
    private MessagesAdapter mMessagesAdapter;
    private ProgressBar mProgressBar;
    private MaterialProgressBar mTopProgressBar;
    private FloatingActionButton mFabToBottom;

    private ArrayList<MessageViewModel> mMessagesArr = new ArrayList<>();
    private RoomViewModel mRoom;

    private Parcelable mMessageListSavedState;

    private ChatRoomComponent mComponent;

    @Inject
    ChatRoomPresenter mPresenter;

    private int mStartNumberLoadMessages = 10;
    private int mCountLoadMessages = 0;

    private boolean mIsLoadBeforeIdMessages = false;
    private boolean mIsRefreshing = false;

    private RoomViewModel mOverviewRoom = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        mStartNumberLoadMessages = Integer.valueOf(prefs.getString("number_load_mess", "10"));

        if (getArguments() != null) {
            mOverviewRoom = getArguments().getParcelable("overviewRoom");
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat_room, container, false);

        mMessageEditText = (EditText) v.findViewById(R.id.message_edit_text);
        mSendButton = (ImageButton) v.findViewById(R.id.send_button);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progress_bar);
        mProgressBar.setIndeterminate(true);

        mTopProgressBar = (MaterialProgressBar) v.findViewById(R.id.top_progress_bar);
        mTopProgressBar.setUseIntrinsicPadding(false);

        mMessagesList = (RecyclerView) v.findViewById(R.id.messages_list);
        mListLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, true);
        mMessagesList.setLayoutManager(mListLayoutManager);
        mMessagesList.setItemViewCacheSize(50);
        mMessagesList.setItemAnimator(new DefaultItemAnimator());
        mMessagesList.getItemAnimator().setAddDuration(300);

        mFabToBottom = (FloatingActionButton) v.findViewById(R.id.fab_to_bottom);
        mFabToBottom.setOnClickListener(v1 -> mMessagesList.smoothScrollToPosition(0));
        mFabToBottom.hide();

        setDataToView(savedInstanceState);

        v.findViewById(R.id.markdown_button).setOnClickListener(v1 -> {
            DialogMarkdownFragment dialog = new DialogMarkdownFragment();
            dialog.setTargetFragment(ChatRoomFragment.this, DialogMarkdownFragment.REQUEST_CODE);
            dialog.show(getFragmentManager(), "MARKDOWN_DIALOG");
        });

        mPresenter.bindView(this);

        if (mOverviewRoom != null) {
            v.findViewById(R.id.input_layout).setVisibility(View.GONE);

            onEvent(mOverviewRoom);

            Button joinRoomButton = (Button) v.findViewById(R.id.join_room_button);
            joinRoomButton.setVisibility(View.VISIBLE);
            joinRoomButton.setOnClickListener(v1 -> mPresenter.joinToRoom(mOverviewRoom.name));
        }

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

            mMessageEditText.requestFocus();
            mMessageEditText.post(() -> {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            });
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
        mMessagesAdapter = new MessagesAdapter(getAppComponent().getDataManager(),
                getActivity(),
                mMessagesArr,
                mMessageEditText);

        mMessagesList.setAdapter(mMessagesAdapter);
        mMessagesList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(final RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    markMessagesAsRead();

                    int firstVisible = mListLayoutManager.findFirstVisibleItemPosition();
                    if (firstVisible >= 10) {
                        mFabToBottom.show();
                    } else {
                        mFabToBottom.hide();
                    }
                }

                int lastMessage = mMessagesArr.size() - 1;

                if (mListLayoutManager.findLastVisibleItemPosition() == lastMessage) {
                    if (mMessagesArr.size() > 0 && !mMessagesArr.get(lastMessage).id.isEmpty()) {
                        if (!mIsLoadBeforeIdMessages && mTopProgressBar.getVisibility() != View.VISIBLE) {
                            mIsLoadBeforeIdMessages = true;

                            showTopProgressBar();
                            mPresenter.loadMessagesBeforeId(mRoom.id, 10, mMessagesArr.get(lastMessage).id);
                        }
                    } else {
                        hideTopProgressBar();
                    }
                }
            }
        });

        if (savedInstanceState != null) {
            mMessageListSavedState = savedInstanceState.getParcelable("scrollPosition");
            savedInstanceState.remove("scrollPosition");

            RoomViewModel room = savedInstanceState.getParcelable("active_room");
            savedInstanceState.remove("active_room");

            if (room != null) {
                mRoom = room;
                mMessagesAdapter.setRoom(mRoom);
            }

            ArrayList<MessageModel> messages = savedInstanceState.getParcelableArrayList("messages");
            savedInstanceState.remove("messages");

//            if (messages != null && messages.size() > 0) {
//                mMessagesArr.clear();
//                mMessagesArr.addAll(messages);
//
//                mMessagesAdapter.notifyDataSetChanged();

            if (mMessageListSavedState != null) {
                mMessagesList.getLayoutManager().onRestoreInstanceState(mMessageListSavedState);
                mMessageListSavedState = null;
            }
            //}
        }

        if (mIsRefreshing) {
            mMessagesList.setVisibility(View.GONE);
            showListProgressBar();
        }

        if (mIsLoadBeforeIdMessages) {
            showTopProgressBar();
        }

        mSendButton.setOnClickListener(v -> {
            if (!mMessageEditText.getText().toString().isEmpty()) {
                if (Utils.getInstance().isNetworkConnected()) {
                    MessageViewModel model = mPresenter.createSendMessage(mMessageEditText.getText().toString());

                    mMessagesArr.add(0, model);
                    mMessagesAdapter.notifyItemInserted(0);

                    int first = mListLayoutManager.findFirstVisibleItemPosition();
                    if (first != 1) {
                        mMessagesList.smoothScrollToPosition(0);
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
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);

        mPresenter.unbindView();
        mComponent = null;

        super.onDestroy();
    }

    private void markMessagesAsRead() {
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

    private void loadMessageRoomServer(final RoomViewModel roomModel) {
        mMessagesAdapter.setRoom(roomModel);

        mPresenter.loadNetworkMessages(roomModel.id, mStartNumberLoadMessages + mCountLoadMessages);
    }

    // Event from MainActivity or notification
    // Load messages of room
    public void onEvent(RoomViewModel model) {
        hideListProgress();
        hideTopProgressBar();

        mCountLoadMessages = 0;

        mMessagesArr.clear();
        mMessagesAdapter.notifyDataSetChanged();

        mMessagesAdapter.setRoom(model);

        if (!Utils.getInstance().isNetworkConnected() && getView() != null) {
            Toast.makeText(getActivity(), R.string.no_network, Toast.LENGTH_SHORT).show();
        }

        mIsRefreshing = false;
        mIsLoadBeforeIdMessages = false;

        mPresenter.loadMessages(model.id, mStartNumberLoadMessages);

        mRoom = model;
    }

    public void onEvent(RefreshMessagesRoomEvent room) {
        if (!Utils.getInstance().isNetworkConnected() && getView() != null) {
            Toast.makeText(getActivity(), R.string.no_network, Toast.LENGTH_SHORT).show();
        } else {
            mIsRefreshing = true;
            loadMessageRoomServer(room.getRoomModel());
        }
    }

    public void onEvent(UpdateMessageEvent message) {
        MessageModel newMessage = message.getMessageModel();

        if (newMessage != null) {
            mPresenter.updateMessages(mRoom.id, newMessage.id, newMessage.text);
        }
    }

    public void onEvent(NewMessageEvent message) {
        if (!message.getRoom().id.equals(mRoom.id)) {
            return;
        }

        if (mMessagesAdapter != null) {
            for (int i = 0; i < mMessagesArr.size(); i++) { // If updated message or send message
                MessageViewModel item = mMessagesArr.get(i);

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

            mMessagesArr.add(0, message.getMessage());
            mMessagesAdapter.notifyItemInserted(0);

            int firstVisible = mListLayoutManager.findFirstVisibleItemPosition();
            if (firstVisible > 1 && firstVisible <= 5) {
                mMessagesList.smoothScrollToPosition(0);
            }
        }
    }

    @Override
    public void showMessages(ArrayList<MessageViewModel> messages) {
        Collections.reverse(messages);

        mMessagesArr.clear();
        mMessagesArr.addAll(messages);

        mMessagesAdapter.notifyDataSetChanged();

        if (mMessageListSavedState != null) {
            mMessagesList.getLayoutManager().onRestoreInstanceState(mMessageListSavedState);
            mMessageListSavedState = null;
        } else if (mListLayoutManager.findLastCompletelyVisibleItemPosition() != 0) { // If room just was loaded
            mMessagesList.scrollToPosition(0);
        }

        mIsRefreshing = false;
    }

    @Override
    public void showError(int resId) {
        hideListProgress();
        hideTopProgressBar();

        mIsRefreshing = false;
        mIsLoadBeforeIdMessages = false;

//        if (error.contains("401")) {
//            getActivity().sendBroadcast(new Intent(MainActivity.BROADCAST_UNAUTHORIZED));
//        }

        Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showUpdateMessage(MessageViewModel message) {
        for (int i = 0; i < mMessagesArr.size(); i++) {
            MessageViewModel item = mMessagesArr.get(i);
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
    public void successReadMessages(int first, int last, String roomId, int countRead) {
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
    public void showLoadBeforeIdMessages(ArrayList<MessageViewModel> messages) {
        if (messages.size() > 0 && !mIsRefreshing) {
            Collections.reverse(messages);

            mMessagesArr.addAll(mMessagesArr.size(), messages);
            mMessagesAdapter.notifyItemRangeInserted(mMessagesArr.size(), messages.size());

            mCountLoadMessages += messages.size();
        }

        hideTopProgressBar();
        mIsLoadBeforeIdMessages = false;
    }

    @Override
    public void deliveredMessage(MessageViewModel message) {
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
    public void showTopProgressBar() {
        if (mTopProgressBar.getVisibility() != View.VISIBLE) {
            mTopProgressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideTopProgressBar() {
        if (mTopProgressBar.getVisibility() == View.VISIBLE) {
            mTopProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void showListProgressBar() {
        hideTopProgressBar();

        mMessagesList.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);

        mIsRefreshing = true;
    }

    @Override
    public void hideListProgress() {
        if (mMessagesList.getVisibility() != View.VISIBLE) {
            mMessagesList.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
        }

        mIsRefreshing = false;
    }

    @Override
    public void joinToRoom() {
        getActivity().onBackPressed();
    }

    @Override
    protected void initDiComponent() {
        mComponent = DaggerChatRoomComponent.builder()
                .applicationComponent(getAppComponent())
                .chatRoomPresenterModule(new ChatRoomPresenterModule())
                .build();

        mComponent.inject(this);
    }

    public static ChatRoomFragment newInstance(RoomModel overviewRoom) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("overviewRoom", overviewRoom);

        ChatRoomFragment fragment = new ChatRoomFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    // Callback for adapter
    public interface ReadMessageCallback {
        void read(ImageView indicator, int position);
    }
}
