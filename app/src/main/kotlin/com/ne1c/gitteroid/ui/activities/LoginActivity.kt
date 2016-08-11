package com.ne1c.gitteroid.ui.activities

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.Toolbar
import android.view.View
import android.webkit.WebChromeClient
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
    private var mAuthBut: Button? = null
    private var mLogoImg: ImageView? = null
    private var mAuthWebView: WebView? = null
    private var mProgressBar: MaterialProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        mAuthBut = findViewById(R.id.auth_but) as Button
        mLogoImg = findViewById(R.id.logo_img) as ImageView
        mAuthWebView = findViewById(R.id.auth_webView) as WebView
        mProgressBar = findViewById(R.id.progress_bar) as MaterialProgressBar

        mProgressBar?.useIntrinsicPadding = false

        mAuthWebView?.settings?.cacheMode = WebSettings.LOAD_NO_CACHE

        mAuthBut?.setOnClickListener { v ->
            if (DependencyManager.INSTANCE.networkService?.isConnected()!!) {
                mAuthBut?.visibility = View.GONE
                mLogoImg?.visibility = View.GONE

                mAuthWebView?.visibility = View.VISIBLE

                mAuthWebView?.settings?.javaScriptEnabled = true
                mAuthWebView?.settings?.saveFormData = false
                mAuthWebView?.setWebViewClient(MyWebViewClient())
                mAuthWebView?.setWebChromeClient(WebChromeClient())
                mAuthWebView?.loadUrl(mPresenter?.authUrl)
            } else {
                Snackbar.make(window.decorView.findViewById(android.R.id.content),
                        R.string.no_network,
                        Snackbar.LENGTH_SHORT).show()
            }
        }

        mPresenter.bindView(this)
    }

    override fun onBackPressed() {
        if (mAuthWebView?.visibility == View.VISIBLE && mAuthWebView?.canGoBack()!!) {
            mAuthWebView?.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun showProgress() {
        mProgressBar?.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        mProgressBar?.visibility = View.GONE
    }

    override fun successAuth() {
        startActivity(Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    override fun errorAuth(resId: Int) {
        Toast.makeText(applicationContext, resId, Toast.LENGTH_SHORT).show()

        if (mAuthWebView?.visibility == View.VISIBLE) {
            mAuthBut?.visibility = View.VISIBLE
            mLogoImg?.visibility = View.VISIBLE

            mAuthWebView?.visibility = View.GONE
        }
    }

    private inner class MyWebViewClient : WebViewClient() {
        private var startLoadToken = false

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
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
