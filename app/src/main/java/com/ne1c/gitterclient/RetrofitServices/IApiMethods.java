package com.ne1c.gitterclient.RetrofitServices;

import com.ne1c.gitterclient.Models.MessageModel;
import com.ne1c.gitterclient.Models.RoomModel;
import com.ne1c.gitterclient.Models.UserModel;
import com.ne1c.gitterclient.Utils;

import java.util.ArrayList;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.Streaming;


public interface IApiMethods {
    String bearer = "your personal access token to gitter api";

    @Headers({
            "Content-Type: application/json",
            "Accept: application/json",
            "Authorization: Bearer " + bearer
    })
    @GET("/v1/rooms")
    ArrayList<RoomModel> getCuurentUserRooms();

    @Headers({
            "Content-Type: application/json",
            "Accept: application/json",
            "Authorization: Bearer " + bearer
    })
    @GET("/v1/rooms/{roomId}/chatMessages")
    void getMessagesRoom(@Path("roomId") String roomId, @Query("limit") int limit, Callback<ArrayList<MessageModel>> callback);

    @Headers({
            "Content-Type: application/json",
            "Accept: application/json",
            "Authorization: Bearer " + bearer
    })
    @POST("/v1/rooms/{roomId}/chatMessages")
    void sendMessage(@Path("roomId") String roomId, @Query("text") String messageText);

    @Headers({
            "Content-Type: application/json",
            "Accept: application/json",
            "Authorization: Bearer " + bearer
    })
    @PUT("/v1/rooms/{roomId}/chatMessages/{chatMessageId}")
    void updateMessage(@Path("roomId") String roomId, @Path("chatMessageId") String chatMessageId, @Query("text") String messageText);

    @Headers({
            "Content-Type: application/json",
            "Accept: application/json",
            "Authorization: Bearer " + bearer
    })
    @GET("/v1/user")
    void getCurrentUser(Callback<ArrayList<UserModel>> callback);

    @Headers({
            "Content-Type: application/json",
            "Accept: application/json",
            "Authorization: Bearer " + bearer
    })
    @POST("/v1/user/{userId}/rooms/{roomId}/unreadItems")
    void getUnreadItems(@Path("userId") String userId, @Path("roomId") String roomId);

    @Headers({
            "Content-Type: application/json",
            "Accept: application/json",
            "Authorization: Bearer " + bearer
    })
    @GET("/v1/rooms/{roomId}/chatMessages")
    @Streaming
    void messageStream(@Path("roomId") String roomId, Callback<Response> callback);
}
