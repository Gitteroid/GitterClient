package com.ne1c.developerstalk.Adapters;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
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

import com.bumptech.glide.Glide;
import com.ne1c.developerstalk.Fragments.ChatRoomFragment;
import com.ne1c.developerstalk.Fragments.EditMessageFragment;
import com.ne1c.developerstalk.Fragments.LinksDialogFragment;
import com.ne1c.developerstalk.Models.MessageModel;
import com.ne1c.developerstalk.Models.RoomModel;
import com.ne1c.developerstalk.Models.StatusMessage;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.RetrofitServices.IApiMethods;
import com.ne1c.developerstalk.Services.NewMessagesService;
import com.ne1c.developerstalk.Util.MarkdownUtils;
import com.ne1c.developerstalk.Util.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
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
    private IApiMethods mApiMethods;

    public MessagesAdapter(Activity activity, ArrayList<MessageModel> messages, EditText editText) {
        mActivity = activity;
        mMessages = messages;
        mMessageEditText = editText;
        mApiMethods = new RestAdapter
                .Builder()
                .setEndpoint(Utils.GITTER_API_URL)
                .build()
                .create(IApiMethods.class);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mActivity).inflate(R.layout.item_chat_message, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MessageModel message = mMessages.get(position);

        holder.parentLayout.setOnClickListener(getParentLayoutClick(message));
        holder.avatarImage.setOnClickListener(getAvatarImageClick(message));
        holder.messageMenu.setOnClickListener(getMenuClick(message, position));

        processingIndicator(holder.newMessageIndicator, message);
        setMessageText(holder, message);

        holder.timeText.setText(getTimeMessage(message));
        holder.nicknameText.setText(getUsername(message));
        //noinspection ResourceType
        setIconMessage(holder.statusMessage, message);

        Glide.with(mActivity).load(message.fromUser.avatarUrlSmall).into(holder.avatarImage);
    }

    private void setMessageText(ViewHolder holder, MessageModel message) {
        holder.messageLayout.removeViews(1, holder.messageLayout.getChildCount() - 1);

        if (message.text.isEmpty()) {
            Spannable span = new SpannableString(mActivity.getString(R.string.message_deleted));
            span.setSpan(new StyleSpan(Typeface.ITALIC), 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            TextView textView = new MarkdownViews().getTextView();
            textView.setText(message.text);

            holder.messageLayout.addView(textView);
        } else {
            setMessageText(holder, message.text);
        }
    }

    private void setMessageText(ViewHolder holder, String text) {
        MarkdownUtils markdown = new MarkdownUtils(text);
        MarkdownViews views = new MarkdownViews();

        int counterSingleline = -1;
        int counterMultiline = -1;
        int counterBold = -1;
        int counterItalics = -1;
        int counterQuote = -1;
        int counterStrikethrough = -1;
        int counterIssue = -1;
        int counterLinks = -1;
        int counterImageLinks = -1;

        if (markdown.existMarkdown()) {
            for (int i = 0; i < markdown.getParsedString().size(); i++) {
                switch (markdown.getParsedString().get(i)) {
                    case "{0}":
                        TextView singleline = views.getSignlelineCodeView();
                        singleline.setText(markdown.getSinglelineCode().get(++counterSingleline));
                        holder.messageLayout.addView(singleline);

                        break;
                    case "{1}":
                        TextView multiline = views.getMultilineCodeView();
                        multiline.setText(markdown.getMultilineCode().get(++counterMultiline));
                        holder.messageLayout.addView(multiline);

                        break;
                    case "{2}":
                        if (holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1) instanceof TextView) {
                            ((TextView) holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1))
                                    .append(views.getBoldSpannableText(markdown.getBold().get(++counterBold)));
                        } else {
                            TextView textView = views.getTextView();
                            textView.setText(views.getBoldSpannableText(markdown.getBold().get(++counterBold)));
                            holder.messageLayout.addView(textView);
                        }

                        break;
                    case "{3}":
                        if (holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1) instanceof TextView) {
                            ((TextView) holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1))
                                    .append(views.getItalicSpannableText(markdown.getItalics().get(++counterItalics)));
                        } else {
                            TextView textView = views.getTextView();
                            textView.setText(views.getItalicSpannableText(markdown.getItalics().get(++counterItalics)));
                            holder.messageLayout.addView(textView);
                        }

                        break;
                    case "{4}":
                        if (holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1) instanceof TextView) {
                            ((TextView) holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1))
                                    .append(views.getStrikethroughSpannableText(markdown.getStrikethrough().get(++counterStrikethrough)));
                        } else {
                            TextView textView = views.getTextView();
                            textView.setText(views.getStrikethroughSpannableText(markdown.getStrikethrough().get(++counterStrikethrough)));
                            holder.messageLayout.addView(textView);
                        }

                        break;
                    case "{5}":
                        LinearLayout quote = views.getQuoteText(markdown.getQuote().get(++counterQuote));
                        holder.messageLayout.addView(quote);

                        break;
                    case "{6}":
                        TextView issue = views.getTextView();
                        issue.setText(views.getIssueSpannableText(markdown.getIssues().get(++counterIssue)));
                        holder.messageLayout.addView(issue);

                        break;
                    case "{7}":
                        if (holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1) instanceof TextView) {
                            ((TextView) holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1))
                                    .append(views.getLinksSpannableText(markdown.getLinks().get(++counterLinks)));
                        } else {
                            TextView textView = views.getTextView();
                            String link = markdown.getLinks().get(++counterLinks);
                            link = link.substring(1, link.indexOf("]"));

                            textView.setText(views.getLinksSpannableText(link));
                            holder.messageLayout.addView(textView);
                        }
                        break;
                    case "{8}":
                        final String linkImage = markdown.getImageLinks().get(++counterImageLinks);

                        ImageView image = views.getLinkImage();
                        image.setScaleType(ImageView.ScaleType.CENTER);

                        image.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(linkImage)));
                            }
                        });

                        //linkImage = linkImage.substring(linkImage.indexOf("http"), linkImage.length() - 2);
                        Glide.with(mActivity).load(linkImage).into(image);

                        holder.messageLayout.addView(image);

