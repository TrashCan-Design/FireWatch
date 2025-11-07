package com.example.firewatch;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceLogsActivity extends AppCompatActivity {

    private RecyclerView recyclerLogs;
    private AdminLogsAdapter logsAdapter;
    private MaterialButton btnRefresh;
    private LinearProgressIndicator progressBar;
    private String deviceId;
    private String location;
    private String block;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_logs);

        // Get device info from intent
        deviceId = getIntent().getStringExtra("device_id");
        location = getIntent().getStringExtra("location");
        block = getIntent().getStringExtra("block");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            // Set title with device info
            String title = "Logs: " + deviceId;
            if (location != null && !location.isEmpty()) {
                title = "Logs: " + location;
                if (block != null && !block.isEmpty()) {
                    title += " - " + block;
                }
            }
            getSupportActionBar().setTitle(title);
        }

        recyclerLogs = findViewById(R.id.recyclerLogs);
        btnRefresh = findViewById(R.id.btnRefresh);
        progressBar = findViewById(R.id.progressBar);

        recyclerLogs.setLayoutManager(new LinearLayoutManager(this));
        logsAdapter = new AdminLogsAdapter();
        recyclerLogs.setAdapter(logsAdapter);

        btnRefresh.setOnClickListener(v -> loadLogs());

        loadLogs();
    }

    private void loadLogs() {
        if (deviceId == null || deviceId.isEmpty()) {
            Toast.makeText(this, "Invalid device ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        SupabaseApi api = ApiClient.api(this);

        // Get all logs and filter by device ID
        api.getLogs().enqueue(new Callback<List<ApiModels.LogRow>>() {
            @Override
            public void onResponse(Call<List<ApiModels.LogRow>> call, Response<List<ApiModels.LogRow>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    // Filter logs for this specific device
                    List<ApiModels.LogRow> deviceLogs = new ArrayList<>();
                    for (ApiModels.LogRow log : response.body()) {
                        if (deviceId.equals(log.esp32_id)) {
                            deviceLogs.add(log);
                        }
                    }

                    if (deviceLogs.isEmpty()) {
                        Toast.makeText(DeviceLogsActivity.this,
                                "No logs found for this device", Toast.LENGTH_SHORT).show();
                    }

                    logsAdapter.setData(deviceLogs);
                } else {
                    Toast.makeText(DeviceLogsActivity.this,
                            "Failed to load logs", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ApiModels.LogRow>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DeviceLogsActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}