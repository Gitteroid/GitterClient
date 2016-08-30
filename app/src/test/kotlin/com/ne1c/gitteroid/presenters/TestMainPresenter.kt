package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.TestExecutorService
import com.ne1c.gitteroid.TestSharedPreferences
import com.ne1c.gitteroid.api.GitterApi
import com.ne1c.gitteroid.api.responses.StatusResponse
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.base.NetworkService
import com.ne1c.gitteroid.models.data.RoomModel
import com.ne1c.gitteroid.models.data.UserModel
import com.ne1c.gitteroid.ui.views.MainView
import com.nhaarman.mockito_kotlin.capture
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.runners.MockitoJUnitRunner
import rx.Observable
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class TestMainPresenter {
    @Mock
    var view: MainView? = null

    @Mock
    var gitterApi: GitterApi? = null

    @Mock
    var networkService: NetworkService? = null

    private var presenter: MainPresenter? = null
    private var dataManager: DataManger? = null
    private var prefs = TestSharedPreferences()

    @Before
    fun setup() {
        dataManager = spy(DataManger(gitterApi!!, prefs))
        presenter = MainPresenter(TestExecutorService(), dataManager!!, networkService!!)
        presenter?.bindView(view)
    }

    @Test
    fun loadProfile_withNetwork_success() {
        val userModel = UserModel()

        `when`(networkService?.isConnected()).thenReturn(true)
        `when`(gitterApi?.getCurrentUser(anyString())).thenReturn(Observable.just(arrayListOf(userModel)))

        presenter?.loadProfile()

        verify(view)?.showProfile(userModel)
        verify(view, never())?.showError(anyInt())
    }

    @Test
    fun loadProfile_withNetwork_failed() {
        `when`(networkService?.isConnected()).thenReturn(true)
        `when`(gitterApi?.getCurrentUser(anyString())).thenReturn(Observable.error(Throwable()))

        presenter?.loadProfile()

        verify(view)?.showProfile(UserModel())
        verify(view, never())?.showError(anyInt())
    }

    @Test
    fun loadProfile_noNetwork_success() {
        `when`(networkService?.isConnected()).thenReturn(false)
    }

    @Test
    fun loadProfile_noNetwork_failed() {
        `when`(networkService?.isConnected()).thenReturn(false)
        `when`(gitterApi?.getCurrentUser(anyString())).thenReturn(Observable.error(Throwable()))

        presenter?.loadProfile()

        verify(view, never())?.showProfile(createUserProfile())
        verify(view)?.showError(anyInt())
    }

    @Test
    fun leaveFromRoom_noNetwork_failed() {
        `when`(networkService?.isConnected()).thenReturn(false)

        presenter?.leaveFromRoom("room_id")

        verify(view, never())?.leavedFromRoom()
        verify(view)?.showError(anyInt())
    }

    @Test
    fun leaveFromRoom_withNetwork_failed() {
        `when`(networkService?.isConnected()).thenReturn(true)

        val response = StatusResponse()
        response.success = false

        `when`(gitterApi?.leaveRoom(anyString(), anyString(), anyString()))
                .thenReturn(Observable.just(response))
        `when`(dataManager?.leaveFromRoom("room_id")).thenReturn(Observable.just(true))

        presenter?.leaveFromRoom("room_id")

        verify(view, never())?.leavedFromRoom()
        verify(view)?.showError(anyInt())
    }

    @Test
    fun leaveFromRoom_withNetwork_success() {
        `when`(networkService?.isConnected()).thenReturn(true)

        val roomId = "room_id"
        val response = StatusResponse()
        response.success = true

        `when`(gitterApi?.leaveRoom(anyString(), anyString(), anyString()))
                .thenReturn(Observable.just(response))

        presenter?.leaveFromRoom(roomId)

        verify(view)?.leavedFromRoom()
        verify(view, never())?.showError(anyInt())
    }

    @Test
    fun loadRooms_withNetwork_fresh_success_with_5_unreadRooms() {
        `when`(networkService?.isConnected()).thenReturn(true)
        `when`(gitterApi?.getCurrentUserRooms(anyString())).thenReturn(Observable.just(generateRooms(unreadCountRooms = 5)))

        presenter?.loadRooms(true)

        verify(view)?.showRooms(capture {
            assert(it.size == 5)
            for (room in it) {
                assert(room.unreadItems > 0)
            }
        })

        verify(view)?.saveAllRooms(capture {
            assert(it.size == 5)
            for (room in it) {
                assert(room.unreadItems > 0)
            }
        })

        verify(view, never())?.errorLoadRooms()
        verify(view, never())?.showError(anyInt())
    }

    @Test
    fun loadRooms_withNetwork_fresh_success_with_noUnreadRooms() {
        `when`(networkService?.isConnected()).thenReturn(true)
        `when`(gitterApi?.getCurrentUserRooms(anyString())).thenReturn(Observable.just(generateRooms()))

        presenter?.loadRooms(true)

        verify(view)?.showRooms(capture {
            assert(it.size == 4)
            for (room in it) {
                assert(room.unreadItems == 0)
            }
        })

        verify(view)?.saveAllRooms(capture {
            assert(it.size == 4)
            for (room in it) {
                assert(room.unreadItems == 0)
            }
        })

        verify(view, never())?.errorLoadRooms()
        verify(view, never())?.showError(anyInt())
    }

    @Test
    fun loadRooms_withNetwork_fresh_success_with_oneToOneRooms() {
        `when`(networkService?.isConnected()).thenReturn(true)
        `when`(gitterApi?.getCurrentUserRooms(anyString())).thenReturn(Observable.just(generateOneToOneRooms()))

        presenter?.loadRooms(true)

        verify(view)?.showRooms(capture {
            assert(it.size == 4)
            for (room in it) {
                assert(room.oneToOne == true)
            }
        })

        verify(view)?.saveAllRooms(capture {
            assert(it.size == 4)
            for (room in it) {
                assert(room.oneToOne == true)
            }
        })

        verify(view, never())?.errorLoadRooms()
        verify(view, never())?.showError(anyInt())
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

    private fun generateRooms(countRooms: Int = 10, unreadCountRooms: Int = 0): ArrayList<RoomModel> {
        val rooms = ArrayList<RoomModel>(countRooms)

        // Generate rooms with unread messages
        for (i: Int in 1..unreadCountRooms) {
            val room = RoomModel()
            room.unreadItems = (Math.random() + 1).toInt()

            rooms.add(room)
        }

        // Generate default rooms
        for (i: Int in 1..(countRooms - unreadCountRooms)) {
            val room = RoomModel()
            rooms.add(room)
        }

        return rooms
    }

    private fun generateOneToOneRooms(oneToOne: Boolean = true,
                                      countRooms: Int = 10,
                                      countOneToOneRooms: Int = 10): ArrayList<RoomModel> {
        val rooms = generateRooms(countRooms = countRooms)

        for (i: Int in 0..countOneToOneRooms - 1) {
            rooms[i].oneToOne = oneToOne
        }

        return rooms
    }
}