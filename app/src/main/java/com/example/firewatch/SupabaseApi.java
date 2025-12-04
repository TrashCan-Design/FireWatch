package com.example.firewatch;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Header;

public interface SupabaseApi {

    //logs

    @GET("rest/v1/logs?select=*&order=timestamp.desc&limit=50")
    Call<List<ApiModels.LogRow>> getLogs();

    @GET("rest/v1/logs?select=*&order=timestamp.desc")
    Call<List<ApiModels.LogRow>> getLogsByDevice(@Query("esp32_id") String esp32IdQuery);

    @GET("rest/v1/logs?select=*&order=timestamp.desc")
    Call<List<ApiModels.LogRow>> getLogsByStatus(@Query("status") String status);

    //status

    @GET("rest/v1/status?select=*&order=last_updated.desc")
    Call<List<ApiModels.StatusRow>> getStatus();

    @GET("rest/v1/status?select=*")
    Call<List<ApiModels.StatusRow>> getDeviceStatus(@Query("esp32_id") String esp32Id);

    @GET("rest/v1/status?select=*&last_status=eq.fire&order=last_updated.desc")
    Call<List<ApiModels.StatusRow>> getFireDevices();

    @GET("rest/v1/status?select=*&last_status=eq.safe&order=last_updated.desc")
    Call<List<ApiModels.StatusRow>> getSafeDevices();

   //device location update
    @PATCH("rest/v1/status")
    Call<Void> updateDeviceLocation(
            @Query("esp32_id") String esp32IdQuery,
            @Body ApiModels.UpdateDeviceLocationRequest request
    );

    /**
     * Update system check status for a specific device
     */
    @PATCH("rest/v1/status")
    Call<Void> updateDeviceCheck(
            @Query("esp32_id") String esp32IdQuery,
            @Body ApiModels.UpdateSystemCheckRequest request
    );

    // user

    @GET("rest/v1/users?select=*")
    Call<List<ApiModels.UserRow>> getUserByClerkId(@Query("clerk_user_id") String clerkUserId);

    @GET("rest/v1/users?select=*")
    Call<List<ApiModels.UserRow>> getUserByEmail(@Query("email") String email);

    @GET("rest/v1/users?select=*&role=eq.admin")
    Call<List<ApiModels.UserRow>> getAdminUsers();

    @POST("rest/v1/users")
    Call<ApiModels.UserRow> createUser(@Body ApiModels.CreateUserRequest user);

    @GET("rest/v1/users?select=*&order=created_at.desc")
    Call<List<ApiModels.UserRow>> getAllUsers();
}