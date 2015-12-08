package com.ne1c.developerstalk.ui.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
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
import android.widget.Toast;

import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.presenters.LoginPresenter;
import com.ne1c.developerstalk.ui.DrawShadowFrameLayout;
import com.ne1c.developerstalk.ui.views.LoginView;
import com.ne1c.developerstalk.utils.UIUtils;
import com.ne1c.developerstalk.utils.Utils;

public class LoginActivity extends AppCompatActivity implements LoginView {
    private Button mAuthBut;
    private ImageView mLogoImg;
    private WebView mAuthWebView;
    private ProgressDialog mDialog;

    private LoginPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAuthBut = (Button) findViewById(R.id.auth_but);
        mLogoImg = (ImageView) findViewById(R.id.logo_img);
        mAuthWebView = (WebView) findViewById(R.id.auth_webView);

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

        mPresenter = new LoginPresenter();
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
    public void showDialog() {
        if (mDialog == null) {
            mDialog = new ProgressDialog(LoginActivity.this);
            mDialog.setIndeterminate(true);
            mDialog.setMessage(getString(R.string.loading));
        }

        mDialog.show();
    }

    @Override
    public void dismissDialog() {
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @Override
    public void successAuth() {
        startActivity(new Intent(getApplicationContext(), RoomsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    @Override
    public void errorAuth(String error) {
        Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT).show();

        if (mAuthWebView.getVisibility() == View.VISIBLE) {
            mAuthBut.setVisibility(View.VISIBLE);
            mLogoImg.setVisibility(View.VISIBLE);

            mAuthWebView.setVisibility(View.GONE);
        }
    }

    private class MyWebViewClient extends WebViewClient {
        private boolean startLoadToken = false;

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (url.equals(mPresenter.getAuthUrl())) {
                showDialog();
            }

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

            dismissDialog();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);

            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.unbindView();
    }
}
