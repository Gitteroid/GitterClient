package com.ne1c.gitterclient.Adapters;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.ne1c.gitterclient.Fragments.ChatRoomFragment;
import com.ne1c.gitterclient.Fragments.EditMessageFragment;
import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.Models.StatusMessage;
import com.ne1c.gitterclient.Models.UserModel;
import com.ne1c.gitterclient.R;
import com.ne1c.gitterclient.RetrofitServices.IApiMethods;
import com.ne1c.gitterclient.Services.NewMessagesService;
import com.ne1c.gitterclient.Utils;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> implements
        ChatRoomFragment.ReadMessageCallback {
    private RoomModel mRoom;
    private ArrayList<MessageModel> mMessages;
    private Activity mActivity;
    private EditText mMessageEditText;
    private UserModel mUserModel;
    private IApiMethods mApiMethods;

    public MessagesAdapter(Activity activity, ArrayList<MessageModel> messages, EditText editText) {
        mActivity = activity;
        mMessages = messages;
        mMessageEditText = editText;
        mUserModel = Utils.getInstance().getUserPref();
        mApiMethods = new RestAdapter
                .Builder()
                .setEndpoint(Utils.getInstance().GITTER_API_URL)
                .build()
                .create(IApiMethods.class);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mActivity).inflate(R.layout.item_chat_message, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        MessageModel message = mMessages.get(position);

        if (message.urls.size() > 0) {
            holder.parentLayout.setOnLongClickListener(setParentLayoutLongClick(message, false));
        } else if (message.sent.equals(StatusMessage.NO_SEND.name())) {
            holder.parentLayout.setOnLongClickListener(setParentLayoutLongClick(message, true));
        }

        holder.parentLayout.setOnClickListener(getParentLayoutClick(message));
        holder.avatarImage.setOnClickListener(getAvatarImageClick(message));
        holder.messageMenu.setOnClickListener(getMenuClick(message));

        processingIndicator(holder.newMessageIndicator, message);
        makeDeletedMessageText(holder.messageText, message);

        holder.timeText.setText(getTimeMessage(message));
        holder.nicknameText.setText(getUsername(message));
        //noinspection ResourceType
        holder.messageMenu.setVisibility(getVisibilityMenu(message));
        setIconMessage(holder.statusMessage, message);

        ImageLoader.getInstance().displayImage(message.fromUser.avatarUrlSmall, holder.avatarImage);
    }

    private void makeDeletedMessageText(TextView text, MessageModel message) {
        if (TextUtils.isEmpty(message.text)) {
            Spannable span = new SpannableString("This message was deleted");
            span.setSpan(new StyleSpan(Typeface.ITALIC), 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setText(span);
        } else {
            text.setText(message.text);
        }
    }

    private int getVisibilityMenu(MessageModel message) {
        if (message.fromUser.id.equals(mUserModel.id)) {
            return View.VISIBLE;
        } else {
            return View.GONE;
        }
    }

    private String getUsername(MessageModel message) {
        if (!TextUtils.isEmpty(message.fromUser.username)) {
            return message.fromUser.username;
        } else {
            return message.fromUser.displayName;
        }
    }

    private void processingIndicator(ImageView indicator, MessageModel message) {
//        if (message.unread) {
//            indicator.setImageResource(R.color.unreadMessage);
//        }
    }

    private String getTimeMessage(MessageModel message) {
        String time = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            long hourOffset =  TimeUnit.HOURS.convert(TimeZone.getDefault().getRawOffset(), TimeUnit.MILLISECONDS);

            calendar.setTime(formatter.parse(message.sent));
            long hour = calendar.get(Calendar.HOUR_OF_DAY) + hourOffset;
            int minutes = calendar.get(Calendar.MINUTE);

            if (hour >= 24) {
                hour -= 24;
            }

            time = String.format("%02d:%02d", hour, minutes);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return time;
    }

    private void setIconMessage(ImageView statusMessage, MessageModel message) {
        if (message.fromUser.id.equals(mUserModel.id)) {
            if (statusMessage.getVisibility() == View.INVISIBLE) {
                statusMessage.setVisibility(View.VISIBLE);
            }

            if (!message.sent.equals(StatusMessage.NO_SEND.name())
                    && !message.sent.equals(StatusMessage.SENDING.name())) {
                statusMessage.setImageResource(R.mipmap.ic_deliver_mess);
            } else if (message.sent.equals(StatusMessage.NO_SEND.name())) {
                statusMessage.setImageResource(R.mipmap.ic_error_mess);
            } else if (message.sent.equals(StatusMessage.SENDING.name())) {
                statusMessage.setImageResource(R.mipmap.ic_sending_mess);
            }
        } else {
            statusMessage.setVisibility(View.INVISIBLE);
        }
    }

    private View.OnClickListener getParentLayoutClick(final MessageModel message) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessageEditText.append("@" + message.fromUser.username + " ");
            }
        };
    }

    private View.OnClickListener getAvatarImageClick(final MessageModel message) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(Utils.getInstance().GITHUB_URL + "/" + message.fromUser.username))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        };
    }

    private View.OnLongClickListener setParentLayoutLongClick(final MessageModel message, final boolean repeatSend) {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                v.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                        if (repeatSend) {
                            menu.add(R.string.retry_send);
                            menu.getItem(0).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    mActivity.sendBroadcast(new Intent(NewMessagesService.BROADCAST_SEND_MESSAGE)
                                            .putExtra(NewMessagesService.SEND_MESSAGE_EXTRA_KEY, mMessageEditText.getText().toString())
                                            .putExtra(NewMessagesService.TO_ROOM_MESSAGE_EXTRA_KEY, mRoom.id));
                                    return true;
                                }
                            });
                        } else {
                            for (int i = 0; i < message.urls.size(); i++) {
                                menu.add(message.urls.get(i).url);
                                menu.getItem(i).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.getTitle().toString())));
                                        return true;
                                    }
                                });
                            }
                        }
                    }
                });
                v.showContextMenu();
                return true;
            }
        };
    }

    private View.OnClickListener getMenuClick(final MessageModel message) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu menu = new PopupMenu(mActivity, v);
                menu.inflate(R.menu.menu_message);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        switch (item.getItemId()) {
                            case R.id.edit_message_menu:
                                EditMessageFragment fragment = new EditMessageFragment();
                                Bundle args = new Bundle();
                                args.putParcelable("message", message);
                                fragment.setArguments(args);
                                fragment.show(mActivity.getFragmentManager(), "dialogEdit");
                                return true;
                            case R.id.delete_message_menu:
                                mApiMethods.updateMessage(Utils.getInstance().getBearer(), mRoom.id, message.id, "",
                                        new Callback<MessageModel>() {
                                            @Override
                                            public void success(MessageModel model, Response response) {
                                                Toast.makeText(mActivity, "Deleted", Toast.LENGTH_SHORT).show();
                                            }

                                            @Override
                                            public void failure(RetrofitError error) {
                                                Toast.makeText(mActivity, "Deleted error", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                return true;
                            default:
                                return false;
                        }
                    }
                });

                menu.show();
            }
        };
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public void setRoom(RoomModel model) {
        mRoom = model;
    }

    @Override
    public void read(ImageView indicator, int position) {
        indicator.animate().alpha(0f).setDuration(2000).withLayer();
        mMessages.get(position).unread = false;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout parentLayout;
        public ImageView avatarImage;
        public ImageView newMessageIndicator;
        public ImageView messageMenu;
        public ImageView statusMessage;
        public TextView nicknameText;
        public TextView messageText;
        public TextView timeText;

        public ViewHolder(View itemView) {
            super(itemView);

            parentLayout = (LinearLayout) itemView.findViewById(R.id.parent_layout);
            avatarImage = (ImageView) itemView.findViewById(R.id.avatar_image);
            newMessageIndicator = (ImageView) itemView.findViewById(R.id.new_message_image);
            nicknameText = (TextView) itemView.findViewById(R.id.nickname_text);
            messageText = (TextView) itemView.findViewById(R.id.message_text);
            timeText = (TextView) itemView.findViewById(R.id.time_text);
            messageMenu = (ImageView) itemView.findViewById(R.id.overflow_message_menu);
            statusMessage = (ImageView) itemView.findViewById(R.id.status_mess_image);
        }
    }
}
