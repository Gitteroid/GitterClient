package com.ne1c.gitterclient.Activities;


import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.ne1c.gitterclient.Models.AuthResponseModel;
import com.ne1c.gitterclient.R;
import com.ne1c.gitterclient.RetrofitServices.IApiMethods;
import com.ne1c.gitterclient.Services.NewMessagesService;
import com.ne1c.gitterclient.Utils;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LoginActivity extends AppCompatActivity {

    private final String CLIENT_ID = "3194d42d0dba207e2a76b47307d7c77d60f537d2";
    private final String CLIENT_SECRET = "6099e41a1d14a612430a7bb1e1738b3259dacdde";
    private final String REDIRECT_URL = "https://github.com/Ne1c/GitterClient";
    private final String RESPONSE_TYPE = "code";

    public final String AUTH_URL = "https://gitter.im/login/oauth/authorize?"
            + "client_id=" + CLIENT_ID + "&response_type=" + RESPONSE_TYPE + "&redirect_uri=" + REDIRECT_URL;

    private Button mAuthBut;
    private ImageView mLogoImg;
    private WebView mAuthWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!TextUtils.isEmpty(Utils.getInstance().getAccessToken())) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
            startService(new Intent(getApplicationContext(), NewMessagesService.class));

            finish();
            return;
        }

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
                mAuthWebView.getSettings().setSaveFormData(false);
                mAuthWebView.setWebViewClient(new MyWebViewClient());
                mAuthWebView.setWebChromeClient(new WebChromeClient());
                mAuthWebView.loadUrl(AUTH_URL);
            }
        });
    }

    private class MyWebViewClient extends WebViewClient {

        private ProgressDialog dialog = new ProgressDialog(LoginActivity.this);
        private boolean startLoadToken = false;

        public MyWebViewClient() {
            dialog.setIndeterminate(true);
            dialog.setMessage(getString(R.string.loading));
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (url.equals(AUTH_URL)) {
                dialog.show();
            }

            // If the authorization is successful, then get access_token
            // startLoadToken check that don't run second request
            if (url.contains("code=") && !url.contains("state=") && !startLoadToken) {
                view.stopLoading();

                startLoadToken = true;
                // Get access token and show MainActivity
                loadAccessToken(url.substring(url.indexOf('=') + 1, url.length()));
            } else {
                super.onPageStarted(view, url, favicon);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            if (dialog.isShowing()) {
                dialog.dismiss();
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

    private void loadAccessToken(String code) {
        final ProgressDialog dialog = new ProgressDialog(LoginActivity.this);
        dialog.setIndeterminate(true);
        dialog.setMessage(getString(R.string.loading));

        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(Utils.getInstance().GITTER_URL)
                .build();

        IApiMethods methods = adapter.create(IApiMethods.class);

        dialog.show();
        methods.authorization(CLIENT_ID, CLIENT_SECRET, code,
                "authorization_code", REDIRECT_URL, new Callback<AuthResponseModel>() {
                    @Override
                    public void success(AuthResponseModel authResponseModel, Response response) {
                        // Write access token to preferences
                        Utils.getInstance().writeAuthResponsePref(authResponseModel);
                        dialog.dismiss();

                        startService(new Intent(getApplicationContext(), NewMessagesService.class));
                        startActivity(new Intent(getApplicationContext(), MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        finish();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        // If error, then set visible "Sign In" button
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();

                        if (mAuthWebView.getVisibility() == View.VISIBLE) {
                            mAuthBut.setVisibility(View.VISIBLE);
                            mLogoImg.setVisibility(View.VISIBLE);

                            mAuthWebView.setVisibility(View.GONE);
                        }
                    }
                });
    }
}
