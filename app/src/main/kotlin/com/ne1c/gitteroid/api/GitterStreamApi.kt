package com.ne1c.gitteroid.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url
import rx.Observable

interface GitterStreamApi {
    @Streaming
    @GET
    fun getMessagesStream(@Url streamUrl: String,
                          @Header("Authorization") access_token: String): Observable<ResponseBody>
}
