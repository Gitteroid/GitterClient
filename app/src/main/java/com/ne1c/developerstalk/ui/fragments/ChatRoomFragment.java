package com.ne1c.developerstalk.ui.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ne1c.developerstalk.R;
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

import de.greenrobot.event.EventBus;
import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import jp.wasabeef.recyclerview.animators.ScaleInBottomAnimator;

public class ChatRoomFragment extends Fragment implements MainActivity.RefreshRoomCallback, ChatView {
    private EditText mMessageEditText;
    private ImageButton mSendButton;
    private RecyclerView mMessagesList;
    private LinearLayoutManager mListLayoutManager;
    private MessagesAdapter mMessagesAdapter;
    private ProgressBar mProgressBar;

    private PtrClassicFrameLayout mPtrFrameLayout;

    private ArrayList<MessageModel> mMessagesArr = new ArrayList<>();
    private RoomModel mRoom;

    private ChatRoomPresenter mPresenter;

    private int startNumberLoadMessages = 10;
    private int countLoadMessages = 0;
    private boolean isRefreshing = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        EventBus.getDefault().register(this);

        mPresenter = new ChatRoomPresenter();

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
        ScaleInBottomAnimator anim = new ScaleInBottomAnimator(new OvershootInterpolator(1f));
        anim.setAddDuration(500);
        anim.setChangeDuration(0);
        mMessagesList.setItemAnimator(anim);

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

        mPtrFrameLayout.setPtrHandler(new PtrHandler() {
            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout ptrFrameLayout, View view, View view1) {
                return PtrDefaultHandler.checkContentCanBePulledDown(ptrFrameLayout, view, view1);
            }

            @Override
            public void onRefreshBegin(PtrFrameLayout ptrFrameLayout) {
                // Message not sent or sending, it hasn't id
                if (mMessagesArr.size() > 0 && !mMessagesArr.get(mMessagesArr.size() - 1).id.isEmpty()) {
                    mPresenter.loadMessagesBeforeId(mRoom.id, 10, mMessagesArr.get(0).id);
                } else {
                    mPtrFrameLayout.refreshComplete();
                }
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
        EventBus.getDefault().unregister(this);

        mPresenter.unbindView();
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

    private void loadMessageRoomServer(final RoomModel roomModel, final boolean showProgressBar, final boolean refresh) {
        mMessagesAdapter.setRoom(roomModel);

        isRefreshing = showProgressBar;
        if (showProgressBar) {
            mPtrFrameLayout.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        mPresenter.loadMessages(roomModel.id, startNumberLoadMessages + countLoadMessages, showProgressBar, refresh);
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
        mPresenter.loadCachedMessages(model.id);
        loadMessageRoomServer(mRoom, false, false);
    }

    public void onEvent(UpdateMessageEvent message) {
        MessageModel newMessage = message.getMessageModel();

        if (newMessage != null) {
            mPresenter.updateMessages(mRoom.id, newMessage.id, newMessage.text);
        }
    }

    public void onEvent(NewMessageEvent message) {
        mPresenter.insertMessageToDb(message.getMessage(), mRoom.id);

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

            markMessagesAsRead(mMessagesList);
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

    @Override
    public void showMessages(ArrayList<MessageModel> messages, boolean showProgress, boolean showRefresh) {
        mMessagesArr.clear();
        mMessagesArr.addAll(messages);

        if (mPtrFrameLayout.isRefreshing()) {
            mMessagesAdapter.notifyDataSetChanged();
            mPtrFrameLayout.refreshComplete();
        } else {
            mMessagesAdapter.notifyDataSetChanged();

            // If room just was loaded
            if (mListLayoutManager.findLastCompletelyVisibleItemPosition() != mMessagesArr.size() - 1) {
                mMessagesList.scrollToPosition(mMessagesArr.size() - 1);
            }
        }

        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mPtrFrameLayout.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);

            isRefreshing = false;
        }
    }

    @Override
    public void showError(String error) {
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.INVISIBLE);
        } else if (mPtrFrameLayout.isRefreshing()) {
            mPtrFrameLayout.refreshComplete();
        }
        isRefreshing = false;
        mPtrFrameLayout.setVisibility(View.VISIBLE);

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

        mPtrFrameLayout.refreshComplete();
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
    public Context getAppContext() {
        return getActivity();
    }

    @Override
    public void onDestroyView() {
        mPresenter.unbindView();
        super.onDestroyView();
    }

    // Callback for adapter
    public interface ReadMessageCallback {
        void read(ImageView indicator, int position);
    }
}
