package com.ne1c.gitteroid.ui.views

import android.support.annotation.StringRes

interface LoginView {
    fun showProgress()

    fun hideProgress()

    fun successAuth()

    fun errorAuth(@StringRes resId: Int)
}
