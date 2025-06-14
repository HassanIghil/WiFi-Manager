package com.example.wifimanager.Devices_List;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.airbnb.lottie.LottieAnimationView;
import com.example.wifimanager.R;
import com.example.wifimanager.receivers.DeviceMonitorReceiver;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class DeviceDetailsActivity extends AppCompatActivity {

    private TextView uploadSpeedView;
    private TextView downloadSpeedView;
    private String deviceMac;
    private Handler handler;
    private static final int REFRESH_INTERVAL = 2000;
    private String stok;
    private ImageView notificationImageView;
    private ImageView qosImage;
    private ImageView blockuser;
    private TextView blockText;
    private SharedPreferences sharedPreferences;
    private LottieAnimationView lottieAnimationView;
    private LinearLayout bottomContent;
    private ImageView accessIcon;
    private TextView accessText;
    private TextView notificationText; // TextView for notification
    private TextView qosText; // TextView for QoS
    private TextView internetAccessText; // TextView for internet access

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_details);

        // Set up toolbar with back arrow
        Toolbar toolbar = findViewById(R.id.toolbarDevice);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Get device name from intent
        String deviceName = getIntent().getStringExtra("DEVICE_NAME");

        // Initialize views
        notificationImageView = findViewById(R.id.notification_icon);
        qosImage = findViewById(R.id.qos_icon);
        blockuser = findViewById(R.id.block_icon);
        ImageView deviceImageView = findViewById(R.id.deviceImage);
        TextView deviceNameTextView = findViewById(R.id.deviceName);
        TextView deviceIpTextView = findViewById(R.id.deviceIp);
        TextView deviceMacTextView = findViewById(R.id.deviceMac);
        uploadSpeedView = findViewById(R.id.uploadSpeed);
        downloadSpeedView = findViewById(R.id.downloadSpeed);
        blockText = findViewById(R.id.blockText);
        lottieAnimationView = findViewById(R.id.lottieAnimationView);
        bottomContent = findViewById(R.id.bottomContent);
        accessIcon = findViewById(R.id.access_icon);
        accessText = findViewById(R.id.accessText);
        notificationText = findViewById(R.id.notificationText); // Initialize notification text
        qosText = findViewById(R.id.qosText); // Initialize QoS text

        // Get data from intent
        String deviceIp = getIntent().getStringExtra("DEVICE_IP");
        deviceMac = getIntent().getStringExtra("DEVICE_MAC");
        int deviceImageRes = getIntent().getIntExtra("DEVICE_IMAGE", R.drawable.android);
        String uploadSpeed = getIntent().getStringExtra("UPLOAD_SPEED");
        String downloadSpeed = getIntent().getStringExtra("DOWNLOAD_SPEED");
        stok = getIntent().getStringExtra("STOK");

        // Set data to views
        deviceImageView.setImageResource(deviceImageRes);
        deviceNameTextView.setText(deviceName);
        deviceIpTextView.setText("IP: " + deviceIp);
        deviceMacTextView.setText("MAC: " + deviceMac);
        updateSpeedDisplay(uploadSpeed, downloadSpeed);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("image_state", MODE_PRIVATE);
        SharedPreferences monitorPrefs = getSharedPreferences("device_monitor", MODE_PRIVATE);

        // Restore notification and qos image states
        boolean isGlobalMonitoring = monitorPrefs.getBoolean("global_monitoring_enabled", false);
        if (isGlobalMonitoring) {
            // If global monitoring is on, device is automatically monitored
            notificationImageView.setImageResource(R.drawable.notificatio_active);
            notificationImageView.setEnabled(false); // Disable the button
            notificationImageView.setAlpha(0.5f); // Show it's disabled
            notificationText.setText("Monitoring (Global)");
        } else {
            // Individual device monitoring
            if (monitorPrefs.getBoolean("monitoring_" + deviceMac, false)) {
                notificationImageView.setImageResource(R.drawable.notificatio_active);
            } else {
                notificationImageView.setImageResource(R.drawable.notification);
            }
            notificationImageView.setEnabled(true);
            notificationImageView.setAlpha(1.0f);
            notificationText.setText("Monitor Device");
        }

        if (sharedPreferences.getBoolean("qos_image_active_" + deviceMac, false)) {
            qosImage.setImageResource(R.drawable.qos_active);
        } else {
            qosImage.setImageResource(R.drawable.qos);
        }

        // Setup periodic updates
        handler = new Handler(Looper.getMainLooper());
        startSpeedUpdates();

        // Set up notification image click listener
        notificationImageView.setOnClickListener(v -> {
            if (!isGlobalMonitoring) {
                // Hide images and texts
                hideViews();
                // Show the animation
                lottieAnimationView.setVisibility(View.VISIBLE);
                lottieAnimationView.playAnimation();

                // Toggle notification state
                toggleNotificationState();
            }
        });

        // Set up qos image click listener
        qosImage.setOnClickListener(v -> {
            // Hide images and texts
            hideViews();
            // Show the animation
            lottieAnimationView.setVisibility(View.VISIBLE);
            lottieAnimationView.playAnimation();

            // Toggle QoS state
            toggleQoSState();
        });

        // Set up block/unblock click listener
        blockuser.setOnClickListener(v -> {
            // Hide images and texts
            hideViews();
            // Show the animation
            lottieAnimationView.setVisibility(View.VISIBLE);
            lottieAnimationView.playAnimation();

            // Toggle block status
            toggleBlockStatus();
        });

        // Set up access icon click listener
        accessIcon.setOnClickListener(v -> {
            // Hide images and texts
            hideViews();
            // Show the animation
            lottieAnimationView.setVisibility(View.VISIBLE);
            lottieAnimationView.playAnimation();

            // Toggle internet access
            toggleInternetAccess();
        });

        // Show loading animation and hide bottom content initially
        lottieAnimationView.setVisibility(View.VISIBLE);
        bottomContent.setVisibility(View.GONE);

        // Check the block status during the loading phase
        checkBlockStatus();

        // Simulate loading delay (replace with actual data loading)
        new Handler().postDelayed(() -> {
            lottieAnimationView.setVisibility(View.GONE);
            bottomContent.setVisibility(View.VISIBLE);
        }, 3000); // Adjust the delay as needed
    }

    private void hideViews() {
        // Instead of hiding individual elements, hide their parent containers
        LinearLayout qosContainer = findViewById(R.id.qos_container);
        LinearLayout accessContainer = findViewById(R.id.access_container);
        LinearLayout blockContainer = findViewById(R.id.block_container);
        LinearLayout notificationContainer = findViewById(R.id.notification_container);

        LinearLayout bottomContent = findViewById(R.id.bottomContent);

        qosContainer.setVisibility(View.GONE);
        accessContainer.setVisibility(View.GONE);
        blockContainer.setVisibility(View.GONE);
        notificationContainer.setVisibility(View.GONE);

        bottomContent.setVisibility(View.GONE);

    }

    private void showViews() {
        // Show the parent containers instead of individual elements
        LinearLayout qosContainer = findViewById(R.id.qos_container);
        LinearLayout accessContainer = findViewById(R.id.access_container);
        LinearLayout blockContainer = findViewById(R.id.block_container);
        LinearLayout notificationContainer = findViewById(R.id.notification_container);

        LinearLayout bottomContent = findViewById(R.id.bottomContent);

        qosContainer.setVisibility(View.VISIBLE);
        accessContainer.setVisibility(View.VISIBLE);
        blockContainer.setVisibility(View.VISIBLE);
        notificationContainer.setVisibility(View.VISIBLE);

        bottomContent.setVisibility(View.VISIBLE);
    }

    private void toggleNotificationState() {
        SharedPreferences monitorPrefs = getSharedPreferences("device_monitor", MODE_PRIVATE);
        boolean isGlobalMonitoring = monitorPrefs.getBoolean("global_monitoring_enabled", false);
        
        if (isGlobalMonitoring) {
            Toast.makeText(this, "Global monitoring is enabled. Configure in settings.", Toast.LENGTH_LONG).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        123);
                return;
            }
        }

        boolean isMonitoring = monitorPrefs.getBoolean("monitoring_" + deviceMac, false);
        String deviceName = ((TextView) findViewById(R.id.deviceName)).getText().toString();
        
        // Setup AlarmManager
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, DeviceMonitorReceiver.class);
        intent.putExtra("device_mac", deviceMac);
        intent.putExtra("stok", stok);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, 
            deviceMac.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (!isMonitoring) {
            // Start monitoring
            monitorPrefs.edit()
                .putString("monitored_mac", deviceMac)
                .putString("stok", stok)
                .putString("device_name_" + deviceMac, deviceName)  // Store device name
                .putBoolean("monitoring_" + deviceMac, true)
                .apply();

            // Schedule exact alarm for checking device status
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis(),
                    15 * 1000, // Check every 15 seconds
                    pendingIntent
                );
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis(),
                    15 * 1000, // Check every 15 seconds
                    pendingIntent
                );
            }
                    
            notificationImageView.setImageResource(R.drawable.notificatio_active);
            notificationText.setText("Monitoring");
            Toast.makeText(this, "Device monitoring started", Toast.LENGTH_SHORT).show();
        } else {
            // Stop monitoring
            monitorPrefs.edit()
                .remove("monitored_mac")
                .remove("stok")
                .putBoolean("monitoring_" + deviceMac, false)
                .apply();
            
            // Cancel AlarmManager
            alarmManager.cancel(pendingIntent);
                    
            notificationImageView.setImageResource(R.drawable.notification);
            notificationText.setText("Monitor Device");
            Toast.makeText(this, "Device monitoring stopped", Toast.LENGTH_SHORT).show();
        }

        // Hide the animation and show views after it finishes
        new Handler().postDelayed(() -> {
            lottieAnimationView.setVisibility(View.GONE);
            showViews();
        }, 2000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry enabling notifications
                toggleNotificationState();
            } else {
                Toast.makeText(this, "Notification permission is required to monitor devices", 
                    Toast.LENGTH_LONG).show();
                notificationImageView.setImageResource(R.drawable.notification);
            }
        }
    }

    private void toggleQoSState() {
        if (qosImage.getDrawable().getConstantState().equals(getResources().getDrawable(R.drawable.qos).getConstantState())) {
            qosImage.setImageResource(R.drawable.qos_active);
            sharedPreferences.edit().putBoolean("qos_image_active_" + deviceMac, true).apply();
        } else {
            qosImage.setImageResource(R.drawable.qos);
            sharedPreferences.edit().putBoolean("qos_image_active_" + deviceMac, false).apply();
        }

        // Hide the animation and show views after it finishes
        new Handler().postDelayed(() -> {
            lottieAnimationView.setVisibility(View.GONE);
            showViews();
        }, 2000); // Adjust the delay based on animation duration
    }

    private void toggleBlockStatus() {
        boolean isBlocked = blockText.getText().toString().equalsIgnoreCase("Unblock");
        int option = isBlocked ? 1 : 0; // 0 for block, 1 for unblock
        String apiUrl = "http://192.168.31.1/cgi-bin/luci/;stok=" + stok +
                "/api/xqnetwork/edit_device?mac=" + deviceMac.replace(":", "%3A") +
                "&model=0&option=" + option;

        new Thread(() -> {
            try {
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

                    if (response.toString().contains("\"code\":0")) {
                        runOnUiThread(() -> {
                            // Toggle the block status in the UI
                            if (isBlocked) {
                                blockuser.setImageResource(R.drawable.block);
                                blockText.setText("Block");
                            } else {
                                blockuser.setImageResource(R.drawable.unblock);
                                blockText.setText("Unblock");
                            }

                            // Hide the animation and show views after it finishes
                            lottieAnimationView.setVisibility(View.GONE);
                            showViews();
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("DeviceDetails", "Error toggling block status: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    // Hide the animation and show views in case of an error
                    lottieAnimationView.setVisibility(View.GONE);
                    showViews();
                });
            }
        }).start();
    }

    private void toggleInternetAccess() {
        boolean isInternetBlocked = accessText.getText().toString().equalsIgnoreCase("Enable Access");

        toggleInternetAccess(deviceMac, isInternetBlocked, new InternetAccessCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    // Update the UI
                    if (isInternetBlocked) {
                        accessIcon.setImageResource(R.drawable.web_block);
                        accessText.setText("Disable Access");
                    } else {
                        accessIcon.setImageResource(R.drawable.web_unblock);
                        accessText.setText("Enable Access");
                    }

                    // Hide the animation and show views after it finishes
                    lottieAnimationView.setVisibility(View.GONE);
                    showViews();
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e("DeviceDetails", "Error toggling internet access: " + error);

                runOnUiThread(() -> {
                    // Hide the animation and show views in case of an error
                    lottieAnimationView.setVisibility(View.GONE);
                    showViews();
                });
            }
        });
    }

    private interface InternetAccessCallback {
        void onSuccess();

        void onFailure(String error);
    }

    private void toggleInternetAccess(String mac, boolean isBlocked, InternetAccessCallback callback) {
        new Thread(() -> {
            try {
                // Determine the API URL based on the current state
                String apiUrl = "http://192.168.31.1/cgi-bin/luci/;stok=" + stok +
                        "/api/xqsystem/set_mac_filter?mac=" + mac.replace(":", "%3A") +
                        "&wan=" + (isBlocked ? 1 : 0); // 0 for block, 1 for unblock

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

                    // Check if the API call was successful
                    if (response.toString().contains("\"code\":0")) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure("API returned an error");
                    }
                } else {
                    callback.onFailure("HTTP error: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }).start();
    }

    private void checkBlockStatus() {
        new Thread(() -> {
            try {
                String apiUrl = "http://192.168.31.1/cgi-bin/luci/;stok=" + stok + "/api/xqnetwork/wifi_macfilter_info";
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

                    // Parse the JSON response
                    JsonObject jsonResponse = new Gson().fromJson(response.toString(), JsonObject.class);
                    JsonArray weblist = jsonResponse.getAsJsonArray("weblist");

                    boolean isBlocked = IntStream.range(0, weblist.size()).mapToObj(i -> weblist.get(i).getAsJsonObject()).anyMatch(device -> device.get("mac").getAsString().equalsIgnoreCase(deviceMac));

                    // Update UI based on whether the device is blocked
                    runOnUiThread(() -> {
                        if (isBlocked) {
                            blockuser.setImageResource(R.drawable.unblock);
                            blockText.setText("Unblock");
                        } else {
                            blockuser.setImageResource(R.drawable.block);
                            blockText.setText("Block");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("DeviceDetails", "Error checking block status: " + e.getMessage(), e);
            }
        }).start();
    }

    private void startSpeedUpdates() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchLatestSpeeds();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        }, REFRESH_INTERVAL);
    }

    private void fetchLatestSpeeds() {
        if (stok == null || deviceMac == null) {
            Log.e("DeviceDetails", "STOK or deviceMac is null. STOK: " + stok + ", MAC: " + deviceMac);
            return;
        }
        new Thread(() -> {
            try {
                // First API call for device list
                String deviceListUrl = "http://192.168.31.1/cgi-bin/luci/;stok=" + stok + "/api/misystem/devicelist";
                HttpURLConnection connection = (HttpURLConnection) new URL(deviceListUrl).openConnection();
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

                    // Parse and update device speeds
                    JsonObject jsonResponse = new Gson().fromJson(response.toString(), JsonObject.class);
                    JsonArray deviceList = jsonResponse.getAsJsonArray("list");

                    boolean deviceFound = false;
                    for (int i = 0; i < deviceList.size(); i++) {
                        JsonObject device = deviceList.get(i).getAsJsonObject();
                        if (deviceMac.equals(device.get("mac").getAsString())) {
                            deviceFound = true;
                            JsonArray ipArray = device.getAsJsonArray("ip");
                            if (ipArray != null && ipArray.size() > 0) {
                                JsonObject ipInfo = ipArray.get(0).getAsJsonObject();
                                String upSpeed = ipInfo.get("upspeed").getAsString();
                                String downSpeed = ipInfo.get("downspeed").getAsString();
                                Log.d("DeviceDetails", "Found device. Up: " + upSpeed + ", Down: " + downSpeed);
                                runOnUiThread(() -> updateSpeedDisplay(upSpeed, downSpeed));
                            }
                            break;
                        }
                    }
                    if (!deviceFound) {
                        Log.e("DeviceDetails", "Device with MAC " + deviceMac + " not found in list");
                    }
                } else {
                    Log.e("DeviceDetails", "API call failed with code: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                Log.e("DeviceDetails", "Error fetching speeds: " + e.getMessage(), e);
            }
        }).start();
    }

    private void updateSpeedDisplay(String uploadSpeed, String downloadSpeed) {
        try {
            if (uploadSpeed == null || downloadSpeed == null) {
                Log.e("DeviceDetails", "Speed values are null");
                return;
            }
            long upSpeed = Long.parseLong(uploadSpeed);
            long downSpeed = Long.parseLong(downloadSpeed);
            Log.d("DeviceDetails", "Updating UI with Up: " + upSpeed + ", Down: " + downSpeed);
            uploadSpeedView.setText(formatSpeed(upSpeed));
            downloadSpeedView.setText(formatSpeed(downSpeed));
        } catch (NumberFormatException e) {
            Log.e("DeviceDetails", "Error parsing speeds: " + e.getMessage());
        }
    }

    private String formatSpeed(long speedInKB) {
        return speedInKB >= 1024 ? String.format("%.2f MB/s", speedInKB / 1024.0) : speedInKB + " KB/s";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // This handles the back button press
        return true;
    }
}