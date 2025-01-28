package com.example.wifimanager;

import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.wifimanager.miwifi.DO.MiWifiDeviceDO;
import com.example.wifimanager.miwifi.DO.MiWifiDevicelistDO;
import com.example.wifimanager.miwifi.DO.MiWifiStatusDO;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String STOK;
    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView uploadSpeedTextView;
    private TextView downloadSpeedTextView;
    private Handler handler;
    private Runnable speedTestRunnable;
    private static final int REFRESH_INTERVAL = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String routerName = getIntent().getStringExtra("ROUTER_NAME");
        STOK = getIntent().getStringExtra("STOK");

        if (STOK == null || STOK.isEmpty()) {
            Toast.makeText(this, "STOK token is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Display the router name in a TextView
        TextView routerNameTextView = findViewById(R.id.routername);
        if (routerName != null) {
            routerNameTextView.setText(routerName);
        } else {
            routerNameTextView.setText("Router name not available");
        }

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Initialize the SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Initialize the TextViews for upload and download speeds
        uploadSpeedTextView = findViewById(R.id.upload);
        downloadSpeedTextView = findViewById(R.id.textView4);

        // Load GIFs into ImageViews
        ImageView upArrowGif = findViewById(R.id.imageView2);
        ImageView downArrowGif = findViewById(R.id.imageView3);
        Glide.with(this)
                .asGif()
                .load(R.drawable.up_arrow) // Replace with your up arrow GIF file name
                .into(upArrowGif);
        Glide.with(this)
                .asGif()
                .load(R.drawable.down_arrow) // Replace with your down arrow GIF file name
                .into(downArrowGif);

        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchConnectedDevices();
        });

        // Initialize Handler and Runnable for periodic speed test refresh
        handler = new Handler();
        speedTestRunnable = new Runnable() {
            @Override
            public void run() {
                fetchSpeedTestData();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        };

        // Start the periodic speed test refresh
        handler.post(speedTestRunnable);

        // Fetch connected devices initially
        fetchConnectedDevices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the periodic refresh when the activity is destroyed
        handler.removeCallbacks(speedTestRunnable);
    }

    private void fetchConnectedDevices() {
        swipeRefreshLayout.setRefreshing(true);
        new Thread(() -> {
            try {
                // Use the token to call the API for connected devices
                String urlStr = "http://192.168.31.1/cgi-bin/luci/;stok=" + STOK + "/api/misystem/devicelist";
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(MainActivity.this, "Failed to fetch connected devices. Response code: " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the response to get the list of connected devices
                Gson gson = new Gson();
                MiWifiDevicelistDO deviceListResponse = gson.fromJson(response.toString(), MiWifiDevicelistDO.class);

                if (deviceListResponse != null && deviceListResponse.getList() != null) {
                    List<MiWifiDeviceDO> deviceList = deviceListResponse.getList();

                    // Sort the device list based on connection time
                    Collections.sort(deviceList, Comparator.comparingLong(MiWifiDeviceDO::getConnectionTime));

                    runOnUiThread(() -> {
                        // Update RecyclerView with the list of connected devices
                        adapter.updateDeviceList(deviceList);

                        // Calculate and display the total upload and download speeds
                        long totalUploadSpeed = 0;
                        long totalDownloadSpeed = 0;
                        for (MiWifiDeviceDO device : deviceList) {
                            for (MiWifiDeviceDO.IP ip : device.getIp()) {
                                totalUploadSpeed += Long.parseLong(ip.getUpspeed());
                                totalDownloadSpeed += Long.parseLong(ip.getDownspeed());
                            }
                        }
                        uploadSpeedTextView.setText(formatSpeed(totalUploadSpeed));
                        downloadSpeedTextView.setText(formatSpeed(totalDownloadSpeed));

                        // Stop the refresh animation
                        swipeRefreshLayout.setRefreshing(false);
                    });
                } else {
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(MainActivity.this, "No connected devices found", Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(MainActivity.this, "Failed to fetch connected devices: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void fetchSpeedTestData() {
        new Thread(() -> {
            try {
                // Use the token to call the API for speed test data
                String urlStr = "http://192.168.31.1/cgi-bin/luci/;stok=" + STOK + "/api/misystem/status";
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Failed to fetch speed test data. Response code: " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the response to get the speed test data
                Gson gson = new Gson();
                MiWifiStatusDO statusResponse = gson.fromJson(response.toString(), MiWifiStatusDO.class);

                if (statusResponse != null && statusResponse.getWan() != null) {
                    MiWifiStatusDO.WAN wan = statusResponse.getWan();
                    runOnUiThread(() -> {
                        uploadSpeedTextView.setText(formatSpeed(Long.parseLong(wan.getUpspeed())));
                        downloadSpeedTextView.setText(formatSpeed(Long.parseLong(wan.getDownspeed())));
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Failed to fetch speed test data", Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Failed to fetch speed test data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private String formatSpeed(long speedInKB) {
        if (speedInKB >= 1024) {
            return String.format("%.2f MB/s", speedInKB / 1024.0);
        } else {
            return speedInKB + " KB/s";
        }
    }
}