package com.ne1c.gitteroid.ui.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.text.util.Linkify
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.DependencyManager
import com.ne1c.gitteroid.models.data.StatusMessage
import com.ne1c.gitteroid.models.view.MessageViewModel
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.services.NotificationService
import com.ne1c.gitteroid.ui.SinglelineSpan
import com.ne1c.gitteroid.ui.fragments.ChatRoomFragment
import com.ne1c.gitteroid.ui.fragments.EditMessageFragment
import com.ne1c.gitteroid.ui.fragments.LinksDialogFragment
import com.ne1c.gitteroid.ui.loadUrlWithChromeTabs
import com.ne1c.gitteroid.utils.MarkdownUtils
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MessagesAdapter(private val mDataManager: DataManger,
                      private val mActivity: Activity,
                      private val mMessages: ArrayList<MessageViewModel>,
                      private val mMessageEditText: EditText) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ChatRoomFragment.ReadMessageCallback {
    private var mRoom: RoomViewModel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 0) {
            return DynamicViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_dynamic_message, parent, false))
        } else {
            return StaticViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position >= mMessages.size) {
            return 0
        }

        val markdownUtils = MarkdownUtils(mMessages[position].text)
        if (markdownUtils.existMarkdown()) {
            return 0 // Dynamic
        } else {
            return 1 // Static
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = mMessages[position]

        if (getItemViewType(position) == 0) {
            val dynamicHolder = holder as DynamicViewHolder
            dynamicHolder.parentLayout.setOnClickListener(getParentLayoutClick(message))
            dynamicHolder.avatarImage.setOnClickListener(getAvatarImageClick(message))
            dynamicHolder.messageMenu.setOnClickListener(getMenuClick(message, position))

            processingIndicator(dynamicHolder.newMessageIndicator, message)
            setMessageDynamicText(dynamicHolder, message.text)

            dynamicHolder.timeText.text = getTimeMessage(message)
            dynamicHolder.nicknameText.text = getUsername(message)
            //noinspection ResourceType
            setIconMessage(dynamicHolder.statusMessage, message)

            Glide.with(mActivity).load(message.fromUser?.avatarUrlSmall).into(dynamicHolder.avatarImage)
        } else {
            val staticHolder = holder as StaticViewHolder
            staticHolder.parentLayout.setOnClickListener(getParentLayoutClick(message))
            staticHolder.avatarImage.setOnClickListener(getAvatarImageClick(message))
            staticHolder.messageMenu.setOnClickListener(getMenuClick(message, position))

            processingIndicator(staticHolder.newMessageIndicator, message)
            setMessageStaticText(staticHolder, message.text)

            staticHolder.timeText.text = getTimeMessage(message)
            staticHolder.nicknameText.text = getUsername(message)
            //noinspection ResourceType
            setIconMessage(staticHolder.statusMessage, message)

            Glide.with(mActivity).load(message.fromUser?.avatarUrlSmall).into(staticHolder.avatarImage)
        }
    }

    private fun setMessageStaticText(holder: StaticViewHolder, message: String) {
        var message = message
        if (message.isEmpty()) {
            val span = SpannableString(mActivity.getString(R.string.message_deleted))
            span.setSpan(StyleSpan(Typeface.ITALIC), 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            holder.messageText.text = span
        } else {
            if (message.substring(message.length - 1) == "\n") {
                message = message.substring(0, message.length - 1)
            }

            holder.messageText.text = message
        }
    }

    private fun setMessageDynamicText(holder: DynamicViewHolder, text: String) {
        var text = text
        val markdown = MarkdownUtils(text)
        val views = MarkdownViews()

        var counterSingleline = -1
        var counterMultiline = -1
        var counterBold = -1
        var counterItalics = -1
        var counterQuote = -1
        var counterStrikethrough = -1
        var counterIssue = -1
        var counterGitterLinks = -1
        val counterLinks = -1
        var counterImageLinks = -1
        var counterMentions = -1

        holder.messageLayout.removeAllViews()

        if (markdown.existMarkdown()) {
            for (i in 0..markdown.parsedString.size - 1) {
                when (markdown.parsedString[i]) {
                    "{0}" // Singleline
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        val textView = holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView
                        textView.append(views.getSinglelineCodeSpan(textView.text.length,
                                markdown.singlelineCode[++counterSingleline]))
                    } else {
                        val textView = views.textView
                        textView.text = views.getSinglelineCodeSpan(textView.text.length,
                                markdown.singlelineCode[++counterSingleline])
                        holder.messageLayout.addView(textView)
                    }
                    "{1}" // Multiline
                    -> {
                        val multiline = views.multilineCodeView
                        (multiline.findViewById(R.id.multiline_textView) as TextView).text = markdown.multilineCode[++counterMultiline]
                        holder.messageLayout.addView(multiline)
                    }
                    "{2}" // Bold
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView).append(views.getBoldSpannableText(markdown.bold[++counterBold]))
                    } else {
                        val textView = views.textView
                        textView.text = views.getBoldSpannableText(markdown.bold[++counterBold])
                        holder.messageLayout.addView(textView)
                    }
                    "{3}" // Italics
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView).append(views.getItalicSpannableText(markdown.italics[++counterItalics]))
                    } else {
                        val textView = views.textView
                        textView.text = views.getItalicSpannableText(markdown.italics[++counterItalics])
                        holder.messageLayout.addView(textView)
                    }
                    "{4}" // Strikethrough
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView).append(views.getStrikethroughSpannableText(markdown.strikethrough[++counterStrikethrough]))
                    } else {
                        val strikeView = views.textView
                        strikeView.text = views.getStrikethroughSpannableText(markdown.strikethrough[++counterStrikethrough])
                        holder.messageLayout.addView(strikeView)
                    }
                    "{5}" // Quote
                    -> {
                        val quote = views.getQuoteText(markdown.quote[++counterQuote])
                        holder.messageLayout.addView(quote)
                    }
                    "{6}" // GitterLinks
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        var link = markdown.gitterLinks[++counterGitterLinks]

                        var url = "http://gitter.im"

                        val urlMatcher = Patterns.WEB_URL.matcher(link)
                        if (urlMatcher.find()) {
                            url = urlMatcher.group()

                            if (url.substring(url.length - 1, url.length) == ")") {
                                url = url.substring(0, url.length - 1)
                            }
                        }

                        link = link.substring(1, link.indexOf("]"))

                        val textView = holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView
                        textView.append(views.getLinksSpannableText(link, url))
                        textView.movementMethod = LinkMovementMethod.getInstance()
                    } else {
                        val textView = views.textView
                        var link = markdown.gitterLinks[++counterGitterLinks]

                        var url = "http://gitter.im"

                        val urlMatcher = Patterns.WEB_URL.matcher(link)
                        if (urlMatcher.find()) {
                            url = urlMatcher.group()

                            if (url.substring(url.length - 1, url.length) == ")") {
                                url = url.substring(0, url.length - 1)
                            }
                        }

                        link = link.substring(1, link.indexOf("]"))

                        textView.text = views.getLinksSpannableText(link, url)
                        textView.movementMethod = LinkMovementMethod.getInstance()

                        holder.messageLayout.addView(textView)
                    }
                    "{7}" // Image
                    -> {
                        val link = markdown.imageLinks[++counterImageLinks]
                        val previewUrl: String
                        val fullUrl: String

                        if (link is MarkdownUtils.PreviewImageModel) {
                            previewUrl = link.previewUrl
                            fullUrl = link.fullUrl
                        } else {
                            previewUrl = link.toString()
                            fullUrl = previewUrl
                        }

                        val image = views.linkImage
                        image.setOnClickListener { v -> mActivity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))) }

                        holder.messageLayout.addView(image, 256, 192)

                        //linkImage = linkImage.substring(linkImage.indexOf("http"), linkImage.length() - 2);
                        Glide.with(mActivity).load(previewUrl).crossFade(500).into(image)
                    }
                    "{8}" // Issue
                    -> {
                        val issue = views.textView
                        issue.text = views.getIssueSpannableText(markdown.issues[++counterIssue])
                        holder.messageLayout.addView(issue)
                    }
                    "{9}" // Mention
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView).append(views.getMentionSpannableText(markdown.mentions[++counterMentions]))
                    } else {
                        val mentionView = views.textView
                        mentionView.text = views.getMentionSpannableText(markdown.mentions[++counterMentions])
                        holder.messageLayout.addView(mentionView)
                    }
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
                    else // Text
                    -> if (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) is TextView) {
                        (holder.messageLayout.getChildAt(holder.messageLayout.childCount - 1) as TextView).append(markdown.parsedString[i])
                    } else {
                        val textView = views.textView
                        textView.text = markdown.parsedString[i]
                        holder.messageLayout.addView(textView)
                    }
                }//                        ViewGroup.LayoutParams params = image.getLayoutParams();
                //                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                //                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                //                        image.setLayoutParams(params);
            }
        } else {
            val textView = views.textView

            if (text.substring(text.length - 1) == "\n") {
                text = text.substring(0, text.length - 1)
            }
            textView.text = text

            holder.messageLayout.addView(textView)
        }

        holder.messageLayout.requestLayout()
    }

    private fun getUsername(message: MessageViewModel): String {
        if (!message.fromUser!!.username.isEmpty()) {
            return message.fromUser!!.username
        } else {
            return message.fromUser!!.displayName
        }
    }

    private fun processingIndicator(indicator: ImageView, message: MessageViewModel) {
        if (message.unread) {
            if (message.fromUser!!.id != mDataManager.getUser().id) {
                indicator.setImageResource(R.color.unreadMessage)
            }
        } else {
            indicator.setImageResource(android.R.color.transparent)
        }
    }

    private fun getTimeMessage(message: MessageViewModel): String {
        if (message.sent == StatusMessage.SENDING.name || message.sent == StatusMessage.NO_SEND.name) {
            return ""
        }

        var time = ""
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val nowCalendar = Calendar.getInstance()
        nowCalendar.timeInMillis = System.currentTimeMillis()

        try {
            // GMT TimeZone offset
            val hourOffset = TimeUnit.HOURS.convert(nowCalendar.timeZone.rawOffset.toLong(), TimeUnit.MILLISECONDS)

            calendar.time = formatter.parse(message.sent)
            var hour = calendar.get(Calendar.HOUR_OF_DAY) + hourOffset
            val minutes = calendar.get(Calendar.MINUTE)
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1 // If January Calendar gives 00, but need 01
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Example: 26:31 hours, output 02:31
            if (hour >= 24) {
                hour -= 24
            }

            if (year != nowCalendar.get(Calendar.YEAR)) {
                time = String.format("%02d.%02d.%d", day, month, year)
            } else if (day != nowCalendar.get(Calendar.DAY_OF_MONTH) || month != nowCalendar.get(Calendar.MONTH)) {
                time = String.format("%02d.%02d", day, month)
            }

            // If time contains already day or year
            if (!time.isEmpty()) {
                time += String.format(" %02d:%02d", hour, minutes)
            } else {
                time = String.format("%02d:%02d", hour, minutes)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return time
    }

    // Set icon status for message: send, sending or no send
    private fun setIconMessage(statusMessage: ImageView, message: MessageViewModel) {
        if (message.fromUser!!.id == mDataManager.getUser().id) {
            if (statusMessage.visibility == View.INVISIBLE) {
                statusMessage.visibility = View.VISIBLE
            }

            if (message.sent != StatusMessage.NO_SEND.name && message.sent != StatusMessage.SENDING.name) {
                statusMessage.setImageResource(R.drawable.ic_deliver_mess)
            } else if (message.sent == StatusMessage.NO_SEND.name) {
                statusMessage.setImageResource(R.drawable.ic_error_mess)
            } else if (message.sent == StatusMessage.SENDING.name) {
                statusMessage.setImageResource(R.drawable.ic_sending_mess)
            }
        } else {
            statusMessage.visibility = View.INVISIBLE
        }
    }

    private fun getParentLayoutClick(message: MessageViewModel): View.OnClickListener = View.OnClickListener { mMessageEditText.append("@" + message.fromUser?.username + " ") }

    private fun getAvatarImageClick(message: MessageViewModel): View.OnClickListener {
        return View.OnClickListener {
            mActivity.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse(DataManger.GITHUB_URL + "/" + message.fromUser?.username)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun getMenuClick(message: MessageViewModel, position: Int): View.OnClickListener {
        return View.OnClickListener { v ->
            val menu = PopupMenu(mActivity, v)
            if (message.fromUser?.id == mDataManager.getUser().id) {
                menu.inflate(R.menu.menu_message_user)
                showMenuUser(menu, message, position)
            } else {
                menu.inflate(R.menu.menu_message_all)
                showMenuAll(menu, message)
            }
        }
    }

    private fun showMenuUser(menu: PopupMenu, message: MessageViewModel, position: Int) {
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.edit_message_menu -> {
                    val fragment = EditMessageFragment()
                    val args = Bundle()
                    args.putParcelable("message", message)
                    fragment.arguments = args
                    fragment.show(mActivity.fragmentManager, "dialogEdit")
                    return@setOnMenuItemClickListener true
                }
                R.id.delete_message_menu -> {
                    mDataManager.updateMessage(mRoom!!.id, message.id, "").subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ messageModel ->
                        Toast.makeText(mActivity, R.string.deleted, Toast.LENGTH_SHORT).show()
                        message.text = ""
                        notifyItemChanged(position)
                    }) { throwable -> Toast.makeText(mActivity, R.string.deleted_error, Toast.LENGTH_SHORT).show() }
                    return@setOnMenuItemClickListener true
                }
                R.id.copy_text_menu -> {
                    copyToClipboard(message.text)
                    return@setOnMenuItemClickListener true
                }
                R.id.retry_send_menu -> {
                    if (DependencyManager.INSTANCE.networkService!!.isConnected()) {
                        if (mMessages[position].sent == StatusMessage.NO_SEND.name && mMessages[position].text == message.text) {
                            // Update status message
                            mMessages[position].sent = StatusMessage.SENDING.name

                            // Repeat send
                            mActivity.sendBroadcast(Intent(NotificationService.BROADCAST_SEND_MESSAGE).putExtra(NotificationService.SEND_MESSAGE_EXTRA_KEY, message.text).putExtra(NotificationService.TO_ROOM_MESSAGE_EXTRA_KEY, mRoom!!.id))

                            notifyItemChanged(position)
                        }
                    } else {
                        Toast.makeText(mActivity, R.string.no_network, Toast.LENGTH_SHORT).show()
                    }

                    return@setOnMenuItemClickListener true
                }
                R.id.links_menu -> {
                    val links = LinksDialogFragment()
                    val argsLinks = Bundle()
                    argsLinks.putParcelableArrayList("links", ArrayList<Parcelable>(message.urls))
                    links.arguments = argsLinks
                    links.show(mActivity.fragmentManager, "dialogLinks")

                    return@setOnMenuItemClickListener true

                }
                else -> return@setOnMenuItemClickListener false
            }
        }

        if (message.urls.size <= 0) {
            menu.menu.removeItem(R.id.links_menu)
        }

        if (message.sent != StatusMessage.NO_SEND.name) {
            menu.menu.removeItem(R.id.retry_send_menu)
        }

        menu.show()
    }

    private fun showMenuAll(menu: PopupMenu, message: MessageViewModel) {
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.copy_text_menu -> {
                    copyToClipboard(message.text)
                    return@setOnMenuItemClickListener true
                }
                R.id.links_menu -> {
                    val links = LinksDialogFragment()
                    val argsLinks = Bundle()
                    argsLinks.putParcelableArrayList("links", ArrayList<Parcelable>(message.urls))
                    links.arguments = argsLinks
                    links.show(mActivity.fragmentManager, "dialogLinks")

                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener false
            }
        }

        if (message.urls.size <= 0) {
            menu.menu.removeItem(R.id.links_menu)
        }

        menu.show()
    }

    override fun getItemCount(): Int {
        return mMessages.size
    }

    fun setRoom(model: RoomViewModel) {
        mRoom = model
    }

    // Use for set new message, because if used notifyItemChanged, then call draw
    // bad animation
    fun setMessage(position: Int, message: MessageViewModel) {
        mMessages[position] = message
    }

    override fun read(indicator: ImageView, position: Int) {
        indicator.animate().alpha(0f).setDuration(1000).withLayer()
        mMessages[position].unread = false
    }

    fun copyToClipboard(text: String) {
        val clipboard = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", text)
        clipboard.primaryClip = clip
    }

    inner class DynamicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var parentLayout: LinearLayout
        var messageLayout: LinearLayout
        var avatarImage: ImageView
        var newMessageIndicator: ImageView
        var messageMenu: ImageView
        var statusMessage: ImageView
        var nicknameText: TextView
        var timeText: TextView

        init {

            parentLayout = itemView.findViewById(R.id.parent_layout) as LinearLayout
            messageLayout = itemView.findViewById(R.id.message_layout) as LinearLayout
            avatarImage = itemView.findViewById(R.id.avatar_image) as ImageView
            newMessageIndicator = itemView.findViewById(R.id.new_message_image) as ImageView
            nicknameText = itemView.findViewById(R.id.nickname_text) as TextView
            timeText = itemView.findViewById(R.id.time_text) as TextView
            messageMenu = itemView.findViewById(R.id.overflow_message_menu) as ImageView
            statusMessage = itemView.findViewById(R.id.status_mess_image) as ImageView
        }
    }

    inner class StaticViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var parentLayout: LinearLayout
        var avatarImage: ImageView
        var newMessageIndicator: ImageView
        var messageMenu: ImageView
        var statusMessage: ImageView
        var nicknameText: TextView
        var messageText: TextView
        var timeText: TextView

        init {

            parentLayout = itemView.findViewById(R.id.parent_layout) as LinearLayout
            avatarImage = itemView.findViewById(R.id.avatar_image) as ImageView
            newMessageIndicator = itemView.findViewById(R.id.new_message_image) as ImageView
            nicknameText = itemView.findViewById(R.id.nickname_text) as TextView
            messageText = itemView.findViewById(R.id.message_text) as TextView
            timeText = itemView.findViewById(R.id.time_text) as TextView
            messageMenu = itemView.findViewById(R.id.overflow_message_menu) as ImageView
            statusMessage = itemView.findViewById(R.id.status_mess_image) as ImageView
        }
    }

    private inner class MarkdownViews {
        fun getSinglelineCodeSpan(fromStartPos: Int, text: String): Spannable {
            val span = SpannableString(text)
            span.setSpan(SinglelineSpan(fromStartPos, fromStartPos + text.length), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            return span
        }

        val multilineCodeView: FrameLayout
            get() = LayoutInflater.from(mActivity).inflate(R.layout.multiline_code_view, null) as FrameLayout

        fun getBoldSpannableText(text: String): Spannable {
            val span = SpannableString(text)
            span.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            return span
        }

        fun getItalicSpannableText(text: String): Spannable {
            val span = SpannableString(text)
            span.setSpan(StyleSpan(Typeface.ITALIC), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            return span
        }

        fun getStrikethroughSpannableText(text: String): Spannable {
            val span = SpannableString(text)
            span.setSpan(StrikethroughSpan(), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            return span
        }

        fun getQuoteText(text: String): LinearLayout {
            val parent = LayoutInflater.from(mActivity).inflate(R.layout.quote_view, null) as LinearLayout

            (parent.findViewById(R.id.text_quote) as TextView).text = text

            return parent
        }

        fun getLinksSpannableText(text: String, link: String): Spannable {
            val span = SpannableString(text)
            span.setSpan(URLSpan(link), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    loadUrlWithChromeTabs(mActivity, link)
                }
            }, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            return span
        }

        val linkImage: ImageView
            get() = LayoutInflater.from(mActivity).inflate(R.layout.image_link_view, null) as ImageView

        fun getIssueSpannableText(text: String): Spannable {
            val span = SpannableString(text)
            span.setSpan(ForegroundColorSpan(Color.GREEN), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            return span
        }

        val textView: TextView
            @SuppressLint("PrivateResource")
            get() {
                val view = TextView(mActivity)
                view.setTextColor(mActivity.resources.getColor(R.color.primary_text_default_material_light))
                view.autoLinkMask = Linkify.ALL
                view.linksClickable = true
                return view
            }

        fun getMentionSpannableText(text: String): Spannable {
            val span = SpannableString(text)
            span.setSpan(StyleSpan(Typeface.ITALIC), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(UnderlineSpan(), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            return span
        }
    }
}
