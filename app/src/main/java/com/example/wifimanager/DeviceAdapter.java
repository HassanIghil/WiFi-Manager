package com.example.wifimanager;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifimanager.miwifi.DO.MiWifiDeviceDO;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<MiWifiDeviceDO> deviceList;
    private Handler handler = new Handler();

    public DeviceAdapter(List<MiWifiDeviceDO> deviceList) {
        this.deviceList = deviceList;
        startRefreshing();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        MiWifiDeviceDO device = deviceList.get(position);
        holder.deviceName.setText(device.getName());
        holder.deviceMac.setText(device.getMac());
        if (device.getIp() != null && !device.getIp().isEmpty()) {
            holder.deviceIp.setText(device.getIp().get(0).getIp());
        } else {
            holder.deviceIp.setText("No IP");
        }

        // Display the connection time
        displayConnectionTime(holder, device);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public void updateDeviceList(List<MiWifiDeviceDO> newDeviceList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DeviceDiffCallback(this.deviceList, newDeviceList));
        this.deviceList.clear();
        this.deviceList.addAll(newDeviceList);
        diffResult.dispatchUpdatesTo(this);
    }

    private void displayConnectionTime(DeviceViewHolder holder, MiWifiDeviceDO device) {
        long connectionTime = Long.parseLong(device.getIp().get(0).getOnline()); // Get the connection time in seconds
        long elapsedTime = connectionTime; // Use the connection time directly
        String timeString = getHumanReadableTime(elapsedTime);
        holder.deviceConnectionTime.setText(timeString);
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

    private void startRefreshing() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged(); // Refresh the entire list
                handler.postDelayed(this, 60000); // Refresh every minute
            }
        }, 60000); // Initial delay of 1 minute
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {

        TextView deviceName;
        TextView deviceMac;
        TextView deviceIp;
        TextView deviceConnectionTime;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceMac = itemView.findViewById(R.id.deviceMac);
            deviceIp = itemView.findViewById(R.id.deviceIp);
            deviceConnectionTime = itemView.findViewById(R.id.deviceConnectionTime);
        }
    }

    static class DeviceDiffCallback extends DiffUtil.Callback {

        private final List<MiWifiDeviceDO> oldList;
        private final List<MiWifiDeviceDO> newList;

        public DeviceDiffCallback(List<MiWifiDeviceDO> oldList, List<MiWifiDeviceDO> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getMac().equals(newList.get(newItemPosition).getMac());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            MiWifiDeviceDO oldDevice = oldList.get(oldItemPosition);
            MiWifiDeviceDO newDevice = newList.get(newItemPosition);
            return oldDevice.equals(newDevice);
        }
    }
}