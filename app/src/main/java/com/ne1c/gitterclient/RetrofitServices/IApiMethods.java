package com.ne1c.gitterclient.RetrofitServices;

import com.ne1c.gitterclient.Models.AuthResponseModel;
import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.Models.UserModel;
import com.ne1c.gitterclient.Utils;

import java.util.ArrayList;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.Streaming;


public interface IApiMethods {

    @Headers({
            "Content-Type: application/json",
            "Accept: application/json"
    })
    @GET("/v1/rooms")
    ArrayList<RoomModel> getCurrentUserRooms(@Header("Authorization") String access_token);

    @Headers({
            "Content-Type: application/json",
            "Accept: application/json"
    })
    @GET("/v1/rooms/{roomId}/chatMessages")
    void getMessagesRoom(@Header("Authorization") String access_token, @Path("roomId") String roomId, @Query("limit") int limit, Callback<ArrayList<MessageModel>> callback);

    @Headers({
            "Content-Type: application/json",
            "Accept: application/json"
    })
    @POST("/v1/rooms/{roomId}/chatMessages")
    void sendMessage(@Header("Authorization") String access_token, @Path("roomId") String roomId, @Query("text") String messageText);

    @Headers({
            "Content-Type: application/json",
            "Accept: application/json"
    })
    @PUT("/v1/rooms/{roomId}/chatMessages/{chatMessageId}")
    void updateMessage(@Header("Authorization") String access_token, @Path("roomId") String roomId, @Path("chatMessageId") String chatMessageId, @Query("text") String messageText);

    @Headers({
            "Content-Type: application/json",
            "Accept: application/json",
    })
    @GET("/v1/user")
    void getCurrentUser(@Header("Authorization") String access_token, Callback<ArrayList<UserModel>> callback);

    @Headers({
            "Content-Type: application/json",
            "Accept: application/json"
    })
    @POST("/v1/user/{userId}/rooms/{roomId}/unreadItems")
    void getUnreadItems(@Header("Authorization") String access_token, @Path("userId") String userId, @Path("roomId") String roomId);

    @FormUrlEncoded
    @POST("/login/oauth/token")
    void authorization(@Field("client_id") String client_id, @Field("client_secret") String client_secret,
                       @Field("code") String code, @Field("grant_type") String grant_type,
                       @Field("redirect_uri") String redirect_uri, Callback<AuthResponseModel> callback);
}
