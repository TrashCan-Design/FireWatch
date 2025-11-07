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

public class AdminStatusAdapter extends RecyclerView.Adapter<AdminStatusAdapter.VH> {

    private final List<ApiModels.StatusRow> data = new ArrayList<>();

    public void setData(List<ApiModels.StatusRow> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_status_admin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ApiModels.StatusRow row = data.get(pos);

        h.txtDeviceId.setText(row.esp32_id);

        String locationText = "";
        if (row.location != null && !row.location.isEmpty()) {
            locationText = "Location: " + row.location;
        }
        if (row.block != null && !row.block.isEmpty()) {
            if (!locationText.isEmpty()) locationText += " | ";
            locationText += "Block: " + row.block;
        }
        if (locationText.isEmpty()) {
            locationText = "No location data";
        }
        h.txtLocation.setText(locationText);

        h.txtTime.setText("Updated: " + formatTime(row.last_updated));

        boolean isFire = "fire".equalsIgnoreCase(row.last_status);
        boolean isOffline = "failed".equalsIgnoreCase(row.last_status);

        if (isFire) {
            h.chip.setText("Fire");
            h.chip.setChipBackgroundColorResource(R.color.status_fire);
            h.chip.setTextColor(h.itemView.getContext().getColor(android.R.color.white));
            h.statusIndicator.setBackgroundColor(
                    h.itemView.getContext().getColor(R.color.status_fire)
            );
        } else if (isOffline) {
            h.chip.setText("Offline");
            h.chip.setChipBackgroundColorResource(R.color.status_unknown);
            h.chip.setTextColor(h.itemView.getContext().getColor(android.R.color.darker_gray));
            h.statusIndicator.setBackgroundColor(
                    h.itemView.getContext().getColor(R.color.status_unknown)
            );
        } else {
            h.chip.setText("Safe");
            h.chip.setChipBackgroundColorResource(R.color.status_safe_light);
            h.chip.setTextColor(h.itemView.getContext().getColor(R.color.status_safe));
            h.statusIndicator.setBackgroundColor(
                    h.itemView.getContext().getColor(R.color.status_safe)
            );
        }
    }

    private String formatTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "Unknown";
        try {
            return timestamp.substring(0, Math.min(16, timestamp.length())).replace('T', ' ');
        } catch (Exception e) {
            return timestamp;
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtDeviceId, txtLocation, txtTime;
        View statusIndicator;
        Chip chip;

        VH(View v) {
            super(v);
            txtDeviceId = v.findViewById(R.id.txtDeviceId);
            txtLocation = v.findViewById(R.id.txtLocation);
            txtTime = v.findViewById(R.id.txtDeviceTime);
            chip = v.findViewById(R.id.chipDeviceStatus);
            statusIndicator = v.findViewById(R.id.statusIndicator);
        }
    }
}