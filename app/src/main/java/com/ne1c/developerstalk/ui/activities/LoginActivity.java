package com.ne1c.developerstalk.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ne1c.developerstalk.Application;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.di.components.DaggerLoginComponent;
import com.ne1c.developerstalk.di.components.LoginComponent;
import com.ne1c.developerstalk.presenters.LoginPresenter;
import com.ne1c.developerstalk.ui.DrawShadowFrameLayout;
import com.ne1c.developerstalk.ui.views.LoginView;
import com.ne1c.developerstalk.utils.UIUtils;
import com.ne1c.developerstalk.utils.Utils;

import javax.inject.Inject;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class LoginActivity extends AppCompatActivity implements LoginView {
    private Button mAuthBut;
    private ImageView mLogoImg;
    private WebView mAuthWebView;
    private MaterialProgressBar mProgressBar;

    private LoginComponent mComponent;

    @Inject
    LoginPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mComponent = DaggerLoginComponent.builder()
                .applicationComponent(((Application) getApplication()).getComponent())
                .build();

        mComponent.inject(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAuthBut = (Button) findViewById(R.id.auth_but);
        mLogoImg = (ImageView) findViewById(R.id.logo_img);
        mAuthWebView = (WebView) findViewById(R.id.auth_webView);
        mProgressBar = (MaterialProgressBar) findViewById(R.id.progress_bar);

        mProgressBar.setUseIntrinsicPadding(false);

        mAuthBut.setOnClickListener(v -> {
            if (Utils.getInstance().isNetworkConnected()) {
                mAuthBut.setVisibility(View.GONE);
                mLogoImg.setVisibility(View.GONE);

                mAuthWebView.setVisibility(View.VISIBLE);

                mAuthWebView.getSettings().setJavaScriptEnabled(true);
                mAuthWebView.getSettings().setSaveFormData(false);
                mAuthWebView.setWebViewClient(new MyWebViewClient());
                mAuthWebView.setWebChromeClient(new WebChromeClient());
                mAuthWebView.loadUrl(mPresenter.getAuthUrl());
            } else {
                Snackbar.make(getWindow().getDecorView().findViewById(android.R.id.content),
                        R.string.no_network,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        });

        mPresenter.bindView(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        int actionBarSize = UIUtils.calculateActionBarSize(this);
        DrawShadowFrameLayout drawShadowFrameLayout =
                (DrawShadowFrameLayout) findViewById(R.id.shadow_layout);
        if (drawShadowFrameLayout != null) {
            drawShadowFrameLayout.setShadowTopOffset(actionBarSize);
        }
        UIUtils.setContentTopClearance(findViewById(R.id.content_layout), actionBarSize);
    }

    @Override
    public void onBackPressed() {
        if (mAuthWebView.getVisibility() == View.VISIBLE && mAuthWebView.canGoBack()) {
            mAuthWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void showProgress() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void successAuth() {
        startActivity(new Intent(getApplicationContext(), RoomsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    @Override
    public void errorAuth(int resId) {
        Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_SHORT).show();

        if (mAuthWebView.getVisibility() == View.VISIBLE) {
            mAuthBut.setVisibility(View.VISIBLE);
            mLogoImg.setVisibility(View.VISIBLE);

            mAuthWebView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        mPresenter.unbindView();
        mComponent = null;
        super.onDestroy();
    }

    private class MyWebViewClient extends WebViewClient {
        private boolean startLoadToken = false;

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            showProgress();

            // If the authorization is successful, then get access_token
            // startLoadToken check that don't run second request
            if (url.contains("code=") && !url.contains("state=") && !startLoadToken) {
                view.stopLoading();
                view.destroy();

                startLoadToken = true;
                // Get access token and show MainActivity
                mPresenter.loadAccessToken(url.substring(url.indexOf('=') + 1, url.length()));
            } else if (!url.contains("about/:blank")) {
                super.onPageStarted(view, url, favicon);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            hideProgress();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);

            return super.shouldOverrideUrlLoading(view, url);
        }
    }
}
