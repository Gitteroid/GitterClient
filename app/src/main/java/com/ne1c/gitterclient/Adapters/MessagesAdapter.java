package com.ne1c.gitterclient.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.R;
import com.ne1c.gitterclient.Utils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    private ArrayList<MessageModel> mMessages;
    private Context mContext;
    private EditText mMessageEditText;

    public MessagesAdapter(Context context, ArrayList<MessageModel> messages, EditText editText) {
        mContext = context;
        mMessages = messages;
        mMessageEditText = editText;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_chat_message, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MessageModel messsage = mMessages.get(position);

        holder.parentLayout.setOnClickListener(setParentLayoutClick(messsage));

        if (messsage.unread) {
            holder.newMessageIndicator.setImageResource(R.color.unreadMessage);
            holder.newMessageIndicator
                    .animate().alpha(0f).setDuration(1000).withLayer();
            messsage.unread = true;
        }

        if (!TextUtils.isEmpty(messsage.fromUser.username)) {
            holder.nicknameText.setText(messsage.fromUser.username);
        } else {
            holder.nicknameText.setText(messsage.fromUser.displayName);
        }

        holder.messageText.setText(mMessages.get(position).text);

        holder.avatarImage.setOnClickListener(setAvatarImageClick(messsage));
        ImageLoader.getInstance().displayImage(messsage.fromUser.avatarUrlSmall, holder.avatarImage);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(formatter.parse(messsage.sent));
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minutes = calendar.get(Calendar.MINUTE);
            String time = String.format("%02d:%02d", hour, minutes);
            holder.timeText.setText(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
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
                mContext.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(Utils.getInstance().GITHUB_URL + message.fromUser.url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        };
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public LinearLayout parentLayout;
        public ImageView avatarImage;
        public ImageView newMessageIndicator;
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
        }
    }
}
