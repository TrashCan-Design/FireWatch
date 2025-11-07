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
    private AdminLogsAdapter logsAdapter;
    private AdminStatusAdapter statusAdapter;
    private List<BuildingConfig.Floor> building;
    private Map<String, String> deviceStatusMap;
    private int currentFloor = 1;
    private SharedPreferences prefs;

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
        b.recyclerStatus.setNestedScrollingEnabled(true);
        statusAdapter = new AdminStatusAdapter();
        b.recyclerStatus.setAdapter(statusAdapter);

        b.recyclerLogs.setLayoutManager(new LinearLayoutManager(this));
        b.recyclerLogs.setNestedScrollingEnabled(true);
        logsAdapter = new AdminLogsAdapter();
        b.recyclerLogs.setAdapter(logsAdapter);
    }

    private void setupButtons() {
        b.btnRefresh.setOnClickListener(v -> fetchAll());
        b.btnToggleService.setOnClickListener(v -> toggleService());
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
                    updateUI(new ArrayList<>(), "--", 0);
                    return;
                }

                LinkedHashMap<String, ApiModels.StatusRow> latest = new LinkedHashMap<>();
                for (ApiModels.StatusRow row : res.body()) {
                    if (!latest.containsKey(row.location)) {
                        latest.put(row.location, row);
                    }
                }

                deviceStatusMap.clear();
                for (ApiModels.StatusRow row : latest.values()) {
                    deviceStatusMap.put(row.location, row.last_status);
                }

                statusAdapter.setData(new ArrayList<>(latest.values()));

                List<String> fires = new ArrayList<>();
                String newestTime = "--";
                for (ApiModels.StatusRow s : latest.values()) {
                    if ("fire".equalsIgnoreCase(s.last_status)) {
                        fires.add(s.esp32_id);
                    }
                    if (s.last_updated != null && (newestTime.equals("--") ||
                            s.last_updated.compareTo(newestTime) > 0)) {
                        newestTime = s.last_updated;
                    }
                }

                updateUI(fires, newestTime, latest.size());
                updateFloorMap();
            }

            @Override
            public void onFailure(Call<List<ApiModels.StatusRow>> call, Throwable t) {
                b.txtStatus.setText("Offline - " + t.getMessage());
                b.txtLastUpdated.setText("Last updated: --");
                statusAdapter.setData(new ArrayList<>());
            }
        });

        api.getLogs().enqueue(new Callback<List<ApiModels.LogRow>>() {
            @Override
            public void onResponse(Call<List<ApiModels.LogRow>> call, Response<List<ApiModels.LogRow>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    logsAdapter.setData(res.body());
                }
            }

            @Override
            public void onFailure(Call<List<ApiModels.LogRow>> call, Throwable t) {}
        });
    }

    private void updateUI(List<String> fires, String newestTime, int totalDevices) {
        if (!fires.isEmpty()) {
            b.txtStatus.setText("🔥 FIRE DETECTED on " + fires.size() + " device(s)");
            b.txtStatus.setTextColor(getColor(android.R.color.white));
            b.chipStatus.setText("FIRE");
            b.chipStatus.setChipBackgroundColorResource(R.color.status_fire);
            b.cardStatus.setCardBackgroundColor(getColor(R.color.status_fire_light));
            b.btnEmergencyCall.setVisibility(View.VISIBLE);

            Integer fireFloor = extractFloorFromDeviceId(fires.get(0));
            if (fireFloor != null && fireFloor >= 1 && fireFloor <= 8) {
                currentFloor = fireFloor;
                b.spinnerFloor.setSelection(fireFloor - 1);
            }

        } else if (totalDevices > 0) {
            b.txtStatus.setText("✅ All Systems Safe");
            b.txtStatus.setTextColor(getColor(R.color.status_safe));
            b.chipStatus.setText("SAFE");
            b.chipStatus.setChipBackgroundColorResource(R.color.status_safe_light);
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

    private void updateFloorMap() {
        if (currentFloor < 1 || currentFloor > building.size()) return;

        BuildingConfig.Floor floor = building.get(currentFloor - 1);
        Set<String> fireRooms = new HashSet<>();

        for (String location : deviceStatusMap.keySet()) {
            String status = deviceStatusMap.get(location);
            if ("fire".equalsIgnoreCase(status)) {
                fireRooms.add(location);
            }
        }

        b.floorMapView.setFloor(floor, fireRooms);
    }

    private Integer extractFloorFromDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) return null;
        try {
            return Integer.parseInt(deviceId.substring(0, 1));
        } catch (Exception e) {
            return null;
        }
    }

    private void makeEmergencyCall() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:101"));
        startActivity(callIntent);
    }

    private void toggleService() {
        boolean isEnabled = prefs.getBoolean("monitoring_enabled", true);

        if (isEnabled) {
            stopService(new Intent(this, StatusPollerService.class));
            prefs.edit().putBoolean("monitoring_enabled", false).apply();
        } else {
            startForegroundService(new Intent(this, StatusPollerService.class));
            prefs.edit().putBoolean("monitoring_enabled", true).apply();
        }

        updateMonitoringButton();
    }

    private void updateMonitoringButton() {
        boolean isEnabled = prefs.getBoolean("monitoring_enabled", true);
        b.btnToggleService.setText(isEnabled ? "Stop Monitoring" : "Start Monitoring");
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
}