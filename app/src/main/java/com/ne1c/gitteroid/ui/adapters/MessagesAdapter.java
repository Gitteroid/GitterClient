package com.ne1c.gitteroid.ui.adapters;

import android.annotation.SuppressLint;
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
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.text.util.Linkify;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ne1c.gitteroid.R;
import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.models.data.StatusMessage;
import com.ne1c.gitteroid.models.view.MessageViewModel;
import com.ne1c.gitteroid.models.view.RoomViewModel;
import com.ne1c.gitteroid.services.NotificationService;
import com.ne1c.gitteroid.ui.SinglelineSpan;
import com.ne1c.gitteroid.ui.fragments.ChatRoomFragment;
import com.ne1c.gitteroid.ui.fragments.EditMessageFragment;
import com.ne1c.gitteroid.ui.fragments.LinksDialogFragment;
import com.ne1c.gitteroid.utils.MarkdownUtils;
import com.ne1c.gitteroid.utils.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements
        ChatRoomFragment.ReadMessageCallback {
    private RoomViewModel mRoom;
    private ArrayList<MessageViewModel> mMessages;
    private Activity mActivity;
    private EditText mMessageEditText;
    private DataManger mDataManager;

    public MessagesAdapter(DataManger dataManger, Activity activity, ArrayList<MessageViewModel> messages, EditText editText) {
        mActivity = activity;
        mMessages = messages;
        mMessageEditText = editText;
        mDataManager = dataManger;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            return new DynamicViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_dynamic_message, parent, false));
        } else {
            return new StaticViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= mMessages.size()) {
            return 0;
        }

        MarkdownUtils markdownUtils = new MarkdownUtils(mMessages.get(position).text);
        if (markdownUtils.existMarkdown()) {
            return 0; // Dynamic
        } else {
            return 1; // Static
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MessageViewModel message = mMessages.get(position);

        if (getItemViewType(position) == 0) {
            DynamicViewHolder dynamicHolder = (DynamicViewHolder) holder;
            dynamicHolder.parentLayout.setOnClickListener(getParentLayoutClick(message));
            dynamicHolder.avatarImage.setOnClickListener(getAvatarImageClick(message));
            dynamicHolder.messageMenu.setOnClickListener(getMenuClick(message, position));

            processingIndicator(dynamicHolder.newMessageIndicator, message);
            setMessageDynamicText(dynamicHolder, message.text);

            dynamicHolder.timeText.setText(getTimeMessage(message));
            dynamicHolder.nicknameText.setText(getUsername(message));
            //noinspection ResourceType
            setIconMessage(dynamicHolder.statusMessage, message);

            Glide.with(mActivity).load(message.fromUser.avatarUrlSmall).into(dynamicHolder.avatarImage);
        } else {
            StaticViewHolder staticHolder = (StaticViewHolder) holder;
            staticHolder.parentLayout.setOnClickListener(getParentLayoutClick(message));
            staticHolder.avatarImage.setOnClickListener(getAvatarImageClick(message));
            staticHolder.messageMenu.setOnClickListener(getMenuClick(message, position));

            processingIndicator(staticHolder.newMessageIndicator, message);
            setMessageStaticText(staticHolder, message.text);

            staticHolder.timeText.setText(getTimeMessage(message));
            staticHolder.nicknameText.setText(getUsername(message));
            //noinspection ResourceType
            setIconMessage(staticHolder.statusMessage, message);

            Glide.with(mActivity).load(message.fromUser.avatarUrlSmall).into(staticHolder.avatarImage);
        }
    }

    private void setMessageStaticText(StaticViewHolder holder, String message) {
        if (message.isEmpty()) {
            Spannable span = new SpannableString(mActivity.getString(R.string.message_deleted));
            span.setSpan(new StyleSpan(Typeface.ITALIC), 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            holder.messageText.setText(span);
        } else {
            if (message.substring(message.length() - 1).equals("\n")) {
                message = message.substring(0, message.length() - 1);
            }

            holder.messageText.setText(message);
        }
    }

    private void setMessageDynamicText(DynamicViewHolder holder, String text) {
        MarkdownUtils markdown = new MarkdownUtils(text);
        MarkdownViews views = new MarkdownViews();

        int counterSingleline = -1;
        int counterMultiline = -1;
        int counterBold = -1;
        int counterItalics = -1;
        int counterQuote = -1;
        int counterStrikethrough = -1;
        int counterIssue = -1;
        int counterGitterLinks = -1;
        int counterLinks = -1;
        int counterImageLinks = -1;
        int counterMentions = -1;

        holder.messageLayout.removeAllViews();

        if (markdown.existMarkdown()) {
            for (int i = 0; i < markdown.getParsedString().size(); i++) {
                switch (markdown.getParsedString().get(i)) {
                    case "{0}": // Singleline
                        if (holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1) instanceof TextView) {
                            TextView textView = (TextView) holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1);
                            textView.append(views.getSinglelineCodeSpan(textView.getText().length(),
                                    markdown.getSinglelineCode().get(++counterSingleline)));
                        } else {
                            TextView textView = views.getTextView();
                            textView.setText(views.getSinglelineCodeSpan(textView.getText().length(),
                                    markdown.getSinglelineCode().get(++counterSingleline)));
                            holder.messageLayout.addView(textView);
                        }

                        break;
                    case "{1}": // Multiline
                        FrameLayout multiline = views.getMultilineCodeView();
                        ((TextView) multiline.findViewById(R.id.multiline_textView)).setText(markdown.getMultilineCode().get(++counterMultiline));
                        holder.messageLayout.addView(multiline);

                        break;
                    case "{2}": // Bold
                        if (holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1) instanceof TextView) {
                            ((TextView) holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1))
                                    .append(views.getBoldSpannableText(markdown.getBold().get(++counterBold)));
                        } else {
                            TextView textView = views.getTextView();
                            textView.setText(views.getBoldSpannableText(markdown.getBold().get(++counterBold)));
                            holder.messageLayout.addView(textView);
                        }

                        break;
                    case "{3}": // Italics
                        if (holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1) instanceof TextView) {
                            ((TextView) holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1))
                                    .append(views.getItalicSpannableText(markdown.getItalics().get(++counterItalics)));
                        } else {
                            TextView textView = views.getTextView();
                            textView.setText(views.getItalicSpannableText(markdown.getItalics().get(++counterItalics)));
                            holder.messageLayout.addView(textView);
                        }

                        break;
                    case "{4}": // Strikethrough
                        if (holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1) instanceof TextView) {
                            ((TextView) holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1))
                                    .append(views.getStrikethroughSpannableText(markdown.getStrikethrough().get(++counterStrikethrough)));
                        } else {
                            TextView strikeView = views.getTextView();
                            strikeView.setText(views.getStrikethroughSpannableText(markdown.getStrikethrough().get(++counterStrikethrough)));
                            holder.messageLayout.addView(strikeView);
                        }


                        break;
                    case "{5}": // Quote
                        LinearLayout quote = views.getQuoteText(markdown.getQuote().get(++counterQuote));
                        holder.messageLayout.addView(quote);

                        break;
                    case "{6}": // GitterLinks
                        if (holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1) instanceof TextView) {
                            String link = markdown.getGitterLinks().get(++counterGitterLinks);

                            String url = "http://gitter.im";

                            Matcher urlMatcher = Patterns.WEB_URL.matcher(link);
                            if (urlMatcher.find()) {
                                url = urlMatcher.group();

                                if (url.substring(url.length() - 1, url.length()).equals(")")) {
                                    url = url.substring(0, url.length() - 1);
                                }
                            }

                            link = link.substring(1, link.indexOf("]"));

                            TextView textView = (TextView) holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1);
                            textView.append(views.getLinksSpannableText(link, url));
                            textView.setMovementMethod(LinkMovementMethod.getInstance());
                        } else {
                            TextView textView = views.getTextView();
                            String link = markdown.getGitterLinks().get(++counterGitterLinks);

                            String url = "http://gitter.im";

                            Matcher urlMatcher = Patterns.WEB_URL.matcher(link);
                            if (urlMatcher.find()) {
                                url = urlMatcher.group();

                                if (url.substring(url.length() - 1, url.length()).equals(")")) {
                                    url = url.substring(0, url.length() - 1);
                                }
                            }

                            link = link.substring(1, link.indexOf("]"));

                            textView.setText(views.getLinksSpannableText(link, url));
                            textView.setMovementMethod(LinkMovementMethod.getInstance());

                            holder.messageLayout.addView(textView);
                        }
                        break;
                    case "{7}": // Image
                        Object link = markdown.getImageLinks().get(++counterImageLinks);
                        String previewUrl;
                        final String fullUrl;

                        if (link instanceof MarkdownUtils.PreviewImageModel) {
                            previewUrl = ((MarkdownUtils.PreviewImageModel) link).getPreviewUrl();
                            fullUrl = ((MarkdownUtils.PreviewImageModel) link).getFullUrl();
                        } else {
                            previewUrl = String.valueOf(link);
                            fullUrl = previewUrl;
                        }

                        ImageView image = views.getLinkImage();
                        image.setOnClickListener(v -> mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))));

                        holder.messageLayout.addView(image, 256, 192);

                        //linkImage = linkImage.substring(linkImage.indexOf("http"), linkImage.length() - 2);
                        Glide.with(mActivity).load(previewUrl).crossFade(500).into(image);


