package com.example.wifimanager.Settings_Components;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifimanager.R;

import java.util.List;

public class BlockedDeviceAdapter extends RecyclerView.Adapter<BlockedDeviceAdapter.DeviceViewHolder> {

    private List<Device> devices;
    private OnDeviceUnblockListener listener;

    public interface OnDeviceUnblockListener {
        void onUnblock(Device device);
    }

    public BlockedDeviceAdapter(List<Device> devices, OnDeviceUnblockListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_blocked_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        holder.bind(devices.get(position));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void updateDevices(List<Device> newDevices) {
        this.devices = newDevices;
        notifyDataSetChanged();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private ImageView deviceIcon;
        private TextView deviceName;
        private TextView deviceMac;
        private TextView deviceIp;
        private ImageButton unblockButton;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceIcon = itemView.findViewById(R.id.deviceIcon);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceMac = itemView.findViewById(R.id.deviceMac);
            deviceIp = itemView.findViewById(R.id.deviceIp);
            unblockButton = itemView.findViewById(R.id.unblockButton);
        }

        void bind(Device device) {
            // Set device name (use MAC if name is empty)
            String displayName = device.getName();
            if (displayName == null || displayName.isEmpty() || displayName.equals(device.getMac())) {
                displayName = "Unknown Device";
            }
            deviceName.setText(displayName);

            // Set MAC address
            deviceMac.setText(device.getMac());

            // Set IP address
            String ipText = device.getIp();
            if (ipText == null || ipText.isEmpty() || ipText.equals("0.0.0.0")) {
                ipText = "IP: Unknown";
            } else if (!ipText.startsWith("IP: ")) {
                ipText = "IP: " + ipText;
            }
            deviceIp.setText(ipText);

            // Set device icon
            int iconRes = "cable".equals(device.getType())
                    ? R.drawable.ic_ethernet
                    : R.drawable.wifi;
            deviceIcon.setImageResource(iconRes);

            unblockButton.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onUnblock(devices.get(getAdapterPosition()));
                }
            });
        }
    }
}