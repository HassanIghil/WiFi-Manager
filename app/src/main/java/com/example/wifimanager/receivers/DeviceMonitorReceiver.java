package com.example.wifimanager.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.wifimanager.R;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class DeviceMonitorReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "device_monitoring";
    private static final String TAG = "DeviceMonitorReceiver";
    private static Map<String, Boolean> lastKnownState = new HashMap<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        String deviceMac = intent.getStringExtra("device_mac");
        String stok = intent.getStringExtra("stok");
        
        createNotificationChannel(context);
        checkDeviceStatus(context, deviceMac, stok);
    }

    private void checkDeviceStatus(Context context, String deviceMac, String stok) {
        new Thread(() -> {
            try {
                String apiUrl = "http://192.168.31.1/cgi-bin/luci/;stok=" + stok + "/api/misystem/devicelist";
                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JsonObject jsonResponse = new Gson().fromJson(response.toString(), JsonObject.class);
                    JsonArray deviceList = jsonResponse.getAsJsonArray("list");

                    boolean deviceFound = false;
                    String deviceName = "";

                    // First, find if the device exists and get its name
                    for (int i = 0; i < deviceList.size(); i++) {
                        JsonObject device = deviceList.get(i).getAsJsonObject();
                        if (deviceMac.equals(device.get("mac").getAsString())) {
                            deviceFound = true;
                            deviceName = device.get("name").getAsString();
                            break;
                        }
                    }
                    
                    // Now handle connection/disconnection
                    Boolean wasConnected = lastKnownState.get(deviceMac);
                    if (deviceFound) {
                        // Device is connected
                        if (wasConnected == null || !wasConnected) {
                            // Only show notification if it wasn't connected before
                            showConnectionNotification(context, deviceName);
                        }
                        lastKnownState.put(deviceMac, true);
                    } else {
                        // Device is disconnected
                        if (wasConnected == null || wasConnected) {
                            // Get the device name from SharedPreferences for disconnection notification
                            SharedPreferences prefs = context.getSharedPreferences("device_monitor", Context.MODE_PRIVATE);
                            String storedDeviceName = prefs.getString("device_name_" + deviceMac, "Unknown Device");
                            showDisconnectionNotification(context, storedDeviceName);
                        }
                        lastKnownState.put(deviceMac, false);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking device status: " + e.getMessage());
            }
        }).start();
    }

    private void showConnectionNotification(Context context, String deviceName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(deviceName + " Connected")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(deviceName.hashCode(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "No permission to show notification", e);
        }
    }

    private void showDisconnectionNotification(Context context, String deviceName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(deviceName + " Disconnected")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(deviceName.hashCode(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "No permission to show notification", e);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Device Monitoring",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Monitors device connection status");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}