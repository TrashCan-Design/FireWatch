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

public class UserStatusAdapter extends RecyclerView.Adapter<UserStatusAdapter.VH> {

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
                .inflate(R.layout.item_status_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ApiModels.StatusRow row = data.get(pos);

        String displayText = row.getDisplayName();
        h.txtLocation.setText(displayText);
        h.txtTime.setText("Updated: " + formatTime(row.last_updated));

        // Check maintenance mode first
        if (!row.isSystemActive()) {
            h.chip.setText("Maintenance");
            h.chip.setChipBackgroundColorResource(R.color.status_unknown_light);
            h.chip.setTextColor(h.itemView.getContext().getColor(android.R.color.darker_gray));
            h.statusIndicator.setBackgroundColor(
                    h.itemView.getContext().getColor(R.color.status_unknown)
            );
            // Gray out the entire card
            h.itemView.setAlpha(0.5f);
            h.txtLocation.setTextColor(h.itemView.getContext().getColor(android.R.color.darker_gray));
        } else {
            h.itemView.setAlpha(1.0f);
            h.txtLocation.setTextColor(h.itemView.getContext().getColor(R.color.on_surface));

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
                h.chip.setChipBackgroundColorResource(R.color.status_unknown_light);
                h.chip.setTextColor(h.itemView.getContext().getColor(android.R.color.black));
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
        TextView txtLocation, txtTime;
        View statusIndicator;
        Chip chip;

        VH(View v) {
            super(v);
            txtLocation = v.findViewById(R.id.txtLocation);
            txtTime = v.findViewById(R.id.txtDeviceTime);
            chip = v.findViewById(R.id.chipDeviceStatus);
            statusIndicator = v.findViewById(R.id.statusIndicator);
        }
    }
}