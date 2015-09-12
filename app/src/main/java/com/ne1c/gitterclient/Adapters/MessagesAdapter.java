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
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.ne1c.gitterclient.Fragments.EditMessageFragment;
import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.Models.UserModel;
import com.ne1c.gitterclient.R;
import com.ne1c.gitterclient.RetrofitServices.IApiMethods;
import com.ne1c.gitterclient.Utils;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

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

        if (message.urls != null) {
            holder.parentLayout.setOnLongClickListener(setParentLayoutLongClick(message));
        }

        holder.parentLayout.setOnClickListener(setParentLayoutClick(message));

        if (message.unread) {
            holder.newMessageIndicator.setImageResource(R.color.unreadMessage);
            holder.newMessageIndicator
                    .animate().alpha(0f).setDuration(1000).withLayer();
            message.unread = true;
        }

        if (!TextUtils.isEmpty(message.fromUser.username)) {
            holder.nicknameText.setText(message.fromUser.username);
        } else {
            holder.nicknameText.setText(message.fromUser.displayName);
        }

        if (TextUtils.isEmpty(message.text)) {
            Spannable span = new SpannableString("This message was deleted");
            span.setSpan(new StyleSpan(Typeface.ITALIC), 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.messageText.setText(span);
        } else {
            holder.messageText.setText(message.text);
        }

        holder.avatarImage.setOnClickListener(setAvatarImageClick(message));
        ImageLoader.getInstance().displayImage(message.fromUser.avatarUrlSmall, holder.avatarImage);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(formatter.parse(message.sent));
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minutes = calendar.get(Calendar.MINUTE);
            String time = String.format("%02d:%02d", hour, minutes);
            holder.timeText.setText(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (mMessages.get(position).fromUser.username.equals(mUserModel.username)) {
            holder.messageMenu.setVisibility(View.VISIBLE);
        } else {
            holder.messageMenu.setVisibility(View.INVISIBLE);
        }

        holder.messageMenu.setOnClickListener(setMenuClick(message));
    }

    private View.OnClickListener setParentLayoutClick(final MessageModel message) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessageEditText.setText(mMessageEditText.getText() + "@" + message.fromUser.username + " ");
            }
        };
    }

    private View.OnClickListener setAvatarImageClick(final MessageModel message) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(Utils.getInstance().GITHUB_URL + message.fromUser.url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        };
    }

    private View.OnLongClickListener setParentLayoutLongClick(final MessageModel message) {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                v.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
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
                });
                v.showContextMenu();
                return true;
            }
        };
    }

    private View.OnClickListener setMenuClick(final MessageModel message) {
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout parentLayout;
        public ImageView avatarImage;
        public ImageView newMessageIndicator;
        public ImageView messageMenu;
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
        }
    }
}
