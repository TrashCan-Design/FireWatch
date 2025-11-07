package com.example.firewatch;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDeviceManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerDevices;
    private DeviceManagementAdapter adapter;
    private MaterialButton btnRefresh;
    private LinearProgressIndicator progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_device_management);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Device Management");
        }

        recyclerDevices = findViewById(R.id.recyclerDevices);
        btnRefresh = findViewById(R.id.btnRefresh);
        progressBar = findViewById(R.id.progressBar);

        recyclerDevices.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceManagementAdapter(this::showEditDialog);
        recyclerDevices.setAdapter(adapter);

        btnRefresh.setOnClickListener(v -> loadDevices());

        loadDevices();
    }

    private void loadDevices() {
        progressBar.setVisibility(View.VISIBLE);
        SupabaseApi api = ApiClient.api(this);

        api.getStatus().enqueue(new Callback<List<ApiModels.StatusRow>>() {
            @Override
            public void onResponse(Call<List<ApiModels.StatusRow>> call, Response<List<ApiModels.StatusRow>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setDevices(response.body());
                } else {
                    Toast.makeText(AdminDeviceManagementActivity.this,
                            "Failed to load devices", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ApiModels.StatusRow>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminDeviceManagementActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditDialog(ApiModels.StatusRow device) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_device, null);

        TextInputEditText edtLocation = dialogView.findViewById(R.id.edtLocation);
        TextInputEditText edtBlock = dialogView.findViewById(R.id.edtBlock);

        edtLocation.setText(device.location != null ? device.location : "");
        edtBlock.setText(device.block != null ? device.block : "");

        new AlertDialog.Builder(this)
                .setTitle("Edit Device: " + device.esp32_id)
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String location = edtLocation.getText() != null ?
                            edtLocation.getText().toString().trim() : "";
                    String block = edtBlock.getText() != null ?
                            edtBlock.getText().toString().trim() : "";

                    if (location.isEmpty() && block.isEmpty()) {
                        Toast.makeText(this, "Please enter at least one field",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateDevice(device.esp32_id, location, block);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateDevice(String esp32Id, String location, String block) {
        progressBar.setVisibility(View.VISIBLE);

        ApiModels.UpdateDeviceLocationRequest request = new ApiModels.UpdateDeviceLocationRequest();
        request.location = location.isEmpty() ? null : location;
        request.block = block.isEmpty() ? null : block;

        SupabaseApi api = ApiClient.api(this);

        api.updateDeviceLocation(esp32Id, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(AdminDeviceManagementActivity.this,
                            "Device updated successfully", Toast.LENGTH_SHORT).show();
                    loadDevices();
                } else {
                    Toast.makeText(AdminDeviceManagementActivity.this,
                            "Update failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminDeviceManagementActivity.this,
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