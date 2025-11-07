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

public class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.VH> {

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
                .inflate(R.layout.item_status, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ApiModels.StatusRow row = data.get(pos);

        h.txtId.setText(row.esp32_id);
        h.txtTime.setText("Updated: " + row.last_updated);
        h.chip.setText(row.last_status);  // ✅ fixed

        int color = row.last_status.equalsIgnoreCase("fire")  // ✅ fixed
                ? h.itemView.getContext().getColor(R.color.status_fire)
                : h.itemView.getContext().getColor(R.color.status_safe);

        h.chip.setChipBackgroundColorResource(
                row.last_status.equalsIgnoreCase("fire")  // ✅ fixed
                        ? R.color.status_fire : R.color.status_safe);

        h.statusIndicator.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtId, txtTime;
        View statusIndicator;
        Chip chip;

        VH(View v) {
            super(v);
            txtId = v.findViewById(R.id.txtDeviceId);
            txtTime = v.findViewById(R.id.txtDeviceTime);
            chip = v.findViewById(R.id.chipDeviceStatus);
            statusIndicator = v.findViewById(R.id.statusIndicator);
        }
    }
}