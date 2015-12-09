package com.ne1c.developerstalk.presenters;

import com.ne1c.developerstalk.api.GitterApi;
import com.ne1c.developerstalk.ui.views.BaseView;
import com.ne1c.developerstalk.ui.views.LoginView;
import com.ne1c.developerstalk.utils.Utils;

import retrofit.RestAdapter;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class LoginPresenter implements BasePresenter {
    private final String CLIENT_ID = "247736d87aa0134a33f73b00cc47b18165296e9e";
    private final String CLIENT_SECRET = "238ca926e7e59121ceb7c46c3a2c91eafe51b6c5";
    private final String GRANT_TYPE = "authorization_code";
    private final String REDIRECT_URL = "http://about:blank";
    private final String RESPONSE_TYPE = "code";

    public final String AUTH_URL = "https://gitter.im/login/oauth/authorize?"
            + "client_id=" + CLIENT_ID + "&response_type=" + RESPONSE_TYPE + "&redirect_uri=" + REDIRECT_URL;

    private LoginView mView;

    @Override
    public void bindView(BaseView view) {
        mView = (LoginView) view;
    }

    @Override
    public void unbindView() {
        mView = null;
    }

    public String getAuthUrl() {
        return AUTH_URL;
    }

    public void loadAccessToken(String code) {
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(Utils.GITTER_URL)
                .build();

        GitterApi api = adapter.create(GitterApi.class);

        mView.showDialog();

        api.authorization(CLIENT_ID, CLIENT_SECRET, code,
                GRANT_TYPE, REDIRECT_URL)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(authResponseModel -> {
                    // Write access token to preferences
                    Utils.getInstance().writeAuthResponsePref(authResponseModel);
                    mView.dismissDialog();
                    mView.successAuth();
                }, error -> {
                    // If error, then set visible "Sign In" button
                    mView.dismissDialog();
                    mView.errorAuth(error.getMessage());
                });
    }

}
