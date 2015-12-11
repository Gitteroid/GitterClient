package com.ne1c.developerstalk.api;

import com.ne1c.developerstalk.models.AuthResponseModel;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.models.UserModel;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.Streaming;
import rx.Observable;

public interface GitterApi {

    @GET("/v1/rooms")
    Observable<List<RoomModel>> getCurrentUserRooms(@Header("Authorization") String access_token);

    @GET("/v1/rooms/{roomId}/chatMessages")
    void getMessagesRoom(@Header("Authorization") String access_token,
                         @Path("roomId") String roomId,
                         @Query("limit") int limit,
                         Callback<ArrayList<MessageModel>> callback);

    @FormUrlEncoded
    @POST("/v1/rooms/{roomId}/chatMessages")
    MessageModel sendMessage(@Header("Authorization") String access_token,
                             @Path("roomId") String roomId,
                             @Field("text") String text);

    @FormUrlEncoded
    @PUT("/v1/rooms/{roomId}/chatMessages/{chatMessageId}")
    void updateMessage(@Header("Authorization") String access_token,
                       @Path("roomId") String roomId,
                       @Path("chatMessageId") String chatMessageId,
                       @Field("text") String messageText,
                       Callback<MessageModel> callback);

    @GET("/v1/user")
    void getCurrentUser(@Header("Authorization") String access_token,
                        Callback<ArrayList<UserModel>> callback);

    @GET("/v1/rooms/{roomId}/chatMessages")
    void getMessagesBeforeId(@Header("Authorization") String access_token,
                             @Path("roomId") String roomId,
                             @Query("limit") int limit,
                             @Query("beforeId") String beforeId,
                             Callback<ArrayList<MessageModel>> callback);

    @FormUrlEncoded
    @POST("/login/oauth/token")
    Observable<AuthResponseModel> authorization(@Field("client_id") String client_id,
                                                @Field("client_secret") String client_secret,
                                                @Field("code") String code,
                                                @Field("grant_type") String grant_type,
                                                @Field("redirect_uri") String redirect_uri);

    @FormUrlEncoded
    @POST("/v1/user/{userId}/rooms/{roomId}/unreadItems")
    void readMessages(@Header("Authorization") String access_token, @Path("userId") String userId,
                      @Path("roomId") String roomId, @Field("chat") String[] chat, Callback<Response> callback);

    @DELETE("/v1/rooms/{roomId}/users/{userId}")
    void leaveRoom(@Header("Authorization") String access_token,
                   @Path("roomId") String roomId,
                   @Path("userId") String userId,
                   Callback<Response> callback);

    @POST("/v1/rooms")
    @FormUrlEncoded
    void joinRoom(@Header("Authorization") String access_token,
                  @Field("uri") String roomUri,
                  Callback<RoomModel> callback);

    @GET("/v1/rooms")
    void searchRooms(@Header("Authorization") String access_token,
                     @Query("q") String searchTerm,
                     Callback<Response> callback);

    @GET("/v1/rooms")
    void searchRooms(@Header("Authorization") String access_token,
                     @Query("q") String searchTerm,
                     @Query("limit") int limit,
                     Callback<Response> callback);

    @GET("/v1/user")
    void searchUsers(@Header("Authorization") String access_token,
                     @Query("q") String searchTerm,
                     Callback<Response> callback);

    @Streaming
    @GET("/v1/rooms/{roomId}/chatMessages")
    Observable<Response> getRoomStream(@Header("Authorization") String access_token,
                                       @Path("roomId") String roomId);
}