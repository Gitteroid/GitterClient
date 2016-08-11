package com.ne1c.gitteroid.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment

import com.ne1c.gitteroid.GitteroidApplication
import com.ne1c.gitteroid.di.HasComponent
import com.ne1c.gitteroid.di.components.ApplicationComponent

abstract class BaseFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDiComponent()
    }

    protected abstract fun initDiComponent()

    fun <T> getActivityComponent(clazz: Class<T>): T {
        val activity = activity
        val has = activity as HasComponent<T>
        return has.component
    }

    val appComponent: ApplicationComponent
        get() = (activity.application as GitteroidApplication).component
}
