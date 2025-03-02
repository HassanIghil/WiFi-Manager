package com.example.wifimanager.Devices_List;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.airbnb.lottie.LottieAnimationView;
import com.example.wifimanager.R;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
        }
        toolbar.setNavigationOnClickListener(v -> finish());

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

        // Initialize SharedPreferences (only for notification and qos)
        sharedPreferences = getSharedPreferences("image_state", MODE_PRIVATE);

        // Restore notification and qos image states from SharedPreferences
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
        notificationImageView.setOnClickListener(v -> {
            if (notificationImageView.getDrawable().getConstantState().equals(getResources().getDrawable(R.drawable.notification).getConstantState())) {
                notificationImageView.setImageResource(R.drawable.notificatio_active);
                sharedPreferences.edit().putBoolean("notification_image_active_" + deviceMac, true).apply();
            } else {
                notificationImageView.setImageResource(R.drawable.notification);
                sharedPreferences.edit().putBoolean("notification_image_active_" + deviceMac, false).apply();
            }
        });

        // Set up qos image click listener
        qosImage.setOnClickListener(v -> {
            if (qosImage.getDrawable().getConstantState().equals(getResources().getDrawable(R.drawable.qos).getConstantState())) {
                qosImage.setImageResource(R.drawable.qos_active);
                sharedPreferences.edit().putBoolean("qos_image_active_" + deviceMac, true).apply();
            } else {
                qosImage.setImageResource(R.drawable.qos);
                sharedPreferences.edit().putBoolean("qos_image_active_" + deviceMac, false).apply();
            }
        });

        // Set up block/unblock click listener
        blockuser.setOnClickListener(v -> {
            // Check the current block status from the UI
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
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e("DeviceDetails", "Error toggling block status: " + e.getMessage(), e);
                }
            }).start();
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

        // Set up access icon click listener
        accessIcon.setOnClickListener(v -> {
            // Check the current internet access status
            boolean isInternetBlocked = accessText.getText().toString().equalsIgnoreCase("Enable Access");

            // Toggle the state
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
                    });
                }

                @Override
                public void onFailure(String error) {
                    Log.e("DeviceDetails", "Error toggling internet access: " + error);
                }
            });
        });
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

    private interface InternetAccessCallback {
        void onSuccess();

        void onFailure(String error);
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