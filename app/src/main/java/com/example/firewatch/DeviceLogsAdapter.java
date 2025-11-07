package com.example.firewatch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class DeviceLogsAdapter extends RecyclerView.Adapter<DeviceLogsAdapter.VH> {

    private final List<ApiModels.LogRow> data = new ArrayList<>();
    private final String currentLocation;
    private final String currentBlock;

    public DeviceLogsAdapter(String currentLocation, String currentBlock) {
        this.currentLocation = currentLocation;
        this.currentBlock = currentBlock;
    }

    public void setData(List<ApiModels.LogRow> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log_admin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ApiModels.LogRow log = data.get(pos);

        h.txtDeviceId.setText(log.esp32_id);
        h.txtLogStatus.setText(log.status.toUpperCase());
        h.txtLogTime.setText(formatTime(log.timestamp));

        // Check if location has changed
        boolean locationChanged = false;
        String logLocation = log.location != null ? log.location : "";
        String logBlock = log.block != null ? log.block : "";
        String curLocation = currentLocation != null ? currentLocation : "";
        String curBlock = currentBlock != null ? currentBlock : "";

        if (!logLocation.equals(curLocation) || !logBlock.equals(curBlock)) {
            locationChanged = true;
        }

        // Apply dark gray theme if location changed (sensor reassigned)
        if (locationChanged && (log.location != null || log.block != null)) {
            h.itemView.setAlpha(0.5f);
            h.txtDeviceId.setTextColor(h.itemView.getContext().getColor(android.R.color.darker_gray));
            h.txtLogStatus.setTextColor(h.itemView.getContext().getColor(android.R.color.darker_gray));
            h.txtLogTime.setTextColor(h.itemView.getContext().getColor(android.R.color.darker_gray));

            h.chipSeverity.setText("Sensor Changed");
            h.chipSeverity.setChipBackgroundColorResource(R.color.status_unknown_light);
            h.chipSeverity.setTextColor(h.itemView.getContext().getColor(android.R.color.darker_gray));
            h.logStatusIndicator.setBackgroundColor(
                    h.itemView.getContext().getColor(R.color.status_unknown)
            );

            // Show old location info
            String oldLocationText = "";
            if (log.location != null && !log.location.isEmpty()) {
                oldLocationText = "Old: " + log.location;
            }
            if (log.block != null && !log.block.isEmpty()) {
                if (!oldLocationText.isEmpty()) oldLocationText += " - ";
                oldLocationText += log.block;
            }
            if (!oldLocationText.isEmpty()) {
                h.txtLogStatus.setText(log.status.toUpperCase() + " (" + oldLocationText + ")");
            }
        } else {
            h.itemView.setAlpha(1.0f);

            boolean isFire = "fire".equalsIgnoreCase(log.status);
            boolean isOffline = "failed".equalsIgnoreCase(log.status);

            if (isFire) {
                int fireColor = h.itemView.getContext().getColor(R.color.status_fire);
                h.txtLogStatus.setTextColor(fireColor);
                h.chipSeverity.setText("Critical");
                h.chipSeverity.setChipBackgroundColorResource(R.color.status_fire);
                h.chipSeverity.setTextColor(h.itemView.getContext().getColor(android.R.color.white));
                h.logStatusIndicator.setBackgroundColor(fireColor);
                h.txtDeviceId.setTextColor(h.itemView.getContext().getColor(R.color.on_surface));
                h.txtLogTime.setTextColor(h.itemView.getContext().getColor(R.color.on_surface_variant));
            } else if (isOffline) {
                int offlineColor = h.itemView.getContext().getColor(R.color.status_unknown);
                h.txtLogStatus.setText("OFFLINE");
                h.txtLogStatus.setTextColor(offlineColor);
                h.chipSeverity.setText("Offline");
                h.chipSeverity.setChipBackgroundColorResource(R.color.status_unknown);
                h.chipSeverity.setTextColor(h.itemView.getContext().getColor(android.R.color.darker_gray));
                h.logStatusIndicator.setBackgroundColor(offlineColor);
                h.txtDeviceId.setTextColor(h.itemView.getContext().getColor(R.color.on_surface));
                h.txtLogTime.setTextColor(h.itemView.getContext().getColor(R.color.on_surface_variant));
            } else {
                int safeColor = h.itemView.getContext().getColor(R.color.status_safe);
                h.txtLogStatus.setTextColor(safeColor);
                h.chipSeverity.setText("Normal");
                h.chipSeverity.setChipBackgroundColorResource(R.color.status_safe_light);
                h.chipSeverity.setTextColor(safeColor);
                h.logStatusIndicator.setBackgroundColor(safeColor);
                h.txtDeviceId.setTextColor(h.itemView.getContext().getColor(R.color.on_surface));
                h.txtLogTime.setTextColor(h.itemView.getContext().getColor(R.color.on_surface_variant));
            }
        }
    }

    private String formatTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "Unknown";
        try {
            return timestamp.substring(0, Math.min(19, timestamp.length())).replace('T', ' ');
        } catch (Exception e) {
            return timestamp;
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtDeviceId, txtLogStatus, txtLogTime;
        Chip chipSeverity;
        View logStatusIndicator;

        VH(View v) {
            super(v);
            txtDeviceId = v.findViewById(R.id.txtDeviceId);
            txtLogStatus = v.findViewById(R.id.txtLogStatus);
            txtLogTime = v.findViewById(R.id.txtLogTime);
            chipSeverity = v.findViewById(R.id.chipSeverity);
            logStatusIndicator = v.findViewById(R.id.logStatusIndicator);
        }
    }
}