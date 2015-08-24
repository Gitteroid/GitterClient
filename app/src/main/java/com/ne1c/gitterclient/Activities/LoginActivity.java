package com.ne1c.gitterclient.Activities;


import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;

import com.ne1c.gitterclient.R;
import com.ne1c.gitterclient.Utils;


public class LoginActivity extends AppCompatActivity {

    private final String CLIENT_ID = "4da42012698bf284a2eb9a47a351f52df0b666d4";
    private final String REDIRECT_URL = "http://gitter.im";
    private final String RESPONSE_TYPE = "code";

    public final String AUTH_URL = "https://gitter.im/login/oauth/authorize?"
            + "client_id=" + CLIENT_ID + "&response_type=" + RESPONSE_TYPE + "&redirect_uri=" + REDIRECT_URL;

    private Button mAuthBut;
    private ImageView mLogoImg;
    private WebView mAuthWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuthBut = (Button) findViewById(R.id.auth_but);
        mLogoImg = (ImageView) findViewById(R.id.logo_img);
        mAuthWebView = (WebView) findViewById(R.id.auth_webView);

        mAuthBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuthBut.setVisibility(View.GONE);
                mLogoImg.setVisibility(View.GONE);

                mAuthWebView.setVisibility(View.VISIBLE);

                mAuthWebView.getSettings().setJavaScriptEnabled(true);
                mAuthWebView.setWebViewClient(new MyWebViewClient());
                mAuthWebView.setWebChromeClient(new WebChromeClient());
                mAuthWebView.loadUrl(AUTH_URL);
            }
        });
    }

    private class MyWebViewClient extends WebViewClient {

        private ProgressDialog mDialog = new ProgressDialog(LoginActivity.this);

        public MyWebViewClient() {
            mDialog.setIndeterminate(true);
            mDialog.setMessage("Loading...");
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (url.equals(AUTH_URL)) {
                mDialog.show();
            }

            // Если прошел авторизацию и url содержит параметр code,
            // то запускаем MainActivity
            if (url.contains("code=")) {
                view.stopLoading();
                view.destroy();
                Utils.getInstance().writeStateAuthPref(getApplicationContext(), true);

                startActivity(new Intent(getApplicationContext(), MainActivity.class)
                        .putExtra("auth", true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            } else {
                super.onPageStarted(view, url, favicon);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);

            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    @Override
    public void onBackPressed() {
        if (mAuthWebView.getVisibility() == View.VISIBLE && mAuthWebView.canGoBack()) {
            mAuthWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
