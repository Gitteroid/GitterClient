package com.ne1c.gitteroid.ui.adapters

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
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
import com.ne1c.gitteroid.ui.fragments.ChatRoomFragment
import com.ne1c.gitteroid.ui.fragments.EditMessageFragment
import com.ne1c.gitteroid.ui.fragments.LinksDialogFragment
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
    private val DYNAMIC_ITEM_TYPE = 0
    private val STATIC_ITEM_TYPE = 1

    private var mRoom: RoomViewModel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == DYNAMIC_ITEM_TYPE) {
            return DynamicViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_dynamic_message, parent, false))
        } else {
            return StaticViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position >= mMessages.size) {
            return DYNAMIC_ITEM_TYPE
        }

        val markdownUtils = MarkdownUtils(mMessages[position].text)
        if (markdownUtils.existMarkdown()) {
            return DYNAMIC_ITEM_TYPE
        } else {
            return STATIC_ITEM_TYPE
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == DYNAMIC_ITEM_TYPE) {
            bindDynamicHolder(holder as DynamicViewHolder)
        } else {
            bindStaticHolder(holder as StaticViewHolder)
        }
    }

    fun bindDynamicHolder(holder: DynamicViewHolder) {
        val message = mMessages[holder.adapterPosition]

        with(holder) {
            replyButton.setOnClickListener(getParentLayoutClick(message))
            avatarImage.setOnClickListener(getAvatarImageClick(message))
            messageMenu.setOnClickListener(getMenuClick(message, position))

            processingIndicator(newMessageIndicator, message)
            setMessageDynamicText(holder, message.text)

            timeText.text = getTimeMessage(message)
            nicknameText.text = getUsername(message)

            setIconMessage(statusMessageImage, message)

            Glide.with(mActivity)
                    .load(message.fromUser?.avatarUrlSmall)
                    .into(avatarImage)
        }
    }

    fun bindStaticHolder(holder: StaticViewHolder) {
        val message = mMessages[holder.adapterPosition]

        with(holder) {
            replyButton.setOnClickListener(getParentLayoutClick(message))
            avatarImage.setOnClickListener(getAvatarImageClick(message))
            messageMenu.setOnClickListener(getMenuClick(message, position))

            processingIndicator(newMessageIndicator, message)
            setMessageStaticText(holder, message.text)

            timeText.text = getTimeMessage(message)
            nicknameText.text = getUsername(message)

            setIconMessage(statusMessageImage, message)

            Glide.with(mActivity)
                    .load(message.fromUser?.avatarUrlSmall)
                    .into(avatarImage)
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
        val markdownView = MarkdownViews(holder.itemView.context)

        markdownView.bindMarkdownText(holder, text)
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
        var replyButton: ImageButton
        var messageLayout: LinearLayout
        var avatarImage: ImageView
        var newMessageIndicator: ImageView
        var messageMenu: ImageView
        var statusMessageImage: ImageView
        var nicknameText: TextView
        var timeText: TextView

        init {
            replyButton = itemView.findViewById(R.id.reply_button) as ImageButton
            messageLayout = itemView.findViewById(R.id.message_layout) as LinearLayout
            avatarImage = itemView.findViewById(R.id.avatar_image) as ImageView
            newMessageIndicator = itemView.findViewById(R.id.new_message_image) as ImageView
            nicknameText = itemView.findViewById(R.id.nickname_text) as TextView
            timeText = itemView.findViewById(R.id.time_text) as TextView
            messageMenu = itemView.findViewById(R.id.overflow_message_menu) as ImageView
            statusMessageImage = itemView.findViewById(R.id.status_mess_image) as ImageView
        }
    }

    inner class StaticViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var replyButton: ImageButton
        var avatarImage: ImageView
        var newMessageIndicator: ImageView
        var messageMenu: ImageView
        var statusMessageImage: ImageView
        var nicknameText: TextView
        var messageText: TextView
        var timeText: TextView

        init {
            replyButton = itemView.findViewById(R.id.reply_button) as ImageButton
            avatarImage = itemView.findViewById(R.id.avatar_image) as ImageView
            newMessageIndicator = itemView.findViewById(R.id.new_message_image) as ImageView
            nicknameText = itemView.findViewById(R.id.nickname_text) as TextView
            messageText = itemView.findViewById(R.id.message_text) as TextView
            timeText = itemView.findViewById(R.id.time_text) as TextView
            messageMenu = itemView.findViewById(R.id.overflow_message_menu) as ImageView
            statusMessageImage = itemView.findViewById(R.id.status_mess_image) as ImageView
        }
    }
}
