package com.ne1c.developerstalk.presenters

import com.ne1c.developerstalk.BuildConfig
import com.ne1c.developerstalk.MockRxSchedulersFactory
import com.ne1c.developerstalk.api.responses.StatusResponse
import com.ne1c.developerstalk.dataproviders.DataManger
import com.ne1c.developerstalk.models.MessageModel
import com.ne1c.developerstalk.ui.views.ChatView
import com.ne1c.developerstalk.utils.Utils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rx.Observable
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(21))
class ChatRoomPresenterTest {
    @Mock
    var view: ChatView? = null

    @Mock
    var dataManger: DataManger? = null

    private var presenter: ChatRoomPresenter? = null

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        Utils.init(RuntimeEnvironment.application.applicationContext)

        presenter = ChatRoomPresenter(MockRxSchedulersFactory(), dataManger)
        presenter?.bindView(view)
    }

    @Test
    fun successLoadCachedMessages() {
        `when`(dataManger?.getDbMessages(anyString())).thenReturn(Observable.just<ArrayList<MessageModel>>(arrayListOf()))

        presenter!!.loadCachedMessages(ROOM_ID)

        verify(view, times(1))?.showMessages(arrayListOf())
        verify(view, never())?.showError(anyInt())
    }

    @Test
    fun failLoadCachedMessages() {
        `when`(dataManger?.getDbMessages(anyString())).thenReturn(Observable.error<ArrayList<MessageModel>>(Throwable(ERROR)))

        presenter!!.loadCachedMessages(anyString())

        verify(view, times(1))?.showError(anyInt())
    }

    @Test
    fun successSendMessage() {
        `when`(dataManger?.sendMessage(anyString(), anyString())).thenReturn(Observable.just(mock<MessageModel>(MessageModel::class.java)))

        presenter!!.sendMessage(ROOM_ID, MESSAGE_TEXT)

        verify(view, times(1))?.deliveredMessage(any<MessageModel>(MessageModel::class.java))
        verify(view, never())?.showError(anyInt())
    }

    @Test
    fun failSendMessage() {
        `when`(dataManger?.sendMessage(anyString(), anyString())).thenReturn(Observable.error<MessageModel>(Throwable(ERROR)))

        presenter?.sendMessage(ROOM_ID, MESSAGE_TEXT)

        verify(view, times(1))?.errorDeliveredMessage()
    }

    @Test
    fun successLoadMessagesBeforeId() {
        `when`(dataManger?.getMessagesBeforeId(anyString(), anyInt(), anyString())).thenReturn(Observable.just<ArrayList<MessageModel>>(arrayListOf()))

        presenter?.loadMessagesBeforeId(ROOM_ID, 100500, ROOM_ID)

        verify(view, times(1))?.showLoadBeforeIdMessages(arrayListOf())
        verify(view, never())?.showError(anyInt())
    }

    @Test
    fun failLoadMessagesBeforeId() {
        `when`(dataManger?.getMessagesBeforeId(anyString(), anyInt(), anyString())).thenReturn(Observable.error<ArrayList<MessageModel>>(Throwable(ERROR)))

        presenter?.loadMessagesBeforeId(ROOM_ID, 100500, ROOM_ID)

        verify(view, never())?.showLoadBeforeIdMessages(arrayListOf())
        verify(view, times(1))?.hideTopProgressBar()
    }

    @Test
    fun successLoadMessages() {
        `when`(dataManger?.getNetworkMessages(anyString(), anyInt())).thenReturn(Observable.just<ArrayList<MessageModel>>(arrayListOf()))

        presenter?.loadNetworkMessages(ROOM_ID, 5)

        verify(view, times(1))?.showMessages(arrayListOf())
        verify(view, never())?.showError(anyInt())
    }

    @Test
    fun failLoadMessages() {
        `when`(dataManger?.getNetworkMessages(anyString(), anyInt())).thenReturn(Observable.error<ArrayList<MessageModel>>(Throwable(ERROR)))

        presenter?.loadNetworkMessages(ROOM_ID, 100500)

        verify(view, never())?.showMessages(arrayListOf())
        verify(view, times(1))?.hideListProgress()
    }

    @Test
    fun successUploadMessages() {
        val message = mock<MessageModel>(MessageModel::class.java)

        `when`(dataManger?.updateMessage(anyString(), anyString(), anyString())).thenReturn(Observable.just(message))

        presenter?.updateMessages(ROOM_ID, ROOM_ID, MESSAGE_TEXT)

        verify(dataManger, times(1))?.insertMessageToDb(message, ROOM_ID)
        verify(view, times(1))?.showUpdateMessage(message)
        verify(view, never())?.showError(anyInt())
    }

    @Test
    fun failUploadMessages() {
        val message = mock<MessageModel>(MessageModel::class.java)

        `when`(dataManger?.updateMessage(anyString(), anyString(), anyString())).thenReturn(Observable.error<MessageModel>(Throwable(ERROR)))

        presenter?.updateMessages(ROOM_ID, ROOM_ID, MESSAGE_TEXT)

        verify(dataManger, never())?.insertMessageToDb(message, ROOM_ID)
        verify(view, never())?.showUpdateMessage(message)
        verify(view, times(1))?.showError(anyInt())
    }

    @Test
    fun successMarkMessageAsRead() {
        val resp = StatusResponse()
        resp.success = true

        val ids = arrayOfNulls<String>(5)

        `when`(dataManger?.readMessages(ROOM_ID, ids)).thenReturn(Observable.just(resp));

        presenter?.markMessageAsRead(100500, 100500, ROOM_ID, ids)

        verify(view, times(1))?.successReadMessages(anyInt(), anyInt(), anyString(), anyInt())
    }

    @Test
    fun failMarkMessageAsRead() {
        val ids = arrayOfNulls<String>(5)

        `when`(dataManger?.readMessages(ROOM_ID, ids)).thenReturn(Observable.error<StatusResponse>(Throwable(ERROR)))

        presenter!!.markMessageAsRead(100500, 100500, ROOM_ID, ids)

        verify(view, never())?.successReadMessages(anyInt(), anyInt(), anyString(), anyInt())
    }

    @After
    fun end() {
        presenter?.unbindView()
    }

    companion object {
        private val ROOM_ID = "jf9w4j3fmn389f394n"
        private val MESSAGE_TEXT = "message"
        private val ERROR = "text_with_error"
    }
}
