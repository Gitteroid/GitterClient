package com.ne1c.developerstalk.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.ne1c.developerstalk.DrawShadowFrameLayout;
import com.ne1c.developerstalk.Models.AuthResponseModel;
import com.ne1c.developerstalk.R;
import com.ne1c.developerstalk.RetrofitServices.IApiMethods;
import com.ne1c.developerstalk.Util.UIUtils;
import com.ne1c.developerstalk.Util.Utils;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LoginActivity extends AppCompatActivity {
    private final String CLIENT_ID = "247736d87aa0134a33f73b00cc47b18165296e9e";
    private final String CLIENT_SECRET = "238ca926e7e59121ceb7c46c3a2c91eafe51b6c5";
    private final String REDIRECT_URL = "http://about:blank";
    private final String RESPONSE_TYPE = "code";

    public final String AUTH_URL = "https://gitter.im/login/oauth/authorize?"
            + "client_id=" + CLIENT_ID + "&response_type=" + RESPONSE_TYPE + "&redirect_uri=" + REDIRECT_URL;

    private Toolbar mToolbar;
    private Button mAuthBut;
    private ImageView mLogoImg;
    private WebView mAuthWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mAuthBut = (Button) findViewById(R.id.auth_but);
        mLogoImg = (ImageView) findViewById(R.id.logo_img);
        mAuthWebView = (WebView) findViewById(R.id.auth_webView);

        mAuthBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.getInstance().isNetworkConnected()) {
                    mAuthBut.setVisibility(View.GONE);
                    mLogoImg.setVisibility(View.GONE);

                    mAuthWebView.setVisibility(View.VISIBLE);

                    mAuthWebView.getSettings().setJavaScriptEnabled(true);
                    mAuthWebView.getSettings().setSaveFormData(false);
                    mAuthWebView.setWebViewClient(new MyWebViewClient());
                    mAuthWebView.setWebChromeClient(new WebChromeClient());
                    mAuthWebView.loadUrl(AUTH_URL);
                } else {
                    Snackbar.make(getWindow().getDecorView().findViewById(android.R.id.content), R.string.no_network, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
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

    private void loadAccessToken(String code) {
        final ProgressDialog dialog = new ProgressDialog(LoginActivity.this);
        dialog.setIndeterminate(true);
        dialog.setMessage(getString(R.string.loading));

        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(Utils.GITTER_URL)
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

                        startActivity(new Intent(getApplicationContext(), RoomsActivity.class)
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

    private class MyWebViewClient extends WebViewClient {
        private ProgressDialog dialog;
        private boolean startLoadToken = false;

        public MyWebViewClient() {
            dialog = new ProgressDialog(LoginActivity.this);
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
                view.destroy();

                startLoadToken = true;
                // Get access token and show MainActivity
                loadAccessToken(url.substring(url.indexOf('=') + 1, url.length()));
            } else if (!url.contains("about/:blank")) {
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
}
