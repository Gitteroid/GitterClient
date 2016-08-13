package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.TestExecutorService
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.base.NetworkService
import com.ne1c.gitteroid.models.data.AuthResponseModel
import com.ne1c.gitteroid.ui.views.LoginView
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.runners.MockitoJUnitRunner
import rx.Observable

@RunWith(MockitoJUnitRunner::class)
class TestLoginPresenter {
    private val CODE = "random_code"

    @Mock
    var dataManager: DataManger? = null

    @Mock
    var networkService: NetworkService? = null

    @Mock
    var view: LoginView? = null

    private var loginPresenter: LoginPresenter? = null

    @Before
    fun setup() {
        loginPresenter = LoginPresenter(TestExecutorService(), dataManager!!, networkService!!)
    }

    @Test
    fun test_isAuth_bindView() {
        Mockito.`when`(dataManager?.isAuthorize()).thenReturn(true)

        loginPresenter!!.bindView(view!!)
        Mockito.verify(view)?.successAuth()
    }

    @Test
    fun test_noAuth_bindView() {
        Mockito.`when`(dataManager?.isAuthorize()).thenReturn(true)

        loginPresenter!!.bindView(view!!)
        Mockito.verify(view, Mockito.never())?.successAuth()
    }

    @Test
    fun test_loadAccessToken_withoutNetwork() {
        Mockito.`when`(networkService?.isConnected()).thenReturn(false)

        loginPresenter?.loadAccessToken(Mockito.anyString())

        Mockito.verify(view)?.errorAuth(R.string.no_network)
    }

    @Test
    fun test_loadAccessToken_withNetwork_success() {
        Mockito.`when`(networkService?.isConnected()).thenReturn(true)

        val response = AuthResponseModel()
        Mockito.`when`(dataManager?.authorization(Mockito.anyString(), Mockito.anyString(), Mockito.eq(CODE), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Observable.just(response))

        loginPresenter?.loadAccessToken(CODE)

        Mockito.verify(view)?.showProgress()
        Mockito.verify(dataManager)?.saveUser(response)
        Mockito.verify(view)?.hideProgress()
        Mockito.verify(view)?.successAuth()
        Mockito.verify(view, Mockito.never())?.errorAuth(Mockito.anyInt())
    }

    @Test
    fun test_loadAccessToken_withNetwork_fail() {
        Mockito.`when`(networkService?.isConnected()).thenReturn(true)

        Mockito.`when`(dataManager?.authorization(Mockito.anyString(), Mockito.anyString(), Mockito.eq(CODE), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Observable.error(Throwable()))

        loginPresenter?.loadAccessToken(CODE)

        Mockito.verify(view)?.showProgress()
        Mockito.verify(view)?.hideProgress()
        Mockito.verify(view)?.errorAuth(Mockito.anyInt())
    }

    @After
    fun end() {
        loginPresenter?.unbindView()
    }
}