package com.example.wifimanager;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.Gson;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifimanager.Devices_List.DeviceDetailsActivity;
import com.example.wifimanager.miwifi.DO.MiWifiDeviceDO;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private List<MiWifiDeviceDO> deviceList;
    private int connectedDevicesCount;
    private final OnItemClickListener listener;
    private String stok;

    public interface OnItemClickListener {
        void onItemClick(MiWifiDeviceDO device);
    }

    public DeviceAdapter(List<MiWifiDeviceDO> deviceList, OnItemClickListener listener, String stok) {
        this.deviceList = deviceList;
        this.listener = listener;
        this.stok = stok;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_item, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
            return new DeviceViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.connectedDevicesTextView.setText("Connected Devices: " + connectedDevicesCount);
        } else {
            DeviceViewHolder deviceHolder = (DeviceViewHolder) holder;
            MiWifiDeviceDO device = deviceList.get(position - 1); // Adjust for header
            deviceHolder.deviceName.setText(device.getName());
            deviceHolder.deviceConnectionTime.setText(getHumanReadableTime(Long.parseLong(device.getIp().get(0).getOnline())));

            // Concatenate MAC address and IP address
            String macAddress = "MAC: " + device.getMac();
            String ipAddress = "IP: " + (device.getIp() != null && !device.getIp().isEmpty() ? device.getIp().get(0).getIp() : "No IP");
            deviceHolder.deviceMac.setText(macAddress);
            deviceHolder.deviceIp.setText(ipAddress);

            // Set device image
            int deviceImageRes = getDeviceImageResource(device.getName());
            deviceHolder.deviceImage.setImageResource(deviceImageRes);

            // Update speed display
            String uploadSpeed = device.getIp().get(0).getUpspeed();
            String downloadSpeed = device.getIp().get(0).getDownspeed();

            // Set click listener with image resource
            deviceHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    Intent intent = new Intent(v.getContext(), DeviceDetailsActivity.class);
                    intent.putExtra("DEVICE_NAME", device.getName());
                    intent.putExtra("DEVICE_IP", device.getIp().get(0).getIp());
                    intent.putExtra("DEVICE_MAC", device.getMac());
                    intent.putExtra("DEVICE_IMAGE", deviceImageRes);
                    intent.putExtra("STOK", stok); // Ensure STOK is passed
                    intent.putExtra("UPLOAD_SPEED", device.getIp().get(0).getUpspeed());
                    intent.putExtra("DOWNLOAD_SPEED", device.getIp().get(0).getDownspeed());
                    v.getContext().startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return deviceList.size() + 1; // Add one for the header
    }

    public void updateDeviceList(List<MiWifiDeviceDO> newDeviceList) {
        this.deviceList = newDeviceList;
        this.connectedDevicesCount = newDeviceList.size();
        notifyDataSetChanged();
    }

    private String getHumanReadableTime(long seconds) {
        if (seconds < 60) {
            return "just now";
        } else if (seconds < 3600) {
            return (seconds / 60) + " min ago";
        } else if (seconds < 86400) {
            return (seconds / 3600) + " hr" + (seconds / 3600 > 1 ? "s" : "") + " ago";
        } else {
            return (seconds / 86400) + " day" + (seconds / 86400 > 1 ? "s" : "") + " ago";
        }
    }

    private int getDeviceImageResource(String deviceName) {
        if (deviceName.toLowerCase().contains("huawei")) {
            return R.drawable.huawei;
        } else if (deviceName.toLowerCase().contains("apple") || deviceName.toLowerCase().contains("iphone") || deviceName.toLowerCase().contains("ipad")) {
            return R.drawable.apple;
        } else if (deviceName.toLowerCase().contains("samsung")) {
            return R.drawable.samsung;
        } else if (deviceName.toLowerCase().contains("desktop") || deviceName.toLowerCase().contains("pc")) {
            return R.drawable.pc;
        } else if (deviceName.toLowerCase().contains("tv") || deviceName.toLowerCase().contains("xbox")) {
            return R.drawable.tv;
        } else {
            return R.drawable.android;
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView connectedDevicesTextView;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            connectedDevicesTextView = itemView.findViewById(R.id.connectedDevicesTextView);
        }
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceConnectionTime;
        TextView deviceMac;
        TextView deviceIp;
        ImageView deviceImage;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceConnectionTime = itemView.findViewById(R.id.deviceConnectionTime);
            deviceMac = itemView.findViewById(R.id.deviceMac);
            deviceIp = itemView.findViewById(R.id.deviceIp);
            deviceImage = itemView.findViewById(R.id.deviceIcon);
        }
    }
}