//                        ViewGroup.LayoutParams params = image.getLayoutParams();
//                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
//                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
//                        image.setLayoutParams(params);

                        break;
                    case "{8}": // Issue
                        TextView issue = views.getTextView();
                        issue.setText(views.getIssueSpannableText(markdown.getIssues().get(++counterIssue)));
                        holder.messageLayout.addView(issue);

                        break;
                    case "{9}": // Mention
                        if (holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1) instanceof TextView) {
                            ((TextView) holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1))
                                    .append(views.getMentionSpannableText(markdown.getMentions().get(++counterMentions)));
                        } else {
                            TextView mentionView = views.getTextView();
                            mentionView.setText(views.getMentionSpannableText(markdown.getMentions().get(++counterMentions)));
                            holder.messageLayout.addView(mentionView);
                        }

                        break;
//                    case "{10}": // Links
//                        if (holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1) instanceof TextView) {
//                            String url = markdown.getLinks().get(++counterLinks);
//
//                            TextView textView = (TextView) holder.messageLayout.getChildAt(holder.messageLayout.getChildCount() - 1);
//                            textView.append(views.getLinksSpannableText(url, url));
//                            textView.setMovementMethod(LinkMovementMethod.getInstance());
//                        } else {
//                            TextView textView = views.getTextView();
//                            String url = markdown.getLinks().get(++counterLinks);
//
//                            textView.setText(views.getLinksSpannableText(url, url));
//                            textView.setMovementMethod(LinkMovementMethod.getInstance());
//
//                            holder.messageLayout.addView(textView);
//                        }
//
//                        break;
                    default: // Text
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

            if (text.substring(text.length() - 1).equals("\n")) {
                text = text.substring(0, text.length() - 1);
            }
            textView.setText(text);

            holder.messageLayout.addView(textView);
        }

        holder.messageLayout.requestLayout();
    }

    private String getUsername(MessageViewModel message) {
        if (!message.fromUser.username.isEmpty()) {
            return message.fromUser.username;
        } else {
            return message.fromUser.displayName;
        }
    }

    private void processingIndicator(ImageView indicator, MessageViewModel message) {
        if (message.unread) {
            if (!message.fromUser.id.equals(Utils.getInstance().getUserPref().id)) {
                indicator.setImageResource(R.color.unreadMessage);
            }
        } else {
            indicator.setImageResource(android.R.color.transparent);
        }
    }

    private String getTimeMessage(MessageViewModel message) {
        if (message.sent.equals(StatusMessage.SENDING.name()) ||
                message.sent.equals(StatusMessage.NO_SEND.name())) {
            return "";
        }

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
            int month = calendar.get(Calendar.MONTH) + 1; // If January Calendar gives 00, but need 01
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
    private void setIconMessage(ImageView statusMessage, MessageViewModel message) {
        if (message.fromUser.id.equals(Utils.getInstance().getUserPref().id)) {
            if (statusMessage.getVisibility() == View.INVISIBLE) {
                statusMessage.setVisibility(View.VISIBLE);
            }

            if (!message.sent.equals(StatusMessage.NO_SEND.name())
                    && !message.sent.equals(StatusMessage.SENDING.name())) {
                statusMessage.setImageResource(R.drawable.ic_deliver_mess);
            } else if (message.sent.equals(StatusMessage.NO_SEND.name())) {
                statusMessage.setImageResource(R.drawable.ic_error_mess);
            } else if (message.sent.equals(StatusMessage.SENDING.name())) {
                statusMessage.setImageResource(R.drawable.ic_sending_mess);
            }
        } else {
            statusMessage.setVisibility(View.INVISIBLE);
        }
    }

    private View.OnClickListener getParentLayoutClick(final MessageViewModel message) {
        return v -> mMessageEditText.append("@" + message.fromUser.username + " ");
    }

    private View.OnClickListener getAvatarImageClick(final MessageViewModel message) {
        return v -> mActivity.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse(Utils.GITHUB_URL + "/" + message.fromUser.username))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private View.OnClickListener getMenuClick(final MessageViewModel message, final int position) {
        return v -> {
            final PopupMenu menu = new PopupMenu(mActivity, v);
            if (message.fromUser.id.equals(Utils.getInstance().getUserPref().id)) {
                menu.inflate(R.menu.menu_message_user);
                showMenuUser(menu, message, position);
            } else {
                menu.inflate(R.menu.menu_message_all);
                showMenuAll(menu, message);
            }
        };
    }

    private void showMenuUser(PopupMenu menu, final MessageViewModel message, final int position) {
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.edit_message_menu:
                    EditMessageFragment fragment = new EditMessageFragment();
                    Bundle args = new Bundle();
                    args.putParcelable("message", message);
                    fragment.setArguments(args);
                    fragment.show(mActivity.getFragmentManager(), "dialogEdit");
                    return true;
                case R.id.delete_message_menu:
                    mDataManager.updateMessage(mRoom.id, message.id, "")
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(messageModel -> {
                                Toast.makeText(mActivity, R.string.deleted, Toast.LENGTH_SHORT).show();
                                message.text = "";
                                notifyItemChanged(position);
                            }, throwable -> {
                                Toast.makeText(mActivity, R.string.deleted_error, Toast.LENGTH_SHORT).show();
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
                            mActivity.sendBroadcast(new Intent(NotificationService.BROADCAST_SEND_MESSAGE)
                                    .putExtra(NotificationService.SEND_MESSAGE_EXTRA_KEY, message.text)
                                    .putExtra(NotificationService.TO_ROOM_MESSAGE_EXTRA_KEY, mRoom.id));

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
        });

        if (message.urls.size() <= 0) {
            menu.getMenu().removeItem(R.id.links_menu);
        }

        if (!message.sent.equals(StatusMessage.NO_SEND.name())) {
            menu.getMenu().removeItem(R.id.retry_send_menu);
        }

        menu.show();
    }

    private void showMenuAll(final PopupMenu menu, final MessageViewModel message) {
        menu.setOnMenuItemClickListener(item -> {
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

    public void setRoom(RoomViewModel model) {
        mRoom = model;
    }

    // Use for set new message, because if used notifyItemChanged, then call draw
    // bad animation
    public void setMessage(int position, MessageViewModel message) {
        mMessages.set(position, message);
    }

    @Override
    public void read(ImageView indicator, int position) {
        indicator.animate().alpha(0f).setDuration(1000).withLayer();
        mMessages.get(position).unread = false;
    }

    public class DynamicViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout parentLayout;
        public LinearLayout messageLayout;
        public ImageView avatarImage;
        public ImageView newMessageIndicator;
        public ImageView messageMenu;
        public ImageView statusMessage;
        public TextView nicknameText;
        public TextView timeText;

        public DynamicViewHolder(View itemView) {
            super(itemView);

            parentLayout = (LinearLayout) itemView.findViewById(R.id.parent_layout);
            messageLayout = (LinearLayout) itemView.findViewById(R.id.message_layout);
            avatarImage = (ImageView) itemView.findViewById(R.id.avatar_image);
            newMessageIndicator = (ImageView) itemView.findViewById(R.id.new_message_image);
            nicknameText = (TextView) itemView.findViewById(R.id.nickname_text);
            timeText = (TextView) itemView.findViewById(R.id.time_text);
            messageMenu = (ImageView) itemView.findViewById(R.id.overflow_message_menu);
            statusMessage = (ImageView) itemView.findViewById(R.id.status_mess_image);
        }
    }

    public class StaticViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout parentLayout;
        public ImageView avatarImage;
        public ImageView newMessageIndicator;
        public ImageView messageMenu;
        public ImageView statusMessage;
        public TextView nicknameText;
        public TextView messageText;
        public TextView timeText;

        public StaticViewHolder(View itemView) {
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

    private class MarkdownViews {
        public Spannable getSinglelineCodeSpan(int fromStartPos, String text) {
            Spannable span = new SpannableString(text);
            span.setSpan(new SinglelineSpan(fromStartPos, fromStartPos + text.length()), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            return span;
        }

        public FrameLayout getMultilineCodeView() {
            return (FrameLayout) LayoutInflater.from(mActivity).inflate(R.layout.multiline_code_view, null);
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
            span.setSpan(new StrikethroughSpan(), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            return span;
        }

        public LinearLayout getQuoteText(String text) {
            LinearLayout parent = (LinearLayout) LayoutInflater.from(mActivity).inflate(R.layout.quote_view, null);

            ((TextView) parent.findViewById(R.id.text_quote)).setText(text);

            return parent;
        }

        public Spannable getLinksSpannableText(String text, String link) {
            Spannable span = new SpannableString(text);
            span.setSpan(new URLSpan(link), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    widget.getContext().startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
                }
            }, 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            return span;
        }

        public ImageView getLinkImage() {
            return (ImageView) LayoutInflater.from(mActivity).inflate(R.layout.image_link_view, null);
        }

        public Spannable getIssueSpannableText(String text) {
            Spannable span = new SpannableString(text);
            span.setSpan(new ForegroundColorSpan(Color.GREEN), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            return span;
        }

        @SuppressLint("PrivateResource")
        public TextView getTextView() {
            TextView view = new TextView(mActivity);
            view.setTextColor(mActivity.getResources().getColor(R.color.primary_text_default_material_light));
            view.setAutoLinkMask(Linkify.ALL);
            view.setLinksClickable(true);
            return view;
        }

        private Spannable getMentionSpannableText(String text) {
            Spannable span = new SpannableString(text);
            span.setSpan(new StyleSpan(Typeface.ITALIC), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new UnderlineSpan(), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            return span;
        }
    }
}
