package com.ne1c.gitterclient.RetrofitServices;

import com.ne1c.gitterclient.Models.AuthResponseModel;
import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.Models.UserModel;

import java.util.ArrayList;

import retrofit.Callback;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

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

    @FormUrlEncoded
    @POST("/v1/rooms/{roomId}/chatMessages")
    MessageModel sendMessage(@Header("Authorization") String access_token, @Path("roomId") String roomId, @Field("text") String text);

    @FormUrlEncoded
    @PUT("/v1/rooms/{roomId}/chatMessages/{chatMessageId}")
    void updateMessage(@Header("Authorization") String access_token, @Path("roomId") String roomId,
                       @Path("chatMessageId") String chatMessageId,
                       @Field("text") String messageText, Callback<MessageModel> callback);

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
    @GET("/v1/rooms/{roomId}/chatMessages")
    void getMessagesBeforeId(@Header("Authorization") String access_token, @Path("roomId") String roomId,
                             @Query("limit") int limit,
                             @Query("beforeId") String beforeId, Callback<ArrayList<MessageModel>> callback);

    @FormUrlEncoded
    @POST("/login/oauth/token")
    void authorization(@Field("client_id") String client_id, @Field("client_secret") String client_secret,
                       @Field("code") String code, @Field("grant_type") String grant_type,
                       @Field("redirect_uri") String redirect_uri, Callback<AuthResponseModel> callback);
}
