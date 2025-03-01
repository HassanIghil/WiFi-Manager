package com.example.wifimanager.Devices_List;

import android.app.Notification;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.WindowManager;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.util.Log;
import com.example.wifimanager.R;
import com.example.wifimanager.miwifi.DO.MiWifiDeviceDO;
import com.example.wifimanager.miwifi.DO.MiWifiDevicelistDO;
import com.example.wifimanager.ui.HomeFragment;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.Gson;

public class DeviceDetailsActivity extends AppCompatActivity {

    private TextView uploadSpeedView;
    private TextView downloadSpeedView;
    private String deviceMac;
    private Handler handler;
    private static final int REFRESH_INTERVAL = 2000;
    private String stok;
    private ImageView notificationImageView;
    private ImageView qosImage;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_details);

        // Set up edge-to-edge display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.statusBars());
            }
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            );
        }

        // Get device name from intent
        String deviceName = getIntent().getStringExtra("DEVICE_NAME");

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(""); // Set empty title
            // Or you can hide the title completely:
            // getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        notificationImageView = findViewById(R.id.notification_icon);
        qosImage = findViewById(R.id.qos_icon);
        ImageView deviceImageView = findViewById(R.id.deviceImage);
        TextView deviceNameTextView = findViewById(R.id.deviceName);
        TextView deviceIpTextView = findViewById(R.id.deviceIp);
        TextView deviceMacTextView = findViewById(R.id.deviceMac);
        uploadSpeedView = findViewById(R.id.uploadSpeed);
        downloadSpeedView = findViewById(R.id.downloadSpeed);

        // Get data from intent
        String deviceIp = getIntent().getStringExtra("DEVICE_IP");
        deviceMac = getIntent().getStringExtra("DEVICE_MAC");
        int deviceImageRes = getIntent().getIntExtra("DEVICE_IMAGE", R.drawable.android);
        String uploadSpeed = getIntent().getStringExtra("UPLOAD_SPEED");
        String downloadSpeed = getIntent().getStringExtra("DOWNLOAD_SPEED");
        stok = getIntent().getStringExtra("STOK");

        // Add this line to get STOK

        // Set data to views
        deviceImageView.setImageResource(deviceImageRes);
        deviceNameTextView.setText(deviceName);
        deviceIpTextView.setText("IP: " + deviceIp);
        deviceMacTextView.setText("MAC: " + deviceMac);
        updateSpeedDisplay(uploadSpeed, downloadSpeed);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("image_state", MODE_PRIVATE);

        // Restore image state
        if (sharedPreferences.getBoolean("notification_image_active_" + deviceMac, false)) {
            notificationImageView.setImageResource(R.drawable.notificatio_active);
        } else {
            notificationImageView.setImageResource(R.drawable.notification);
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
        notificationImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (notificationImageView.getDrawable().getConstantState().equals(getResources().getDrawable(R.drawable.notification).getConstantState())) {
                    notificationImageView.setImageResource(R.drawable.notificatio_active);
                    sharedPreferences.edit().putBoolean("notification_image_active_" + deviceMac, true).apply();
                } else {
                    notificationImageView.setImageResource(R.drawable.notification);
                    sharedPreferences.edit().putBoolean("notification_image_active_" + deviceMac, false).apply();
                }
            }
        });
        qosImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (qosImage.getDrawable().getConstantState().equals(getResources().getDrawable(R.drawable.qos).getConstantState())) {
                    qosImage.setImageResource(R.drawable.qos_active);
                    sharedPreferences.edit().putBoolean("qos_image_active_" + deviceMac, true).apply();
                } else {
                    qosImage.setImageResource(R.drawable.qos);
                    sharedPreferences.edit().putBoolean("qos_image_active_" + deviceMac, false).apply();
                }
            }
        });
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
                    MiWifiDevicelistDO deviceList = new Gson().fromJson(response.toString(), MiWifiDevicelistDO.class);
                    if (deviceList != null && deviceList.getList() != null) {
                        boolean deviceFound = false;
                        for (MiWifiDeviceDO device : deviceList.getList()) {
                            if (deviceMac.equals(device.getMac())) {
                                deviceFound = true;
                                if (!device.getIp().isEmpty()) {
                                    String upSpeed = device.getIp().get(0).getUpspeed();
                                    String downSpeed = device.getIp().get(0).getDownspeed();
                                    Log.d("DeviceDetails", "Found device. Up: " + upSpeed + ", Down: " + downSpeed);
                                    runOnUiThread(() -> updateSpeedDisplay(upSpeed, downSpeed));
                                }
                                break;
                            }
                        }
                        if (!deviceFound) {
                            Log.e("DeviceDetails", "Device with MAC " + deviceMac + " not found in list");
                        }
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
