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

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.VH> {

    private final List<ApiModels.LogRow> data = new ArrayList<>();

    // 🔄 Update adapter data
    public void setData(List<ApiModels.LogRow> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ApiModels.LogRow log = data.get(pos);

        // Set main text
        h.txtLogStatus.setText(log.status);
        h.txtLogTime.setText(log.timestamp);

        // 🔥 Determine color & label based on status
        if (log.status != null && log.status.equalsIgnoreCase("fire")) {
            // FIRE = Critical 🔴
            int fireColor = h.itemView.getContext().getColor(R.color.status_fire);
            h.txtLogStatus.setTextColor(fireColor);
            h.chipSeverity.setText("Critical");
            h.chipSeverity.setChipBackgroundColorResource(R.color.status_fire);
            h.chipSeverity.setChipStrokeColorResource(R.color.status_fire);
            h.logStatusIndicator.setBackgroundColor(fireColor);
        } else {
            // SAFE = Normal 🟢
            int safeColor = h.itemView.getContext().getColor(R.color.status_safe);
            h.txtLogStatus.setTextColor(safeColor);
            h.chipSeverity.setText("Normal");
            h.chipSeverity.setChipBackgroundColorResource(R.color.status_safe);
            h.chipSeverity.setChipStrokeColorResource(R.color.status_safe);
            h.logStatusIndicator.setBackgroundColor(safeColor);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // 🧩 ViewHolder
    static class VH extends RecyclerView.ViewHolder {
        TextView txtLogStatus, txtLogTime;
        Chip chipSeverity;
        View logStatusIndicator;

        VH(View v) {
            super(v);
            txtLogStatus = v.findViewById(R.id.txtLogStatus);
            txtLogTime = v.findViewById(R.id.txtLogTime);
            chipSeverity = v.findViewById(R.id.chipSeverity);
            logStatusIndicator = v.findViewById(R.id.logStatusIndicator);
        }
    }
}