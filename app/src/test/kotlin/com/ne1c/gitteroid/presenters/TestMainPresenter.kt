package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.TestExecutorService
import com.ne1c.gitteroid.TestSharedPreferences
import com.ne1c.gitteroid.api.GitterApi
import com.ne1c.gitteroid.api.responses.StatusResponse
import com.ne1c.gitteroid.dataproviders.ClientDatabase
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.base.NetworkService
import com.ne1c.gitteroid.models.data.UserModel
import com.ne1c.gitteroid.ui.views.MainView
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.runners.MockitoJUnitRunner
import rx.Observable

@RunWith(MockitoJUnitRunner::class)
class TestMainPresenter {
    @Mock
    var view: MainView? = null

    @Mock
    var gitterApi: GitterApi? = null

    @Mock
    var networkService: NetworkService? = null

    @Mock
    var database: ClientDatabase? = null

    private var presenter: MainPresenter? = null
    private var dataManager: DataManger? = null
    private var prefs = TestSharedPreferences()

    @Before
    fun setup() {
        dataManager = Mockito.spy(DataManger(gitterApi!!, database!!, prefs))
        presenter = MainPresenter(TestExecutorService(), dataManager!!, networkService!!)
        presenter?.bindView(view)
    }

    @Test
    fun loadProfile_withNetwork_success() {
        val userModel = UserModel()

        Mockito.`when`(networkService?.isConnected()).thenReturn(true)
        Mockito.`when`(gitterApi?.getCurrentUser(Mockito.anyString())).thenReturn(Observable.just(arrayListOf(userModel)))

        presenter?.loadProfile()

        Mockito.verify(view)?.showProfile(userModel)
        Mockito.verify(view, Mockito.never())?.showError(Mockito.anyInt())
    }

    @Test
    fun loadProfile_withNetwork_failed() {
        Mockito.`when`(networkService?.isConnected()).thenReturn(true)
        Mockito.`when`(gitterApi?.getCurrentUser(Mockito.anyString())).thenReturn(Observable.error(Throwable()))

        presenter?.loadProfile()

        Mockito.verify(view)?.showProfile(UserModel())
        Mockito.verify(view, Mockito.never())?.showError(Mockito.anyInt())
    }

    @Test
    fun loadProfile_noNetwork_success() {
        Mockito.`when`(networkService?.isConnected()).thenReturn(false)
    }

    @Test
    fun loadProfile_noNetwork_failed() {
        Mockito.`when`(networkService?.isConnected()).thenReturn(false)
        Mockito.`when`(gitterApi?.getCurrentUser(Mockito.anyString())).thenReturn(Observable.error(Throwable()))

        presenter?.loadProfile()

        Mockito.verify(view, Mockito.never())?.showProfile(createUserProfile())
        Mockito.verify(view)?.showError(Mockito.anyInt())
    }

    @Test
    fun leaveFromRoom_noNetwork_failed() {
        Mockito.`when`(networkService?.isConnected()).thenReturn(false)

        presenter?.leaveFromRoom("room_id")

        Mockito.verify(view, Mockito.never())?.leavedFromRoom()
        Mockito.verify(view)?.showError(Mockito.anyInt())
    }

    @Test
    fun leaveFromRoom_withNetwork_failed() {
        Mockito.`when`(networkService?.isConnected()).thenReturn(true)

        val response = StatusResponse()
        response.success = false

        Mockito.`when`(gitterApi?.leaveRoom(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Observable.just(response))
        Mockito.`when`(dataManager?.leaveFromRoom("room_id")).thenReturn(Observable.just(true))

        presenter?.leaveFromRoom("room_id")

        Mockito.verify(view, Mockito.never())?.leavedFromRoom()
        Mockito.verify(view)?.showError(Mockito.anyInt())
    }

    @Test
    fun leaveFromRoom_withNetwork_success() {
        Mockito.`when`(networkService?.isConnected()).thenReturn(true)

        val roomId = "room_id"
        val response = StatusResponse()
        response.success = true

        Mockito.`when`(gitterApi?.leaveRoom(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Observable.just(response))
        Mockito.`when`(dataManager?.leaveFromRoom(roomId)).thenReturn(Observable.just(true))

        presenter?.leaveFromRoom(roomId)

        Mockito.verify(view)?.leavedFromRoom()
        Mockito.verify(view, Mockito.never())?.showError(Mockito.anyInt())
    }

    @After
    fun end() {
        prefs.edit().clear().commit()
        presenter?.unbindView()
    }

    private fun createUserProfile(): UserModel {
        val user = UserModel(id = "test_id", username = "Ne1c")

        return user
    }
}