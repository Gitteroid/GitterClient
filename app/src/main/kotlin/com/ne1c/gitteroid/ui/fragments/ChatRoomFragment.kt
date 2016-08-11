package com.ne1c.gitteroid.ui.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcelable
import android.preference.PreferenceManager
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.di.components.ChatRoomComponent
import com.ne1c.gitteroid.di.components.DaggerChatRoomComponent
import com.ne1c.gitteroid.di.modules.ChatRoomPresenterModule
import com.ne1c.gitteroid.events.NewMessageEvent
import com.ne1c.gitteroid.events.ReadMessagesEvent
import com.ne1c.gitteroid.events.RefreshMessagesRoomEvent
import com.ne1c.gitteroid.events.UpdateMessageEvent
import com.ne1c.gitteroid.models.MessageMapper
import com.ne1c.gitteroid.models.data.MessageModel
import com.ne1c.gitteroid.models.data.StatusMessage
import com.ne1c.gitteroid.models.view.MessageViewModel
import com.ne1c.gitteroid.models.view.RoomViewModel
import com.ne1c.gitteroid.presenters.ChatRoomPresenter
import com.ne1c.gitteroid.ui.activities.MainActivity
import com.ne1c.gitteroid.ui.adapters.MessagesAdapter
import com.ne1c.gitteroid.ui.views.ChatView
import com.ne1c.gitteroid.utils.MarkdownUtils
import com.ne1c.gitteroid.utils.Utils

import java.util.ArrayList
import java.util.Collections

import javax.inject.Inject

import de.greenrobot.event.EventBus
import me.zhanghai.android.materialprogressbar.MaterialProgressBar

import android.view.View.GONE
import com.ne1c.gitteroid.ui.activities.MainActivity.MESSAGE_INTENT_KEY
import com.ne1c.gitteroid.ui.activities.MainActivity.ROOM_ID_INTENT_KEY

class ChatRoomFragment : BaseFragment(), ChatView {
    private var mMessageEditText: EditText? = null
    private var mSendButton: ImageButton? = null
    private var mMessagesList: RecyclerView? = null
    private var mListLayoutManager: LinearLayoutManager? = null
    private var mMessagesAdapter: MessagesAdapter? = null
    private var mProgressBar: ProgressBar? = null
    private var mTopProgressBar: MaterialProgressBar? = null
    private var mFabToBottom: FloatingActionButton? = null
    private var mNewMessagePopupTextView: TextView? = null
    private var mNoMessagesLayout: LinearLayout? = null

    private val mMessagesArr = ArrayList<MessageViewModel>()
    private var mRoom: RoomViewModel? = null

    private var mMessageListSavedState: Parcelable? = null

    private var mStartNumberLoadMessages = 10
    private var mCountLoadMessages = 0

    private var mIsLoadBeforeIdMessages = false
    private var mIsRefreshing = false

    private var mOverviewRoom: RoomViewModel? = null

    private var mComponent: ChatRoomComponent? = null

    @Inject
    internal var mPresenter: ChatRoomPresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true

        val prefs = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
        mStartNumberLoadMessages = Integer.valueOf(prefs.getString("number_load_mess", "10"))!!

        val overview = arguments.getBoolean("overview")
        if (overview) {
            mOverviewRoom = arguments.getParcelable<RoomViewModel>("room")
        } else {
            mRoom = arguments.getParcelable<RoomViewModel>("room")
        }

