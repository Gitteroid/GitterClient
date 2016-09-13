package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.TestExecutorService
import com.ne1c.gitteroid.TestSharedPreferences
import com.ne1c.gitteroid.api.GitterApi
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.base.NetworkService
import com.ne1c.gitteroid.models.data.MessageModel
import com.ne1c.gitteroid.ui.views.ChatView
import com.nhaarman.mockito_kotlin.capture
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.runners.MockitoJUnitRunner
import rx.Observable

@RunWith(MockitoJUnitRunner::class)
class TestChatRoomPresenter {

    @Mock
    lateinit var networkService: NetworkService

    @Mock
    lateinit var gitterApi: GitterApi

    @Mock
    lateinit var view: ChatView

    private val preferences = TestSharedPreferences()
    private lateinit var dataManager: DataManger
    private lateinit var presenter: ChatRoomPresenter

    @Before
    fun setup() {
        dataManager = DataManger(gitterApi, preferences)
        presenter = ChatRoomPresenter(TestExecutorService(), dataManager, networkService)

        presenter.bindView(view)
    }

    @Test
    fun sendMessage_withNetwork_success() {
        val messageBody = "test_text"
        val roomId = "test_room_id"

        `when`(networkService.isConnected()).thenReturn(true)
        `when`(dataManager.sendMessage(roomId, messageBody))
                .thenReturn(Observable.just(createMessageModel(messageBody)))

        presenter.sendMessage(roomId, messageBody)

        verify(view).deliveredMessage(capture {
            assert(it.id == roomId)
            assert(it.text == messageBody)
        })
    }

    @After
    fun end() {
        presenter.unbindView()
    }

    fun createMessageModel(text: String): MessageModel {
        val model: MessageModel = MessageModel()
        model.text = text

        return model
    }
}