package com.example.firewatch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class DeviceManagementAdapter extends RecyclerView.Adapter<DeviceManagementAdapter.ViewHolder> {

    private final List<ApiModels.StatusRow> devices = new ArrayList<>();
    private final OnEditClickListener listener;

    public interface OnEditClickListener {
        void onEditClick(ApiModels.StatusRow device);
    }

    public DeviceManagementAdapter(OnEditClickListener listener) {
        this.listener = listener;
    }

    public void setDevices(List<ApiModels.StatusRow> newDevices) {
        devices.clear();
        if (newDevices != null) {
            devices.addAll(newDevices);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_management, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApiModels.StatusRow device = devices.get(position);

        holder.txtDeviceId.setText(device.esp32_id);

        String locationText = device.location != null && !device.location.isEmpty()
                ? device.location : "Not set";
        holder.txtLocation.setText("Location: " + locationText);

        String blockText = device.block != null && !device.block.isEmpty()
                ? device.block : "Not set";
        holder.txtBlock.setText("Block: " + blockText);

        holder.txtStatus.setText("Status: " + device.last_status);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDeviceId;
        TextView txtLocation;
        TextView txtBlock;
        TextView txtStatus;
        MaterialButton btnEdit;

        ViewHolder(View itemView) {
            super(itemView);
            txtDeviceId = itemView.findViewById(R.id.txtDeviceId);
            txtLocation = itemView.findViewById(R.id.txtLocation);
            txtBlock = itemView.findViewById(R.id.txtBlock);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}