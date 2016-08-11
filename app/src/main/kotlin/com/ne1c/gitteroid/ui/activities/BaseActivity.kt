package com.ne1c.gitteroid.ui.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.ne1c.gitteroid.GitteroidApplication
import com.ne1c.gitteroid.di.components.ApplicationComponent

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        initDiComponent()
        super.onCreate(savedInstanceState)
    }

    protected abstract fun initDiComponent()

    protected val appComponent: ApplicationComponent
        get() = (application as GitteroidApplication).component
}
