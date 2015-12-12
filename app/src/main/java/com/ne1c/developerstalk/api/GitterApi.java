package com.ne1c.developerstalk.api;

import com.ne1c.developerstalk.models.AuthResponseModel;
import com.ne1c.developerstalk.models.MessageModel;
import com.ne1c.developerstalk.models.RoomModel;
import com.ne1c.developerstalk.models.UserModel;

import java.util.ArrayList;

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
    Observable<ArrayList<RoomModel>> getCurrentUserRooms(@Header("Authorization") String access_token);

    @GET("/v1/rooms/{roomId}/chatMessages")
    Observable<ArrayList<MessageModel>> getMessagesRoom(@Header("Authorization") String access_token,
                                                        @Path("roomId") String roomId,
                                                        @Query("limit") int limit);

    @FormUrlEncoded
    @POST("/v1/rooms/{roomId}/chatMessages")
    Observable<MessageModel> sendMessage(@Header("Authorization") String access_token,
                                         @Path("roomId") String roomId,
                                         @Field("text") String text);

    @FormUrlEncoded
    @PUT("/v1/rooms/{roomId}/chatMessages/{chatMessageId}")
    Observable<MessageModel> updateMessage(@Header("Authorization") String access_token,
                                           @Path("roomId") String roomId,
                                           @Path("chatMessageId") String chatMessageId,
                                           @Field("text") String messageText);

    @GET("/v1/user")
    Observable<ArrayList<UserModel>> getCurrentUser(@Header("Authorization") String access_token);

    @GET("/v1/rooms/{roomId}/chatMessages")
    Observable<ArrayList<MessageModel>> getMessagesBeforeId(@Header("Authorization") String access_token,
                                                            @Path("roomId") String roomId,
                                                            @Query("limit") int limit,
                                                            @Query("beforeId") String beforeId);

    @FormUrlEncoded
    @POST("/login/oauth/token")
    Observable<AuthResponseModel> authorization(@Field("client_id") String client_id,
                                                @Field("client_secret") String client_secret,
                                                @Field("code") String code,
                                                @Field("grant_type") String grant_type,
                                                @Field("redirect_uri") String redirect_uri);

    @FormUrlEncoded
    @POST("/v1/user/{userId}/rooms/{roomId}/unreadItems")
    Observable<Response> readMessages(@Header("Authorization") String access_token, @Path("userId") String userId,
                                      @Path("roomId") String roomId, @Field("chat") String[] chat);

    @DELETE("/v1/rooms/{roomId}/users/{userId}")
    Observable<Response> leaveRoom(@Header("Authorization") String access_token,
                                   @Path("roomId") String roomId,
                                   @Path("userId") String userId);

    @POST("/v1/rooms")
    @FormUrlEncoded
    Observable<RoomModel> joinRoom(@Header("Authorization") String access_token,
                                   @Field("uri") String roomUri);

    @GET("/v1/rooms")
    Observable<Response> searchRooms(@Header("Authorization") String access_token,
                                     @Query("q") String searchTerm);

    @GET("/v1/rooms")
    Observable<Response> searchRooms(@Header("Authorization") String access_token,
                                     @Query("q") String searchTerm,
                                     @Query("limit") int limit);

    @GET("/v1/user")
    Observable<Response> searchUsers(@Header("Authorization") String access_token,
                                     @Query("q") String searchTerm);

    @Streaming
    @GET("/v1/rooms/{roomId}/chatMessages")
    Observable<Response> getRoomStream(@Header("Authorization") String access_token,
                                       @Path("roomId") String roomId);
}