//                        ViewGroup.LayoutParams params = image.getLayoutParams();
//                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
//                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
//                        image.setLayoutParams(params);

                        break;
                    default:
                        if (holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1) instanceof TextView) {
                            ((TextView) holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1))
                                    .append(markdown.getParsedString().get(i));
                        } else {
                            TextView textView = views.getTextView();
                            textView.setText(markdown.getParsedString().get(i));
                            holder.messageLayout.addView(textView);
                        }
                }
            }
        } else {
            TextView textView = views.getTextView();
            textView.setText(text);
            holder.messageLayout.addView(textView);
        }
    }

    private String getUsername(MessageModel message) {
        if (!message.fromUser.username.isEmpty()) {
            return message.fromUser.username;
        } else {
            return message.fromUser.displayName;
        }
    }

    private void processingIndicator(ImageView indicator, MessageModel message) {
        if (message.unread) {
            if (!message.fromUser.id.equals(Utils.getInstance().getUserPref().id)) {
                indicator.setImageResource(R.color.unreadMessage);
            }
        } else {
            indicator.setImageResource(android.R.color.transparent);
        }
    }

    private String getTimeMessage(MessageModel message) {
        String time = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.setTimeInMillis(System.currentTimeMillis());

        try {
            // GMT TimeZone offset
            long hourOffset = TimeUnit.HOURS.convert(nowCalendar.getTimeZone().getRawOffset(), TimeUnit.MILLISECONDS);

            calendar.setTime(formatter.parse(message.sent));
            long hour = calendar.get(Calendar.HOUR_OF_DAY) + hourOffset;
            int minutes = calendar.get(Calendar.MINUTE);
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Example: 26:31 hours, output 02:31
            if (hour >= 24) {
                hour -= 24;
            }

            if (year != nowCalendar.get(Calendar.YEAR)) {
                time = String.format("%02d.%02d.%d", day, month, year);
            } else if (day != nowCalendar.get(Calendar.DAY_OF_MONTH) ||
                    month != nowCalendar.get(Calendar.MONTH)) {
                time = String.format("%02d.%02d", day, month);
            }

            // If time contains already day or year
            if (!time.isEmpty()) {
                time += String.format(" %02d:%02d", hour, minutes);
            } else {
                time = String.format("%02d:%02d", hour, minutes);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return time;
    }

    // Set icon status for message: send, sending or no send
    private void setIconMessage(ImageView statusMessage, MessageModel message) {
        if (message.fromUser.id.equals(Utils.getInstance().getUserPref().id)) {
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
                        Uri.parse(Utils.GITHUB_URL + "/" + message.fromUser.username))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        };
    }

    private View.OnClickListener getMenuClick(final MessageModel message, final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu menu = new PopupMenu(mActivity, v);
                if (message.fromUser.id.equals(Utils.getInstance().getUserPref().id)) {
                    menu.inflate(R.menu.menu_message_user);
                    showMenuUser(menu, message, position);
                } else {
                    menu.inflate(R.menu.menu_message_all);
                    showMenuAll(menu, message);
                }
            }
        };
    }

    private void showMenuUser(PopupMenu menu, final MessageModel message, final int position) {
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
                                        Toast.makeText(mActivity, R.string.deleted, Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void failure(RetrofitError error) {
                                        Toast.makeText(mActivity, R.string.deleted_error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                        return true;
                    case R.id.copy_text_menu:
                        Utils.getInstance().copyToClipboard(message.text);
                        return true;
                    case R.id.retry_send_menu:
                        if (Utils.getInstance().isNetworkConnected()) {
                            if (mMessages.get(position).sent.equals(StatusMessage.NO_SEND.name()) &&
                                    mMessages.get(position).text.equals(message.text)) {
                                // Update status message
                                mMessages.get(position).sent = StatusMessage.SENDING.name();

                                // Repeat send
                                mActivity.sendBroadcast(new Intent(NewMessagesService.BROADCAST_SEND_MESSAGE)
                                        .putExtra(NewMessagesService.SEND_MESSAGE_EXTRA_KEY, message.text)
                                        .putExtra(NewMessagesService.TO_ROOM_MESSAGE_EXTRA_KEY, mRoom.id));

                                notifyItemChanged(position);
                            }
                        } else {
                            Toast.makeText(mActivity, R.string.no_network, Toast.LENGTH_SHORT).show();
                        }

                        return true;
                    case R.id.links_menu:
                        LinksDialogFragment links = new LinksDialogFragment();
                        Bundle argsLinks = new Bundle();
                        argsLinks.putParcelableArrayList("links", new ArrayList<Parcelable>(message.urls));
                        links.setArguments(argsLinks);
                        links.show(mActivity.getFragmentManager(), "dialogLinks");

                        return true;
                    default:
                        return false;
                }
            }
        });

        if (message.urls.size() <= 0) {
            menu.getMenu().removeItem(R.id.links_menu);
        }

        if (!message.sent.equals(StatusMessage.NO_SEND.name())) {
            menu.getMenu().removeItem(R.id.retry_send_menu);
        }

        menu.show();
    }

    private void showMenuAll(final PopupMenu menu, final MessageModel message) {
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.copy_text_menu:
                        Utils.getInstance().copyToClipboard(message.text);
                        return true;
                    case R.id.links_menu:
                        LinksDialogFragment links = new LinksDialogFragment();
                        Bundle argsLinks = new Bundle();
                        argsLinks.putParcelableArrayList("links", new ArrayList<Parcelable>(message.urls));
                        links.setArguments(argsLinks);
                        links.show(mActivity.getFragmentManager(), "dialogLinks");

                        return true;
                    default:
                        return false;
                }
            }
        });

        if (message.urls.size() <= 0) {
            menu.getMenu().removeItem(R.id.links_menu);
        }

        menu.show();
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public void setRoom(RoomModel model) {
        mRoom = model;
    }

    // Use for set new message, because if used notifyItemChanged, then call draw
    // bad animation
    public void setMessage(int position, MessageModel message) {
        mMessages.set(position, message);
    }

    @Override
    public void read(ImageView indicator, int position) {
        indicator.animate().alpha(0f).setDuration(1000).withLayer();
        mMessages.get(position).unread = false;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout parentLayout;
        public LinearLayout messageLayout;
        public ImageView avatarImage;
        public ImageView newMessageIndicator;
        public ImageView messageMenu;
        public ImageView statusMessage;
        public TextView nicknameText;
        //public TextView messageText;
        public TextView timeText;

        public ViewHolder(View itemView) {
            super(itemView);

            parentLayout = (LinearLayout) itemView.findViewById(R.id.parent_layout);
            messageLayout = (LinearLayout) itemView.findViewById(R.id.message_layout);
            avatarImage = (ImageView) itemView.findViewById(R.id.avatar_image);
            newMessageIndicator = (ImageView) itemView.findViewById(R.id.new_message_image);
            nicknameText = (TextView) itemView.findViewById(R.id.nickname_text);
            //messageText = (TextView) itemView.findViewById(R.id.message_text);
            timeText = (TextView) itemView.findViewById(R.id.time_text);
            messageMenu = (ImageView) itemView.findViewById(R.id.overflow_message_menu);
            statusMessage = (ImageView) itemView.findViewById(R.id.status_mess_image);
        }
    }

    private class MarkdownViews {
        public TextView getSignlelineCodeView() {
            return (TextView) LayoutInflater.from(mActivity).inflate(R.layout.singleline_code_view, null);
        }

        public TextView getMultilineCodeView() {
            return (TextView) LayoutInflater.from(mActivity).inflate(R.layout.multiline_code_view, null);
        }

        public Spannable getBoldSpannableText(String text) {
            Spannable span = new SpannableString(text);
            span.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            return span;
        }

        public Spannable getItalicSpannableText(String text) {
            Spannable span = new SpannableString(text);
            span.setSpan(new StyleSpan(Typeface.ITALIC), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            return span;
        }

        public Spannable getStrikethroughSpannableText(String text) {
            Spannable span = new SpannableString(text);
            span.setSpan(new StyleSpan(new StrikethroughSpan().getSpanTypeId()), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            return span;
        }

        public LinearLayout getQuoteText(String text) {
            LinearLayout parent = (LinearLayout) LayoutInflater.from(mActivity).inflate(R.layout.quote_view, null);

            ((TextView) parent.findViewById(R.id.text_quote)).setText(text);

            return parent;
        }

        public Spannable getLinksSpannableText(String text) {
            Spannable span = new SpannableString(text);
            span.setSpan(new ForegroundColorSpan(Color.BLUE), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new UnderlineSpan(), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            return span;
        }

        public ImageView getLinkImage() {
            return new ImageView(mActivity);
        }

        public Spannable getIssueSpannableText(String text) {
            Spannable span = new SpannableString(text);
            span.setSpan(new ForegroundColorSpan(Color.GREEN), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            return span;
        }

        public TextView getTextView() {
            TextView view = new TextView(mActivity);
            view.setTextColor(mActivity.getResources().getColor(R.color.primary_text_default_material_light));
            return view;
        }
    }
}
