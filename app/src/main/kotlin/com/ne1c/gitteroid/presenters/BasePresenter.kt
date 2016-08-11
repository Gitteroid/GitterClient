package com.ne1c.gitteroid.presenters

abstract class BasePresenter<in T> {
    abstract fun bindView(view: T)

    abstract fun unbindView()

    abstract fun onDestroy()
}
