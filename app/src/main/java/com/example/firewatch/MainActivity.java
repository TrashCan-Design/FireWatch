package com.example.firewatch;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.firewatch.databinding.ActivityMainBinding;

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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding b;
    private UserStatusAdapter statusAdapter;
    private List<BuildingConfig.Floor> building;
    private Map<String, String> deviceStatusMap;
    private int currentFloor = 1;
    private SharedPreferences prefs;
    private boolean hasActiveFire = false;
    private List<String> currentFireDevices = new ArrayList<>();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                    startMonitoringService();
                } else {
                    showPermissionRationale();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setSupportActionBar(b.topAppBar);

        prefs = getSharedPreferences("firewatch_prefs", MODE_PRIVATE);
        building = BuildingConfig.generateBuilding();
        deviceStatusMap = new HashMap<>();

        setupFloorSelector();
        setupRecyclers();
        setupButtons();

        fetchAll();
        updateMonitoringButton();

        checkNotificationPermission();
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
        b.recyclerStatus.setNestedScrollingEnabled(true);
        statusAdapter = new UserStatusAdapter();
        b.recyclerStatus.setAdapter(statusAdapter);
    }

    private void setupButtons() {
        b.btnRefresh.setOnClickListener(v -> fetchAll());
        b.btnToggleService.setOnClickListener(v -> toggleMonitoring());
        b.btnEmergencyCall.setOnClickListener(v -> makeEmergencyCall());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
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
                    if (!latest.containsKey(row.location)) {
                        latest.put(row.location, row);
                    }
                }

                deviceStatusMap.clear();
                currentFireDevices.clear();
                List<String> offlineDevices = new ArrayList<>();

                for (ApiModels.StatusRow row : latest.values()) {
                    deviceStatusMap.put(row.location, row.last_status);
                    Log.d(TAG, "Device: " + row.location + " Status: " + row.last_status);
                }

                statusAdapter.setData(new ArrayList<>(latest.values()));

                String newestTime = "--";
                for (ApiModels.StatusRow s : latest.values()) {
                    if ("fire".equalsIgnoreCase(s.last_status)) {
                        currentFireDevices.add(s.location);
                        Log.d(TAG, "Fire detected at: " + s.location);
                    } else if ("failed".equalsIgnoreCase(s.last_status)) {
                        offlineDevices.add(s.location);
                        Log.d(TAG, "Offline device at: " + s.location);
                    }
                    if (s.last_updated != null && (newestTime.equals("--") ||
                            s.last_updated.compareTo(newestTime) > 0)) {
                        newestTime = s.last_updated;
                    }
                }

                hasActiveFire = !currentFireDevices.isEmpty();
                updateUI(currentFireDevices, offlineDevices, newestTime, latest.size());

                if (hasActiveFire) {
                    Integer fireFloor = extractFloorFromDeviceId(currentFireDevices.get(0));
                    Log.d(TAG, "First fire floor: " + fireFloor + " from location: " + currentFireDevices.get(0));
                    if (fireFloor != null && fireFloor >= 1 && fireFloor <= 8) {
                        currentFloor = fireFloor;
                        b.spinnerFloor.setSelection(fireFloor - 1);
                        updateFloorMap();
                    }
                }

                updateFloorMap();
            }

            @Override
            public void onFailure(Call<List<ApiModels.StatusRow>> call, Throwable t) {
                hasActiveFire = false;
                updateUIOffline();
                statusAdapter.setData(new ArrayList<>());
                Log.e(TAG, "Failed to fetch status: " + t.getMessage());
            }
        });
    }

    private void updateUI(List<String> fires, List<String> offline, String newestTime, int totalDevices) {
        if (!fires.isEmpty()) {
            b.txtStatus.setText("🔥 FIRE ALERT - " + fires.size() + " location");
            int col = ContextCompat.getColor(MainActivity.this, R.color.status_fire);
            b.txtStatus.setTextColor(col);
            int val = ContextCompat.getColor(MainActivity.this, R.color.white);
            b.chipStatus.setText("Fire");
            b.chipStatus.setTextColor(val);
            b.chipStatus.setChipBackgroundColorResource(R.color.status_fire);
            b.cardStatus.setCardBackgroundColor(getColor(R.color.status_fire_light));
            b.btnEmergencyCall.setVisibility(View.VISIBLE);
            b.btnEmergencyCall.setEnabled(true);

        } else if (!offline.isEmpty() && offline.size() == totalDevices) {
            // ALL DEVICES OFFLINE - Grey styling
            b.txtStatus.setText("All Devices Offline - " + offline.size() + " locations");
            b.txtStatus.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            b.chipStatus.setText("Offline");
            b.chipStatus.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            b.chipStatus.setChipBackgroundColorResource(R.color.status_unknown_light);
            b.cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
            b.btnEmergencyCall.setVisibility(View.GONE);
            b.btnEmergencyCall.setEnabled(false);

        } else if (!offline.isEmpty()) {
            // SOME DEVICES OFFLINE - Warning styling
            int safeDevices = totalDevices - offline.size();
            b.txtStatus.setText("" + offline.size() + " Devices Offline - " + safeDevices + " Safe");
            b.txtStatus.setTextColor(ContextCompat.getColor(this, R.color.status_alert));
            b.chipStatus.setText("Warning");
            b.chipStatus.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            b.chipStatus.setChipBackgroundColorResource(R.color.status_alert);
            b.cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
            b.btnEmergencyCall.setVisibility(View.GONE);
            b.btnEmergencyCall.setEnabled(false);

        } else if (totalDevices > 0) {
            // ALL SAFE - Green styling
            b.txtStatus.setText("All Areas are Safe");
            int col = ContextCompat.getColor(MainActivity.this, R.color.status_safe);
            b.txtStatus.setTextColor(col);
            b.chipStatus.setText("Safe");
            b.chipStatus.setChipBackgroundColorResource(R.color.status_safe_light);
            b.cardStatus.setCardBackgroundColor(getColor(android.R.color.black));
            b.btnEmergencyCall.setVisibility(View.GONE);
            b.btnEmergencyCall.setEnabled(false);

        } else {
            // NO DEVICES - Grey styling
            b.txtStatus.setText("System offline");
            int col = ContextCompat.getColor(MainActivity.this, R.color.status_safe);
            b.txtStatus.setTextColor(col);
            b.chipStatus.setText("OFFLINE");
            b.chipStatus.setChipBackgroundColorResource(R.color.status_unknown);
            b.cardStatus.setCardBackgroundColor(getColor(android.R.color.white));
            b.btnEmergencyCall.setVisibility(View.GONE);
            b.btnEmergencyCall.setEnabled(false);

        }

        b.txtLastUpdated.setText("Last updated: " + newestTime);
    }

    private void updateUIOffline() {
        // SERVER CONNECTION FAILED - Grey styling (same as offline)
        b.txtStatus.setText("Device not Connected to Network");
        b.txtStatus.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        b.chipStatus.setText("Offline");
        b.chipStatus.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        b.chipStatus.setChipBackgroundColorResource(R.color.status_unknown_light);
        b.cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
        b.txtLastUpdated.setText("Last updated: --");
        b.btnEmergencyCall.setVisibility(View.GONE);
        b.btnEmergencyCall.setEnabled(false);
    }

    private void updateFloorMap() {
        if (currentFloor < 1 || currentFloor > building.size()) return;

        BuildingConfig.Floor floor = building.get(currentFloor - 1);
        Set<String> fireRoomsOnFloor = new HashSet<>();

        for (String location : deviceStatusMap.keySet()) {
            String status = deviceStatusMap.get(location);

            if ("fire".equalsIgnoreCase(status)) {
                String roomNumber = extractRoomNumber(location);
                Integer deviceFloor = extractFloorFromDeviceId(location);

                Log.d(TAG, "Fire location: " + location + " -> Room: " + roomNumber + " Floor: " + deviceFloor);

                if (deviceFloor != null && deviceFloor == currentFloor && roomNumber != null) {
                    fireRoomsOnFloor.add(roomNumber);
                    Log.d(TAG, "Added fire room to map: " + roomNumber);
                }
            }
        }

        Log.d(TAG, "Updating floor " + currentFloor + " with " + fireRoomsOnFloor.size() + " fire rooms");
        b.floorMapView.setFloor(floor, fireRoomsOnFloor);
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
            Log.e(TAG, "Error extracting floor from: " + deviceId, e);
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
            Log.e(TAG, "Error extracting room number from: " + deviceId, e);
            return deviceId;
        }
    }

    private void makeEmergencyCall() {
        if (!hasActiveFire) {
            Toast.makeText(this, "No active fire detected", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:101"));
        startActivity(callIntent);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    showPermissionRationale();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                if (prefs.getBoolean("monitoring_enabled", true)) {
                    startMonitoringService();
                }
            }
        } else {
            if (prefs.getBoolean("monitoring_enabled", true)) {
                startMonitoringService();
            }
        }
    }

    private void showPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle("Notification Permission Required")
                .setMessage("FireWatch needs notification permission to alert you about fire emergencies in real-time. " +
                        "Without this permission, you won't receive critical fire alerts.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
                })
                .setNegativeButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNeutralButton("Cancel", null)
                .setCancelable(false)
                .show();
    }

    private void startMonitoringService() {
        Intent serviceIntent = new Intent(this, StatusPollerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void toggleMonitoring() {
        boolean isEnabled = prefs.getBoolean("monitoring_enabled", true);

        if (isEnabled) {
            stopService(new Intent(this, StatusPollerService.class));
            prefs.edit().putBoolean("monitoring_enabled", false).apply();
            Toast.makeText(this, "Fire alerts disabled", Toast.LENGTH_SHORT).show();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    showPermissionRationale();
                    return;
                }
            }

            startMonitoringService();
            prefs.edit().putBoolean("monitoring_enabled", true).apply();
            Toast.makeText(this, "Fire alerts enabled", Toast.LENGTH_SHORT).show();
        }

        updateMonitoringButton();
    }

    private void updateMonitoringButton() {
        boolean isEnabled = prefs.getBoolean("monitoring_enabled", true);
        b.btnToggleService.setText(isEnabled ? "Disable Alerts" : "Enable Alerts");
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