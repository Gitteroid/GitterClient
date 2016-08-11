package com.ne1c.gitteroid.api

import com.ne1c.gitteroid.api.responses.JoinRoomResponse
import com.ne1c.gitteroid.api.responses.StatusResponse
import com.ne1c.gitteroid.models.data.*
import okhttp3.ResponseBody
import retrofit2.http.*
import rx.Observable
import java.util.*

interface GitterApi {
    @GET("/v1/rooms")
    fun getCurrentUserRooms(@Header("Authorization") access_token: String): Observable<ArrayList<RoomModel>>

    @GET("/v1/rooms/{roomId}/chatMessages")
    fun getMessagesRoom(@Header("Authorization") access_token: String,
                        @Path("roomId") roomId: String,
                        @Query("limit") limit: Int): Observable<ArrayList<MessageModel>>

    @FormUrlEncoded
    @POST("/v1/rooms/{roomId}/chatMessages")
    fun sendMessage(@Header("Authorization") access_token: String,
                    @Path("roomId") roomId: String,
                    @Field("text") text: String): Observable<MessageModel>

    @FormUrlEncoded
    @PUT("/v1/rooms/{roomId}/chatMessages/{chatMessageId}")
    fun updateMessage(@Header("Authorization") access_token: String,
                      @Path("roomId") roomId: String,
                      @Path("chatMessageId") chatMessageId: String,
                      @Field("text") messageText: String): Observable<MessageModel>

    @GET("/v1/user")
    fun getCurrentUser(@Header("Authorization") access_token: String): Observable<ArrayList<UserModel>>

    @GET("/v1/rooms/{roomId}/chatMessages")
    fun getMessagesBeforeId(@Header("Authorization") access_token: String,
                            @Path("roomId") roomId: String,
                            @Query("limit") limit: Int,
                            @Query("beforeId") beforeId: String): Observable<ArrayList<MessageModel>>

    @FormUrlEncoded
    @POST
    fun     authorization(@Url authUrl: String,
                      @Field("client_id") client_id: String,
                      @Field("client_secret") client_secret: String,
                      @Field("code") code: String,
                      @Field("grant_type") grant_type: String,
                      @Field("redirect_uri") redirect_uri: String): Observable<AuthResponseModel>

    @FormUrlEncoded
    @POST("/v1/user/{userId}/rooms/{roomId}/unreadItems")
    fun readMessages(@Header("Authorization") access_token: String,
                     @Path("userId") userId: String,
                     @Path("roomId") roomId: String,
                     @Field("chat") chat: Array<String>): Observable<StatusResponse>

    @DELETE("/v1/rooms/{roomId}/users/{userId}")
    fun leaveRoom(@Header("Authorization") access_token: String,
                  @Path("roomId") roomId: String,
                  @Path("userId") userId: String): Observable<StatusResponse>

    @POST("/v1/rooms")
    @FormUrlEncoded
    fun joinRoom(@Header("Authorization") access_token: String,
                 @Field("uri") roomUri: String): Observable<JoinRoomResponse>

    @GET("/v1/user")
    fun searchUsers(@Header("Authorization") access_token: String,
                    @Query("q") searchTerm: String): Observable<ResponseBody>

    @GET("/v1/rooms")
    fun searchRooms(@Header("Authorization") access_token: String,
                    @Query("q") query: String,
                    @Query("limit") limit: Int,
                    @Query("offset") offset: Int): Observable<SearchRoomsResponse>  // Offset not working
}