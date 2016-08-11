package com.ne1c.gitteroid.presenters

import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.dataproviders.DataManger
import com.ne1c.gitteroid.di.base.ExecutorService
import com.ne1c.gitteroid.di.base.NetworkService
import com.ne1c.gitteroid.ui.views.LoginView
import com.ne1c.rainbowmvp.base.BasePresenter
import rx.subscriptions.CompositeSubscription

class LoginPresenter(private val executor: ExecutorService,
                     private val mDataManager: DataManger,
                     private val networkService: NetworkService) : BasePresenter<LoginView>() {
    companion object {
        val TAG = LoginPresenter::class.java.simpleName
    }

    private val CLIENT_ID = "e94f6920cfbc194174a942fc1a5541355b124309"
    private val CLIENT_SECRET = "bb702baf80daabf7809dc4244f46f37252130c5c"
    private val GRANT_TYPE = "authorization_code"
    private val REDIRECT_URL = "http://about:blank"
    private val RESPONSE_TYPE = "code"

    val authUrl = "https://gitter.im/login/oauth/authorize?client_id=$CLIENT_ID&response_type=$RESPONSE_TYPE&redirect_uri=$REDIRECT_URL"

    private var mSubscriptions: CompositeSubscription? = null

    override fun bindView(view: LoginView) {
        super.bindView(view)
        mSubscriptions = CompositeSubscription()
    }

    override fun onDestroy() {
        mSubscriptions?.unsubscribe()
    }

    fun loadAccessToken(code: String) {
        if (!networkService.isConnected()) {
            mView?.errorAuth(R.string.no_network)
            return
        }

        mView?.showProgress()

        val sub = mDataManager.authorization(CLIENT_ID, CLIENT_SECRET, code, GRANT_TYPE, REDIRECT_URL)
                .map({ mDataManager.saveUser(it) })
                .subscribeOn(executor.getSubscribeOn())
                .observeOn(executor.getObserveOn())
                .subscribe({
                    // Write access token to preferences
                    mView?.hideProgress()
                    mView?.successAuth()
                }, { throwable ->
                    // If error, then set visible "Sign In" button
                    mView?.hideProgress()
                    mView?.errorAuth(R.string.error_auth)
                })

        mSubscriptions?.add(sub)
    }
}