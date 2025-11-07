package com.example.firewatch;

import com.google.gson.annotations.SerializedName;

public class ApiModels {

    public static class UserRow {
        public String id;
        public String clerk_user_id;
        public String email;
        public String username;
        public String first_name;
        public String last_name;
        public String role;
        public String created_at;
        public String updated_at;
    }

    public static class CreateUserRequest {
        public String clerk_user_id;
        public String email;
        public String username;
        public String first_name;
        public String last_name;
        public String role;
    }

    public static class LogRow {
        public long id;
        public String esp32_id;
        public String status;
        public String timestamp;
        public String location; // Track location at time of log
        public String block;    // Track block at time of log

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

    public static class StatusRow {
        public String esp32_id;
        public String last_status;
        public String last_updated;
        public String location;
        public String block;

        @SerializedName("check")
        public Boolean check; // System maintenance control - using existing column

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

        public boolean isFire() {
            return "fire".equalsIgnoreCase(last_status);
        }

        public boolean isSafe() {
            return "safe".equalsIgnoreCase(last_status);
        }

        public boolean isFailed() {
            return "failed".equalsIgnoreCase(last_status);
        }

        public boolean isSystemActive() {
            return check != null && check;
        }
    }

    public static class UpdateDeviceLocationRequest {
        public String location;
        public String block;
    }

    public static class UpdateSystemCheckRequest {
        @SerializedName("check")
        public Boolean check;
    }

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