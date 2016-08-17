package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.TestExecutorService
import com.ne1c.gitteroid.TestSharedPreferences
import com.ne1c.gitteroid.api.GitterApi
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.base.NetworkService
import com.ne1c.gitteroid.models.data.AuthResponseModel
import com.ne1c.gitteroid.ui.views.LoginView
import org.junit.After
import org.junit.Assert
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
    var networkService: NetworkService? = null

    @Mock
    var gitterApi: GitterApi? = null

    @Mock
    var view: LoginView? = null

    private var dataManager: DataManger? = null
    private var loginPresenter: LoginPresenter? = null
    private var prefs: TestSharedPreferences = TestSharedPreferences()

    @Before
    fun setup() {
        dataManager = DataManger(gitterApi!!, prefs)

        loginPresenter = LoginPresenter(TestExecutorService(), dataManager!!, networkService!!)
    }

    @Test
    fun bindView_isAuth() {
        prefs.edit()
                .putString(DataManger.ACCESS_TOKEN_PREF_KEY, "token")
                .commit()

        loginPresenter?.bindView(view)
        Mockito.verify(view)?.successAuth()
    }

    @Test
    fun bindView_noAuth() {
        loginPresenter?.bindView(view)
        Mockito.verify(view, Mockito.never())?.successAuth()
    }

    @Test
    fun loadAccessToken_withoutNetwork() {
        loginPresenter?.bindView(view)
        loginPresenter?.loadAccessToken(CODE)

        Mockito.verify(view)?.errorAuth(R.string.no_network)
    }

    @Test
    fun loadAccessToken_withNetwork_success() {
        loginPresenter?.bindView(view)
        Mockito.`when`(networkService?.isConnected()).thenReturn(true)

        val response = AuthResponseModel()
        response.access_token = "access_token"
        response.expires_in = "expires_in"
        response.token_type = "token_type"

        Mockito.`when`(gitterApi?.authorization(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Observable.just(response))

        loginPresenter?.loadAccessToken(CODE)

        Mockito.verify(view)?.showProgress()
        Mockito.verify(view)?.hideProgress()
        Mockito.verify(view)?.successAuth()
        Mockito.verify(view, Mockito.never())?.errorAuth(Mockito.anyInt())

        Assert.assertEquals(prefs.getString(DataManger.ACCESS_TOKEN_PREF_KEY, ""), "access_token")
        Assert.assertEquals(prefs.getString(DataManger.EXPIRES_IN_PREF_KEY, ""), "expires_in")
        Assert.assertEquals(prefs.getString(DataManger.TOKEN_TYPE_PREF_KEY, ""), "token_type")
    }

    @Test
    fun loadAccessToken_withNetwork_fail() {
        loginPresenter?.bindView(view)

        Mockito.`when`(networkService?.isConnected()).thenReturn(true)
        Mockito.`when`(gitterApi?.authorization(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Observable.error(Throwable()))

        loginPresenter?.loadAccessToken(CODE)

        Mockito.verify(view)?.showProgress()
        Mockito.verify(view)?.hideProgress()
        Mockito.verify(view)?.errorAuth(Mockito.anyInt())
    }

    @After
    fun end() {
        prefs.edit().clear().commit()
        
        loginPresenter?.unbindView()
    }
}