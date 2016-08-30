package com.ne1c.gitteroid.ui.activities

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.support.v7.widget.Toolbar
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.ne1c.gitteroid.R
import com.ne1c.gitteroid.di.DependencyManager
import com.ne1c.gitteroid.presenters.LoginPresenter
import com.ne1c.gitteroid.ui.views.LoginView
import com.ne1c.rainbowmvp.base.BaseActivity
import me.zhanghai.android.materialprogressbar.MaterialProgressBar

class LoginActivity : BaseActivity<LoginPresenter>(), LoginView {
    private var authBut: Button? = null
    private var logoImageView: ImageView? = null
    private var authWebView: WebView? = null
    private var progressBar: MaterialProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setSupportActionBar(findViewById(R.id.toolbar) as Toolbar)

        authBut = findViewById(R.id.auth_but) as Button
        logoImageView = findViewById(R.id.logo_img) as ImageView
        authWebView = findViewById(R.id.auth_webView) as WebView
        progressBar = findViewById(R.id.progress_bar) as MaterialProgressBar

        progressBar?.useIntrinsicPadding = false

        authWebView?.settings?.cacheMode = WebSettings.LOAD_NO_CACHE

        authBut?.setOnClickListener {
            if (DependencyManager.INSTANCE.networkService!!.isConnected()) {
                authBut?.visibility = View.GONE
                logoImageView?.visibility = View.GONE

                authWebView?.visibility = View.VISIBLE

                authWebView?.settings?.javaScriptEnabled = true
                authWebView?.settings?.saveFormData = false
                authWebView?.setWebViewClient(MyWebViewClient())
                authWebView?.loadUrl(LoginPresenter.AUTH_URL)
            } else {
                val parentView = window.decorView.findViewById(android.R.id.content)
                Snackbar.make(parentView, R.string.no_network, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        mPresenter.bindView(this)
    }

    override fun onStop() {
        mPresenter.unbindView()

        super.onStop()
    }

    override fun onBackPressed() {
        if (authWebView?.visibility == View.VISIBLE && authWebView?.canGoBack()!!) {
            authWebView?.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun showProgress() {
        progressBar?.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        progressBar?.visibility = View.GONE
    }

    override fun successAuth() {
        authWebView?.visibility = View.GONE

        startActivity(Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    override fun errorAuth(@StringRes resId: Int) {
        Toast.makeText(applicationContext, resId, Toast.LENGTH_SHORT).show()

        if (authWebView?.visibility == View.VISIBLE) {
            authBut?.visibility = View.VISIBLE
            logoImageView?.visibility = View.VISIBLE

            authWebView?.visibility = View.GONE
        }
    }

    private inner class MyWebViewClient : WebViewClient() {
        private var startLoadToken = false

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            showProgress()

            // If the authorization is successful, then get access_token
            // startLoadToken check that don't run second request
            if (url.contains("code=") && !url.contains("state=") && !startLoadToken) {
                view.stopLoading()

                startLoadToken = true
                // Get access token and show MainActivity
                mPresenter?.loadAccessToken(url.substring(url.indexOf('=') + 1, url.length))
            } else if (!url.contains("about/:blank")) {
                super.onPageStarted(view, url, favicon)
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)

            hideProgress()
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            view.loadUrl(url)

            return super.shouldOverrideUrlLoading(view, url)
        }
    }

    override fun getPresenterTag(): String = LoginPresenter.TAG
}