        EventBus.getDefault().register(this)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_chat_room, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setDataToView(savedInstanceState)
    }

    private fun initViews(v: View) {
        mMessageEditText = v.findViewById(R.id.message_edit_text) as EditText
        mSendButton = v.findViewById(R.id.send_button) as ImageButton

        mProgressBar = v.findViewById(R.id.progress_bar) as ProgressBar
        mProgressBar!!.isIndeterminate = true

        mTopProgressBar = v.findViewById(R.id.top_progress_bar) as MaterialProgressBar
        mTopProgressBar!!.useIntrinsicPadding = false

        mMessagesList = v.findViewById(R.id.messages_list) as RecyclerView
        mListLayoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, true)
        mMessagesList!!.layoutManager = mListLayoutManager
        mMessagesList!!.setItemViewCacheSize(50)

        mFabToBottom = v.findViewById(R.id.fab_to_bottom) as FloatingActionButton
        mFabToBottom!!.setOnClickListener { v1 -> mMessagesList!!.smoothScrollToPosition(0) }
        mFabToBottom!!.hide()

        mNoMessagesLayout = v.findViewById(R.id.no_messages_layout) as LinearLayout

        mNewMessagePopupTextView = v.findViewById(R.id.new_message_popup) as TextView
        mNewMessagePopupTextView!!.setOnClickListener { v1 ->
            mMessagesList!!.smoothScrollToPosition(0)
            hideNewMessagePopup()
        }

        v.findViewById(R.id.markdown_button).setOnClickListener { v1 ->
            val dialog = DialogMarkdownFragment()
            dialog.setTargetFragment(this@ChatRoomFragment, DialogMarkdownFragment.REQUEST_CODE)
            dialog.show(fragmentManager, "MARKDOWN_DIALOG")
        }

        if (mOverviewRoom != null) {
            v.findViewById(R.id.input_layout).visibility = GONE

            val joinRoomButton = v.findViewById(R.id.join_room_button) as Button
            joinRoomButton.visibility = View.VISIBLE
            joinRoomButton.setOnClickListener { v1 -> mPresenter!!.joinToRoom(mOverviewRoom!!.name) }
        }
    }

    private fun hideNewMessagePopup() {
        mNewMessagePopupTextView!!.animate().alpha(0f).withEndAction { mNewMessagePopupTextView!!.visibility = GONE }.start()
    }

    private fun loadRoom() {
        if (mOverviewRoom == null) {
            loadMessages(mRoom)
        } else {
            loadMessages(mOverviewRoom)
        }
    }

    override fun onStart() {
        super.onStart()

        mPresenter!!.bindView(this)

        if (messagesIsNotLoaded()) {
            loadRoom()
        }

        LocalBroadcastManager.getInstance(activity).registerReceiver(mNewMessageReceiver, IntentFilter(MainActivity.BROADCAST_NEW_MESSAGE))
    }

    override fun onStop() {
        super.onStop()

        LocalBroadcastManager.getInstance(activity).unregisterReceiver(mNewMessageReceiver)
        mPresenter!!.unbindView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == DialogMarkdownFragment.REQUEST_CODE) {
            pickedMarkdown(data!!.getIntExtra("layout_id", -1))
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun pickedMarkdown(layoutId: Int) {
        when (layoutId) {
            MarkdownUtils.SINGLELINE_CODE -> {
                mMessageEditText!!.append("``")
                mMessageEditText!!.setSelection(mMessageEditText!!.length() - 1)
            }
            MarkdownUtils.MULTILINE_CODE -> {
                if (mMessageEditText!!.length() > 0) {
                    mMessageEditText!!.append("``````")
                } else {
                    mMessageEditText!!.append("\n``````")
                }

                mMessageEditText!!.setSelection(mMessageEditText!!.length() - 3)
            }
            MarkdownUtils.BOLD -> {
                mMessageEditText!!.append("****")
                mMessageEditText!!.setSelection(mMessageEditText!!.length() - 2)
            }
            MarkdownUtils.ITALICS -> {
                mMessageEditText!!.append("**")
                mMessageEditText!!.setSelection(mMessageEditText!!.length() - 1)
            }
            MarkdownUtils.STRIKETHROUGH -> {
                mMessageEditText!!.append("~~~~")
                mMessageEditText!!.setSelection(mMessageEditText!!.length() - 2)
            }
            MarkdownUtils.QUOTE -> {
                if (mMessageEditText!!.length() > 0) {
                    mMessageEditText!!.append("\n>")
                } else {
                    mMessageEditText!!.append(">")
                }

                mMessageEditText!!.setSelection(mMessageEditText!!.length())
            }
            MarkdownUtils.GITTER_LINK -> {
                mMessageEditText!!.append("[](http://)")
                mMessageEditText!!.setSelection(mMessageEditText!!.length() - 10)
            }
            MarkdownUtils.IMAGE_LINK -> {
                mMessageEditText!!.append("![](http://)")
                mMessageEditText!!.setSelection(mMessageEditText!!.length() - 10)
            }
            else -> {
            }
        }

        mMessageEditText!!.requestFocus()
        mMessageEditText!!.post {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState!!.putParcelable("scrollPosition", mMessagesList!!.layoutManager.onSaveInstanceState())
        outState.putParcelable("active_room", mRoom)
        outState.putParcelableArrayList("messages", mMessagesArr)

        super.onSaveInstanceState(outState)
    }

    private fun setDataToView(savedInstanceState: Bundle) {
        mMessagesAdapter = MessagesAdapter(appComponent.dataManager,
                activity,
                mMessagesArr,
                mMessageEditText)

        mMessagesList!!.adapter = mMessagesAdapter
        mMessagesList!!.addOnScrollListener(mMessagesScrollListener)

        initSavedState(savedInstanceState)

        if (mIsRefreshing) {
            mMessagesList!!.visibility = GONE
            showListProgressBar()
        }

        if (mIsLoadBeforeIdMessages) {
            showTopProgressBar()
        }

        mSendButton!!.setOnClickListener { v -> sendMessage() }
    }

    private fun initSavedState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mMessageListSavedState = savedInstanceState.getParcelable<Parcelable>("scrollPosition")
            savedInstanceState.remove("scrollPosition")

            val room = savedInstanceState.getParcelable<RoomViewModel>("active_room")
            savedInstanceState.remove("active_room")

            if (room != null) {
                mRoom = room
                mMessagesAdapter!!.setRoom(mRoom)
            }

            val messages = savedInstanceState.getParcelableArrayList<MessageModel>("messages")
            savedInstanceState.remove("messages")

            //            if (messages != null && messages.size() > 0) {
            //                mMessagesArr.clear();
            //                mMessagesArr.addAll(messages);
            //
            //                mMessagesAdapter.notifyDataSetChanged();

            if (mMessageListSavedState != null) {
                mMessagesList!!.layoutManager.onRestoreInstanceState(mMessageListSavedState)
                mMessageListSavedState = null
            }
            //}
        }
    }

    private val mMessagesScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
            showFab(newState)
            loadNewMessages()

            if (newState == RecyclerView.SCROLL_STATE_DRAGGING && mNewMessagePopupTextView!!.visibility == View.VISIBLE) {
                hideNewMessagePopup()
            }
        }
    }

    private fun showFab(newScrollState: Int) {
        if (newScrollState == RecyclerView.SCROLL_STATE_IDLE) {
            markMessagesAsRead()

            val firstVisible = mListLayoutManager!!.findFirstVisibleItemPosition()
            if (firstVisible >= 10) {
                mFabToBottom!!.show()
            } else {
                mFabToBottom!!.hide()
            }
        }
    }

    private fun loadNewMessages() {
        val lastMessage = mMessagesArr.size - 1

        if (mListLayoutManager!!.findLastVisibleItemPosition() == lastMessage) {
            if (mMessagesArr.size > 0 && !mMessagesArr[lastMessage].id.isEmpty()) {
                if (!mIsLoadBeforeIdMessages && mTopProgressBar!!.visibility != View.VISIBLE) {
                    mIsLoadBeforeIdMessages = true

                    showTopProgressBar()
                    mPresenter!!.loadMessagesBeforeId(mRoom!!.id, 10, mMessagesArr[lastMessage].id)
                }
            } else {
                hideTopProgressBar()
            }
        }
    }

    private fun sendMessage() {
        if (!mMessageEditText!!.text.toString().isEmpty()) {
            if (Utils.instance.isNetworkConnected) {
                val model = mPresenter!!.createSendMessage(mMessageEditText!!.text.toString())

                mMessagesArr.add(0, model)
                mMessagesAdapter!!.notifyItemInserted(0)

                val first = mListLayoutManager!!.findFirstVisibleItemPosition()
                if (first != 1) {
                    mMessagesList!!.smoothScrollToPosition(0)
                }

                mPresenter!!.sendMessage(mRoom!!.id, model.text)
                mMessageEditText!!.setText("")
            } else {
                Toast.makeText(activity, R.string.no_network, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(activity, R.string.message_empty, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        mComponent = null
        mPresenter!!.onDestroy()
        EventBus.getDefault().unregister(this)

        super.onDestroy()
    }

    private fun markMessagesAsRead() {
        val first = mListLayoutManager!!.findFirstVisibleItemPosition()
        val last = mListLayoutManager!!.findLastVisibleItemPosition()

        if (Utils.instance.isNetworkConnected) {
            val listUnreadIds = ArrayList<String>()
            if (first > -1 && last > -1) {
                for (i in first..last) {
                    if (mMessagesArr[i].unread) {
                        listUnreadIds.add(mMessagesArr[i].id)
                    }
                }
            }

            if (listUnreadIds.size > 0) {
                listUnreadIds.add("") // If listUnreadIds have one item

                val roomId = mRoom!!.id

                mPresenter!!.markMessageAsRead(first, last, roomId,
                        listUnreadIds.toTypedArray())
            }
        }
    }

    private fun loadMessageRoomServer() {
        mPresenter!!.loadMessages(mRoom!!.id, mStartNumberLoadMessages + mCountLoadMessages)
    }

    private fun loadMessages(model: RoomViewModel) {
        hideListProgress()
        hideTopProgressBar()

        mRoom = model
        mCountLoadMessages = 0

        mMessagesAdapter!!.setRoom(mRoom)

        mIsRefreshing = false
        mIsLoadBeforeIdMessages = false

        mPresenter!!.loadMessages(model.id, mStartNumberLoadMessages)
    }

    private val mNewMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val roomId = intent.getStringExtra(ROOM_ID_INTENT_KEY)
            val message = intent.getParcelableExtra<MessageModel>(MESSAGE_INTENT_KEY)

            if (roomId != mRoom!!.id) {
                return
            }

            if (mMessagesAdapter != null) {
                for (i in mMessagesArr.indices) { // If updated message or send message
                    val item = mMessagesArr[i]

                    // Send message
                    if (message.text == item.text && item.sent == StatusMessage.SENDING.name) {
                        return
                    }

                    // Update message
                    if (item.sent != StatusMessage.NO_SEND.name
                            && item.sent != StatusMessage.SENDING.name
                            && item.id == message.id) {
                        return
                    }
                }

                mMessagesArr.add(0, MessageMapper.mapToView(message))
                mMessagesAdapter!!.notifyItemInserted(0)

                val firstVisible = mListLayoutManager!!.findFirstVisibleItemPosition()

                if (firstVisible == 0) {
                    mMessagesList!!.smoothScrollToPosition(0)
                } else {
                    showNewMessagePopup()
                }
            }
        }
    }

    private fun showNewMessagePopup() {
        mNewMessagePopupTextView!!.animate().alpha(1f).setDuration(500).withStartAction {
            mNewMessagePopupTextView!!.alpha = 0f
            mNewMessagePopupTextView!!.visibility = View.VISIBLE
        }.start()
    }

    fun onEvent(room: RefreshMessagesRoomEvent) {
        if (room.roomModel.id == mRoom!!.id) {
            mIsRefreshing = true
            showListProgressBar()
            loadMessageRoomServer()
        }
    }

    fun onEvent(message: UpdateMessageEvent) {
        val newMessage = message.messageModel

        if (newMessage != null) {
            mPresenter!!.updateMessages(mRoom!!.id, newMessage.id, newMessage.text)
        }
    }

    fun onEvent(event: NewMessageEvent) {
        if (event.room.id != mRoom!!.id) {
            return
        }

        if (mMessagesAdapter != null) {
            for (i in mMessagesArr.indices) { // If updated message or send message
                val item = mMessagesArr[i]

                // Send message
                if (event.message.text == item.text && item.sent == StatusMessage.SENDING.name) {
                    return
                }

                // Update message
                if (item.sent != StatusMessage.NO_SEND.name
                        && item.sent != StatusMessage.SENDING.name
                        && item.id == event.message.id) {
                    return
                }
            }

            mMessagesArr.add(0, event.message)
            mMessagesAdapter!!.notifyItemInserted(0)

            val firstVisible = mListLayoutManager!!.findFirstVisibleItemPosition()
            if (firstVisible >= 1 && firstVisible <= 5) {
                mMessagesList!!.smoothScrollToPosition(0)
            }
        }
    }

    override fun showMessages(messages: ArrayList<MessageViewModel>) {
        Collections.reverse(messages)

        mMessagesArr.clear()
        mMessagesArr.addAll(messages)

        mMessagesAdapter!!.notifyDataSetChanged()

        if (mNoMessagesLayout!!.visibility == View.VISIBLE) {
            mNoMessagesLayout!!.visibility = GONE
        }

        if (mMessageListSavedState != null) {
            mMessagesList!!.layoutManager.onRestoreInstanceState(mMessageListSavedState)
            mMessageListSavedState = null
        } else if (mListLayoutManager!!.findLastCompletelyVisibleItemPosition() != 0) { // If room just was loaded
            mMessagesList!!.scrollToPosition(0)
        }

        mIsRefreshing = false
    }

    override fun showError(resId: Int) {
        hideListProgress()
        hideTopProgressBar()

        mIsRefreshing = false
        mIsLoadBeforeIdMessages = false

        Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show()

        if (messagesIsNotLoaded()) {
            mNoMessagesLayout!!.visibility = View.VISIBLE
        }
    }

    override fun showUpdateMessage(message: MessageViewModel) {
        for (i in mMessagesArr.indices) {
            val item = mMessagesArr[i]
            // Update message
            if (item.sent != StatusMessage.NO_SEND.name
                    && item.sent != StatusMessage.SENDING.name
                    && item.id == message.id) {
                mMessagesArr[i] = message
                mMessagesAdapter!!.setMessage(i, message)
                mMessagesAdapter!!.notifyItemChanged(i)
            }
        }

        Toast.makeText(activity, R.string.updated, Toast.LENGTH_SHORT).show()
    }

    override fun successReadMessages(first: Int, last: Int, roomId: String, countRead: Int) {
        if (roomId == mRoom!!.id) {
            for (i in first..last) {
                if (mMessagesAdapter!!.getItemViewType(i) == 0) {
                    val holder = mMessagesList!!.findViewHolderForAdapterPosition(i) as MessagesAdapter.DynamicViewHolder
                    if (holder != null) {
                        mMessagesAdapter!!.read(holder.newMessageIndicator, i)
                        mMessagesArr[i].unread = false
                    }
                } else {
                    val holder = mMessagesList!!.findViewHolderForAdapterPosition(i) as MessagesAdapter.StaticViewHolder
                    if (holder != null) {
                        mMessagesAdapter!!.read(holder.newMessageIndicator, i)
                        mMessagesArr[i].unread = false
                    }
                }
            }
        }

        // Send notification to MainActivity
        val readMessagesEventBus = ReadMessagesEvent()

        // -1 but last item equals to empty string
        readMessagesEventBus.countRead = countRead
        readMessagesEventBus.roomId = mRoom!!.id
        EventBus.getDefault().post(readMessagesEventBus)
    }

    override fun showLoadBeforeIdMessages(messages: ArrayList<MessageViewModel>) {
        if (messages.size > 0 && !mIsRefreshing) {
            Collections.reverse(messages)

            mMessagesArr.addAll(mMessagesArr.size, messages)
            mMessagesAdapter!!.notifyItemRangeInserted(mMessagesArr.size, messages.size)

            mCountLoadMessages += messages.size
        }

        hideTopProgressBar()
        mIsLoadBeforeIdMessages = false
    }

    override fun deliveredMessage(message: MessageViewModel) {
        for (i in mMessagesArr.indices) {
            if (mMessagesArr[i].sent == StatusMessage.SENDING.name && mMessagesArr[i].text == message.text) {
                mMessagesArr[i] = message
                mMessagesAdapter!!.notifyItemChanged(i)

                if (mListLayoutManager!!.findLastVisibleItemPosition() == mMessagesArr.size - 2) {
                    mMessagesList!!.scrollToPosition(mMessagesArr.size - 1)
                }
            }
        }
    }

    override fun errorDeliveredMessage() {
        Toast.makeText(activity, R.string.error_send, Toast.LENGTH_SHORT).show()

        for (i in mMessagesArr.indices) {
            if (mMessagesArr[i].sent == StatusMessage.SENDING.name) {
                mMessagesArr[i].sent = StatusMessage.NO_SEND.name
                mMessagesAdapter!!.notifyItemChanged(i)
            }
        }
    }

    override fun showTopProgressBar() {
        if (mTopProgressBar!!.visibility != View.VISIBLE) {
            mTopProgressBar!!.visibility = View.VISIBLE
        }
    }

    override fun hideTopProgressBar() {
        if (mTopProgressBar!!.visibility == View.VISIBLE) {
            mTopProgressBar!!.visibility = GONE
        }
    }

    override fun showListProgressBar() {
        hideTopProgressBar()

        mMessagesList!!.visibility = GONE
        mProgressBar!!.visibility = View.VISIBLE

        mIsRefreshing = true
    }

    override fun hideListProgress() {
        if (mMessagesList!!.visibility != View.VISIBLE) {
            mMessagesList!!.visibility = View.VISIBLE
            mProgressBar!!.visibility = GONE
        }

        mIsRefreshing = false
    }

    override fun joinToRoom() {
        activity.onBackPressed()
    }

    override fun initDiComponent() {
        mComponent = DaggerChatRoomComponent.builder().applicationComponent(appComponent).chatRoomPresenterModule(ChatRoomPresenterModule()).build()

        mComponent!!.inject(this)
    }

    private fun messagesIsNotLoaded(): Boolean {
        return mMessagesAdapter == null || mMessagesAdapter!!.itemCount == 0
    }

    interface ReadMessageCallback {
        fun read(indicator: ImageView, position: Int)
    }

    companion object {

        @JvmOverloads fun newInstance(room: RoomViewModel, overview: Boolean = false): ChatRoomFragment {
            val bundle = Bundle()
            bundle.putParcelable("room", room)
            bundle.putBoolean("overview", overview)

            val fragment = ChatRoomFragment()
            fragment.arguments = bundle

            return fragment
        }
    }
}
