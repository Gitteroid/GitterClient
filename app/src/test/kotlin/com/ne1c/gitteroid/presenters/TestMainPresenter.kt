package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.TestExecutorService
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.base.NetworkService
import com.ne1c.gitteroid.ui.views.MainView
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TestMainPresenter {
    @Mock
    var view: MainView? = null

    @Mock
    var dataManager: DataManger? = null

    @Mock
    var networkService: NetworkService? = null

    private var presenter: MainPresenter? = null

    @Before
    fun setup() {
        presenter = MainPresenter(TestExecutorService(), dataManager!!, networkService!!)
        presenter?.bindView(view)
    }

    @After
    fun end() {
        presenter?.unbindView()
    }
}