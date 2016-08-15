package com.ne1c.gitteroid.di

import com.ne1c.gitteroid.presenters.ChatRoomPresenter
import com.ne1c.gitteroid.presenters.LoginPresenter
import com.ne1c.gitteroid.presenters.MainPresenter
import com.ne1c.gitteroid.presenters.SearchRoomPresenter
import com.ne1c.rainbowmvp.PresenterStorage
import com.ne1c.rainbowmvp.base.BasePresenter

class PresenterStorage : PresenterStorage {
    override fun create(tag: String): BasePresenter<*>? {
        if (tag.equals(ChatRoomPresenter.TAG)) {
            return ChatRoomPresenter(DependencyManager.INSTANCE.executorService,
                    DependencyManager.INSTANCE.dataManager,
                    DependencyManager.INSTANCE.networkService)
        }

        if (tag.equals(LoginPresenter.TAG)) {
            return LoginPresenter(DependencyManager.INSTANCE.executorService,
                    DependencyManager.INSTANCE.dataManager,
                    DependencyManager.INSTANCE.networkService)
        }

        if (tag.equals(MainPresenter.TAG)) {
            return MainPresenter(DependencyManager.INSTANCE.executorService,
                    DependencyManager.INSTANCE.dataManager,
                    DependencyManager.INSTANCE.networkService)
        }

        if (tag.equals(SearchRoomPresenter.TAG)) {
            return SearchRoomPresenter(DependencyManager.INSTANCE.executorService,
                    DependencyManager.INSTANCE.dataManager)
        }

        return null
    }
}