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

public class AdminLogsAdapter extends RecyclerView.Adapter<AdminLogsAdapter.VH> {

    private final List<ApiModels.LogRow> data = new ArrayList<>();

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

        boolean isFire = "fire".equalsIgnoreCase(log.status);
        boolean isOffline = "failed".equalsIgnoreCase(log.status);

        if (isFire) {
            int fireColor = h.itemView.getContext().getColor(R.color.status_fire);
            h.txtLogStatus.setTextColor(fireColor);
            h.chipSeverity.setText("Critical");
            h.chipSeverity.setChipBackgroundColorResource(R.color.status_fire);
            h.chipSeverity.setTextColor(h.itemView.getContext().getColor(android.R.color.white));
            h.logStatusIndicator.setBackgroundColor(fireColor);
        } else if (isOffline) {
            int offlineColor = h.itemView.getContext().getColor(R.color.status_unknown);
            h.txtLogStatus.setText("OFFLINE");
            h.txtLogStatus.setTextColor(offlineColor);
            h.chipSeverity.setText("Offline");
            h.chipSeverity.setChipBackgroundColorResource(R.color.status_unknown);
            h.chipSeverity.setTextColor(h.itemView.getContext().getColor(android.R.color.darker_gray));
            h.logStatusIndicator.setBackgroundColor(offlineColor);
        } else {
            int safeColor = h.itemView.getContext().getColor(R.color.status_safe);
            h.txtLogStatus.setTextColor(safeColor);
            h.chipSeverity.setText("Normal");
            h.chipSeverity.setChipBackgroundColorResource(R.color.status_safe_light);
            h.chipSeverity.setTextColor(safeColor);
            h.logStatusIndicator.setBackgroundColor(safeColor);
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