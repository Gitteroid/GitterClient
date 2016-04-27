package com.ne1c.gitteroid.api;

import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Streaming;
import retrofit2.http.Url;
import rx.Observable;

public interface GitterStreamApi {
    @Streaming
    @GET
    Observable<ResponseBody> getMessagesStream(@Url String streamUrl,
                                               @Header("Authorization") String access_token);
}
