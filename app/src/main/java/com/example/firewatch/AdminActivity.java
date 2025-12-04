package com.example.firewatch;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.firewatch.databinding.ActivityAdminBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends AppCompatActivity {

    private ActivityAdminBinding b;
    private AdminStatusAdapter statusAdapter;
    private List<BuildingConfig.Floor> building;
    private Map<String, String> deviceStatusMap;
    private int currentFloor = 1;
    private SharedPreferences prefs;
    private boolean hasActiveFire = false;
    private List<String> currentFireDevices = new ArrayList<>();
    private boolean systemActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        b = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.topAppBar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("FireWatch Admin");
        }

        prefs = getSharedPreferences("firewatch_prefs", MODE_PRIVATE);
        building = BuildingConfig.generateBuilding();
        deviceStatusMap = new HashMap<>();

        setupFloorSelector();
        setupRecyclers();
        setupButtons();

        fetchAll();
        updateMonitoringButton();
    }

    private void setupFloorSelector() {
        List<String> floorNames = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            floorNames.add("Floor " + i);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, floorNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spinnerFloor.setAdapter(adapter);

        b.spinnerFloor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFloor = position + 1;
                updateFloorMap();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclers() {
        b.recyclerStatus.setLayoutManager(new LinearLayoutManager(this));
        statusAdapter = new AdminStatusAdapter(device -> {
            Intent intent = new Intent(AdminActivity.this, DeviceLogsActivity.class);
            intent.putExtra("device_id", device.esp32_id);
            intent.putExtra("location", device.location);
            intent.putExtra("block", device.block);
            startActivity(intent);
        });
        b.recyclerStatus.setAdapter(statusAdapter);
    }

    private void setupButtons() {
        b.btnRefresh.setOnClickListener(v -> fetchAll());
        b.btnToggleService.setOnClickListener(v -> toggleSystemMaintenance());
        b.btnEmergencyCall.setOnClickListener(v -> makeEmergencyCall());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, AdminSettingsActivity.class));
            return true;
        } else if (id == R.id.action_device_management) {
            startActivity(new Intent(this, AdminDeviceManagementActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void fetchAll() {
        SupabaseApi api = ApiClient.api(this);

        api.getStatus().enqueue(new Callback<List<ApiModels.StatusRow>>() {
            @Override
            public void onResponse(Call<List<ApiModels.StatusRow>> call, Response<List<ApiModels.StatusRow>> res) {
                if (!res.isSuccessful() || res.body() == null) {
                    updateUIOffline();
                    return;
                }

                LinkedHashMap<String, ApiModels.StatusRow> latest = new LinkedHashMap<>();
                for (ApiModels.StatusRow row : res.body()) {
                    if (!latest.containsKey(row.esp32_id)) {
                        latest.put(row.esp32_id, row);
                    }
                }

                // Check if any device has system_active = false (maintenance mode)
                systemActive = true;
                for (ApiModels.StatusRow row : latest.values()) {
                    if (row.check != null && !row.check) {
                        systemActive = false;
                        break;
                    }
                }

                deviceStatusMap.clear();
                currentFireDevices.clear();
                List<String> offlineDevices = new ArrayList<>();

                for (ApiModels.StatusRow row : latest.values()) {
                    String key = row.location != null && !row.location.isEmpty()
                            ? row.location : row.esp32_id;

                    if (!row.isSystemActive()) {
                        deviceStatusMap.put(key, "maintenance");
                    } else {
                        deviceStatusMap.put(key, row.last_status);
                    }

                    if (row.isSystemActive() && "fire".equalsIgnoreCase(row.last_status)) {
                        currentFireDevices.add(key);
                    } else if ("failed".equalsIgnoreCase(row.last_status)) {
                        offlineDevices.add(key);
                    }
                }

                statusAdapter.setData(new ArrayList<>(latest.values()));

                String newestTime = "--";
                for (ApiModels.StatusRow s : latest.values()) {
                    if (s.last_updated != null && (newestTime.equals("--") ||
                            s.last_updated.compareTo(newestTime) > 0)) {
                        newestTime = s.last_updated;
                    }
                }

                hasActiveFire = !currentFireDevices.isEmpty();
                updateUI(currentFireDevices, offlineDevices, newestTime, latest.size());

                if (hasActiveFire) {
                    Integer fireFloor = extractFloorFromDeviceId(currentFireDevices.get(0));
                    if (fireFloor != null && fireFloor >= 1 && fireFloor <= 8) {
                        currentFloor = fireFloor;
                        b.spinnerFloor.setSelection(fireFloor - 1);
                    }
                }

                updateFloorMap();
                updateMonitoringButton();
            }

            @Override
            public void onFailure(Call<List<ApiModels.StatusRow>> call, Throwable t) {
                hasActiveFire = false;
                updateUIOffline();
                statusAdapter.setData(new ArrayList<>());
            }
        });
    }

    private void updateUI(List<String> fires, List<String> offline, String newestTime, int totalDevices) {
        if (!systemActive) {
            b.txtStatus.setText("🔧 System Under Maintenance");
            b.txtStatus.setTextColor(getColor(R.color.status_unknown));
            b.chipStatus.setText("MAINTENANCE");
            b.chipStatus.setChipBackgroundColorResource(R.color.status_unknown_light);
            b.chipStatus.setTextColor(getColor(android.R.color.black));
            b.cardStatus.setCardBackgroundColor(getColor(android.R.color.white));
            b.btnEmergencyCall.setVisibility(View.GONE);

        } else if (!fires.isEmpty()) {
            b.txtStatus.setText("🔥 FIRE DETECTED on " + fires.size() + " location(s)");
            b.txtStatus.setTextColor(getColor(R.color.status_fire));
            b.chipStatus.setText("FIRE");
            b.chipStatus.setChipBackgroundColorResource(R.color.status_fire);
            b.chipStatus.setTextColor(getColor(android.R.color.white));
            b.cardStatus.setCardBackgroundColor(getColor(R.color.status_fire_light));
            b.btnEmergencyCall.setVisibility(View.VISIBLE);

        } else if (!offline.isEmpty() && offline.size() == totalDevices) {
            b.txtStatus.setText("All Devices Offline - " + offline.size() + " locations");
            b.txtStatus.setTextColor(getColor(android.R.color.black));
            b.chipStatus.setText("OFFLINE");
            b.chipStatus.setChipBackgroundColorResource(R.color.status_unknown_light);
            b.chipStatus.setTextColor(getColor(android.R.color.black));
            b.cardStatus.setCardBackgroundColor(getColor(android.R.color.white));
            b.btnEmergencyCall.setVisibility(View.GONE);

        } else if (!offline.isEmpty()) {
            int safeDevices = totalDevices - offline.size();
            b.txtStatus.setText(offline.size() + " Devices Offline - " + safeDevices + " Safe");
            b.txtStatus.setTextColor(getColor(R.color.status_alert));
            b.chipStatus.setText("WARNING");
            b.chipStatus.setChipBackgroundColorResource(R.color.status_alert);
            b.chipStatus.setTextColor(getColor(android.R.color.white));
            b.cardStatus.setCardBackgroundColor(getColor(android.R.color.white));
            b.btnEmergencyCall.setVisibility(View.GONE);

        } else if (totalDevices > 0) {
            b.txtStatus.setText("All Systems Safe");
            b.txtStatus.setTextColor(getColor(R.color.status_safe));
            b.chipStatus.setText("SAFE");
            b.chipStatus.setChipBackgroundColorResource(R.color.status_safe_light);
            b.chipStatus.setTextColor(getColor(R.color.status_safe));
            b.cardStatus.setCardBackgroundColor(getColor(android.R.color.white));
            b.btnEmergencyCall.setVisibility(View.GONE);
        } else {
            b.txtStatus.setText("No devices connected");
            b.txtStatus.setTextColor(getColor(android.R.color.darker_gray));
            b.chipStatus.setText("OFFLINE");
            b.chipStatus.setChipBackgroundColorResource(R.color.status_unknown);
            b.cardStatus.setCardBackgroundColor(getColor(android.R.color.white));
            b.btnEmergencyCall.setVisibility(View.GONE);
        }

        b.txtLastUpdated.setText("Last updated: " + newestTime);
    }

    private void updateUIOffline() {
        b.txtStatus.setText("Connection Failed");
        b.txtStatus.setTextColor(getColor(android.R.color.black));
        b.chipStatus.setText("OFFLINE");
        b.chipStatus.setChipBackgroundColorResource(R.color.status_unknown_light);
        b.chipStatus.setTextColor(getColor(android.R.color.black));
        b.cardStatus.setCardBackgroundColor(getColor(android.R.color.white));
        b.txtLastUpdated.setText("Last updated: --");
        b.btnEmergencyCall.setVisibility(View.GONE);
    }

    private void updateFloorMap() {
        if (currentFloor < 1 || currentFloor > building.size()) return;

        BuildingConfig.Floor floor = building.get(currentFloor - 1);
        Set<String> fireRooms = new HashSet<>();

        for (String location : deviceStatusMap.keySet()) {
            String status = deviceStatusMap.get(location);
            if ("fire".equalsIgnoreCase(status)) {
                String roomNumber = extractRoomNumber(location);
                Integer deviceFloor = extractFloorFromDeviceId(location);

                if (deviceFloor != null && deviceFloor == currentFloor && roomNumber != null) {
                    fireRooms.add(roomNumber);
                }
            }
        }

        b.floorMapView.setFloor(floor, fireRooms);
    }

    private Integer extractFloorFromDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) return null;
        try {
            if (deviceId.startsWith("STAIRS_")) {
                String[] parts = deviceId.split("_");
                if (parts.length >= 3) {
                    return Integer.parseInt(parts[2]);
                }
            }
            char firstChar = deviceId.charAt(0);
            if (Character.isDigit(firstChar)) {
                return Character.getNumericValue(firstChar);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private String extractRoomNumber(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) return null;
        try {
            if (deviceId.startsWith("STAIRS_")) {
                return deviceId;
            }
            if (deviceId.contains("-")) {
                String[] parts = deviceId.split("-");
                return parts[0].trim();
            }
            return deviceId.trim();
        } catch (Exception e) {
            return deviceId;
        }
    }

    private void makeEmergencyCall() {
        if (!hasActiveFire) {
            return;
        }
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:101"));
        startActivity(callIntent);
    }

    private void toggleSystemMaintenance() {
        String message = systemActive
                ? "This will put the system in maintenance mode and disable monitoring for ALL users. Continue?"
                : "This will activate the system and enable monitoring for all users. Continue?";

        new AlertDialog.Builder(this)
                .setTitle("System Control")
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    updateSystemCheckStatus(!systemActive);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSystemCheckStatus(boolean active) {
        Toast.makeText(this, active ? "Activating system..." : "Entering maintenance mode...", Toast.LENGTH_SHORT).show();

        // First, get all devices
        SupabaseApi api = ApiClient.api(this);

        api.getStatus().enqueue(new Callback<List<ApiModels.StatusRow>>() {
            @Override
            public void onResponse(Call<List<ApiModels.StatusRow>> call, Response<List<ApiModels.StatusRow>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(AdminActivity.this, "Failed to fetch devices", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<ApiModels.StatusRow> devices = response.body();
                updateAllDevicesCheck(devices, active, 0);
            }

            @Override
            public void onFailure(Call<List<ApiModels.StatusRow>> call, Throwable t) {
                Toast.makeText(AdminActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAllDevicesCheck(List<ApiModels.StatusRow> devices, boolean active, int index) {
        if (index >= devices.size()) {
            // All devices updated
            systemActive = active;
            Toast.makeText(this,
                    active ? "System activated successfully" : "Maintenance mode enabled",
                    Toast.LENGTH_SHORT).show();
            fetchAll();
            return;
        }

        SupabaseApi api = ApiClient.api(this);
        ApiModels.UpdateSystemCheckRequest request = new ApiModels.UpdateSystemCheckRequest();
        request.check = active;

        String deviceId = devices.get(index).esp32_id;

        api.updateDeviceCheck("eq." + deviceId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Update next device
                    updateAllDevicesCheck(devices, active, index + 1);
                } else {
                    Toast.makeText(AdminActivity.this,
                            "Failed to update device: " + deviceId,
                            Toast.LENGTH_SHORT).show();
                    // Continue with next device anyway
                    updateAllDevicesCheck(devices, active, index + 1);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AdminActivity.this,
                        "Error updating " + deviceId + ": " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                // Continue with next device anyway
                updateAllDevicesCheck(devices, active, index + 1);
            }
        });
    }

    private void updateSystemActiveStatus(boolean active) {
        // Update all devices' system_active status
        SupabaseApi api = ApiClient.api(this);

        Toast.makeText(this, active ? "Activating system..." : "Entering maintenance mode...", Toast.LENGTH_SHORT).show();


        fetchAll();
    }

    private void updateMonitoringButton() {
        b.btnToggleService.setText(systemActive ? "Stop System" : "Start System");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    stopService(new Intent(this, StatusPollerService.class));

                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();

                    prefs.edit()
                            .putBoolean("is_logged_in", false)
                            .remove("user_id")
                            .remove("user_email")
                            .remove("user_role")
                            .remove("clerk_user_id")
                            .remove("clerk_role")
                            .remove("user_first_name")
                            .remove("user_last_name")
                            .remove("user_username")
                            .apply();

                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAll();
    }
}