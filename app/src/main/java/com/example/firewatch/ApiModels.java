package com.example.firewatch;

import com.google.gson.annotations.SerializedName;

public class ApiModels {

    // User table model
    public static class UserRow {
        public String id;
        public String clerk_user_id;
        public String email;
        public String username;
        public String first_name;
        public String last_name;
        public String role; // "user" or "admin"
        public String created_at;
        public String updated_at;
    }

    // Create user request
    public static class CreateUserRequest {
        public String clerk_user_id;
        public String email;
        public String username;
        public String first_name;
        public String last_name;
        public String role;
    }

    // Log table model
    public static class LogRow {
        public long id;
        public String esp32_id;
        public String status; // "fire", "safe", or "failed"
        public String timestamp;

        // Helper methods
        public boolean isFire() {
            return "fire".equalsIgnoreCase(status);
        }

        public boolean isSafe() {
            return "safe".equalsIgnoreCase(status);
        }

        public boolean isFailed() {
            return "failed".equalsIgnoreCase(status);
        }
    }

    // Status table model
    public static class StatusRow {
        public String esp32_id;
        public String last_status; // "fire", "safe", or "failed"
        public String last_updated;
        public String location; // varchar(30)
        public String block; // varchar(30)

        // Helper method to get display name for user view
        public String getDisplayName() {
            if (location != null && !location.isEmpty() && block != null && !block.isEmpty()) {
                return location + " - " + block;
            } else if (location != null && !location.isEmpty()) {
                return location;
            } else if (block != null && !block.isEmpty()) {
                return block;
            }
            return esp32_id;
        }

        // Helper to check if this is a fire status
        public boolean isFire() {
            return "fire".equalsIgnoreCase(last_status);
        }

        // Helper to check if this is safe
        public boolean isSafe() {
            return "safe".equalsIgnoreCase(last_status);
        }

        // Helper to check if failed
        public boolean isFailed() {
            return "failed".equalsIgnoreCase(last_status);
        }
    }

    // Update device location request
    public static class UpdateDeviceLocationRequest {
        public String location;
        public String block;
    }

    // Device configuration helper (if needed for future use)
    public static class DeviceConfig {
        public String esp32_id;
        public String location;
        public String block;
        public int floor;

        public DeviceConfig(String id, String location, String block, int floor) {
            this.esp32_id = id;
            this.location = location;
            this.block = block;
            this.floor = floor;
        }
    }
}