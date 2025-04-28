package com.example.wifimanager.Devices_List;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.wifimanager.R;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class DeviceMonitorWorker extends Worker {
    private static final String CHANNEL_ID = "device_monitor_channel";
    private static final String CHANNEL_NAME = "Device Monitor";
    private static final String TAG = "DeviceMonitorWorker";
    private static final String PREFS_NAME = "device_monitor";
    private static final String LAST_SEEN_DEVICES = "last_seen_devices";

    public DeviceMonitorWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        createNotificationChannel();
    }

    @Override
    public @NonNull Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String stok = prefs.getString("stok", "");
        boolean isGlobalMonitoring = prefs.getBoolean("global_monitoring_enabled", false);

        if (stok.isEmpty()) {
            Log.e(TAG, "Missing stok");
            return Result.failure();
        }

        try {
            String apiUrl = "http://192.168.31.1/cgi-bin/luci/;stok=" + stok + "/api/misystem/devicelist";
            HttpURLConnection connection = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
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

                // Get previously seen devices
                Set<String> lastSeenDevices = prefs.getStringSet(LAST_SEEN_DEVICES, new HashSet<>());
                Set<String> currentDevices = new HashSet<>();

                for (int i = 0; i < deviceList.size(); i++) {
                    JsonObject device = deviceList.get(i).getAsJsonObject();
                    String mac = device.get("mac").getAsString();
                    String name = device.has("name") ? device.get("name").getAsString() : mac;
                    boolean isOnline = device.has("online") && device.get("online").getAsInt() == 1;

                    if (isOnline) {
                        currentDevices.add(mac);
                        
                        if (!lastSeenDevices.contains(mac)) {
                            // New device connected
                            if (isGlobalMonitoring || prefs.getBoolean("monitoring_" + mac, false)) {
                                showNotification(context, name);
                            }
                        }
                    }
                }

                // Update last seen devices
                prefs.edit().putStringSet(LAST_SEEN_DEVICES, currentDevices).apply();
            }
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error monitoring devices: " + e.getMessage());
            return Result.retry();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context context = getApplicationContext();
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Device connectivity notifications");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(Context context, String deviceName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Device Connected")
                .setContentText(deviceName + " has connected to the network")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}