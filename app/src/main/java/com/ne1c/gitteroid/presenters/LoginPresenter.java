package com.ne1c.gitteroid.presenters;

import com.ne1c.gitteroid.R;
import com.ne1c.gitteroid.dataproviders.DataManger;
import com.ne1c.gitteroid.ui.views.LoginView;
import com.ne1c.gitteroid.utils.RxSchedulersFactory;
import com.ne1c.gitteroid.utils.Utils;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class LoginPresenter extends BasePresenter<LoginView> {
    private final String CLIENT_ID = "247736d87aa0134a33f73b00cc47b18165296e9e";
    private final String CLIENT_SECRET = "238ca926e7e59121ceb7c46c3a2c91eafe51b6c5";
    private final String GRANT_TYPE = "authorization_code";
    private final String REDIRECT_URL = "http://about:blank";
    private final String RESPONSE_TYPE = "code";

    public final String AUTH_URL = "https://gitter.im/login/oauth/authorize?"
            + "client_id=" + CLIENT_ID + "&response_type=" + RESPONSE_TYPE + "&redirect_uri=" + REDIRECT_URL;

    private LoginView mView;

    private CompositeSubscription mSubscriptions;
    private DataManger mDataManager;
    private RxSchedulersFactory mFactory;

    public LoginPresenter(RxSchedulersFactory factory, DataManger dataManager) {
        mDataManager = dataManager;
        mFactory = factory;
    }

    @Override
    public void bindView(LoginView view) {
        mView = view;
        mSubscriptions = new CompositeSubscription();
    }

    @Override
    public void unbindView() {
        mSubscriptions.unsubscribe();
        mView = null;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDestroy() {

    }

    public String getAuthUrl() {
        return AUTH_URL;
    }

    public void loadAccessToken(String code) {
        if (!Utils.getInstance().isNetworkConnected()) {
            mView.errorAuth(R.string.no_network);
            return;
        }

        mView.showProgress();

        Subscription sub = mDataManager.authorization(CLIENT_ID, CLIENT_SECRET, code,
                GRANT_TYPE, REDIRECT_URL)
                .map(authResponseModel -> {
                    Utils.getInstance().writeAuthResponsePref(authResponseModel);
                    return authResponseModel;
                })
                .subscribeOn(mFactory.io())
                .observeOn(mFactory.androidMainThread())
                .subscribe(authResponseModel -> {
                    // Write access token to preferences
                    mView.hideProgress();
                    mView.successAuth();
                }, throwable -> {
                    // If error, then set visible "Sign In" button
                    mView.hideProgress();
                    mView.errorAuth(R.string.error_auth);
                });

        mSubscriptions.add(sub);
    }
}