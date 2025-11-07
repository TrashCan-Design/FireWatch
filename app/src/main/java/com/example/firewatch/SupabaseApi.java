package com.example.firewatch;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseApi {

    // ==================== LOGS TABLE ====================

    /**
     * Get logs with optional filters
     * Default: Get latest 50 logs ordered by timestamp descending
     */
    @GET("rest/v1/logs?select=*&order=timestamp.desc&limit=50")
    Call<List<ApiModels.LogRow>> getLogs();

    /**
     * Get logs for specific device
     */
    @GET("rest/v1/logs?select=*&order=timestamp.desc")
    Call<List<ApiModels.LogRow>> getLogsByDevice(@Query("esp32_id") String esp32Id);

    /**
     * Get logs by status (fire, safe, failed)
     */
    @GET("rest/v1/logs?select=*&order=timestamp.desc")
    Call<List<ApiModels.LogRow>> getLogsByStatus(@Query("status") String status);

    // ==================== STATUS TABLE ====================

    /**
     * Get all device statuses
     * Ordered by last_updated descending
     */
    @GET("rest/v1/status?select=*&order=last_updated.desc")
    Call<List<ApiModels.StatusRow>> getStatus();

    /**
     * Get status for specific device
     */
    @GET("rest/v1/status?select=*")
    Call<List<ApiModels.StatusRow>> getDeviceStatus(@Query("esp32_id") String esp32Id);

    /**
     * Get all devices with fire status
     */
    @GET("rest/v1/status?select=*&last_status=eq.fire&order=last_updated.desc")
    Call<List<ApiModels.StatusRow>> getFireDevices();

    /**
     * Get all safe devices
     */
    @GET("rest/v1/status?select=*&last_status=eq.safe&order=last_updated.desc")
    Call<List<ApiModels.StatusRow>> getSafeDevices();

    /**
     * Update device location and block
     * Uses PATCH to update only specified fields
     */
    @PATCH("rest/v1/status?esp32_id=eq.{esp32_id}")
    Call<Void> updateDeviceLocation(
            @retrofit2.http.Path("esp32_id") String esp32Id,
            @Body ApiModels.UpdateDeviceLocationRequest request
    );

    // ==================== USERS TABLE ====================

    /**
     * Get user by Clerk user ID
     */
    @GET("rest/v1/users?select=*")
    Call<List<ApiModels.UserRow>> getUserByClerkId(@Query("clerk_user_id") String clerkUserId);

    /**
     * Get user by email
     */
    @GET("rest/v1/users?select=*")
    Call<List<ApiModels.UserRow>> getUserByEmail(@Query("email") String email);

    /**
     * Get all admin users
     */
    @GET("rest/v1/users?select=*&role=eq.admin")
    Call<List<ApiModels.UserRow>> getAdminUsers();

    /**
     * Create new user
     * Note: Supabase requires Prefer: return=representation header to return created object
     */
    @POST("rest/v1/users")
    Call<ApiModels.UserRow> createUser(@Body ApiModels.CreateUserRequest user);

    /**
     * Get all users (admin only)
     */
    @GET("rest/v1/users?select=*&order=created_at.desc")
    Call<List<ApiModels.UserRow>> getAllUsers();
}