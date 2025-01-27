package com.example.wifimanager;

import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifimanager.miwifi.DO.MiWifiDeviceDO;
import com.example.wifimanager.miwifi.DO.MiWifiDevicelistDO;
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
    private Handler handler;
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 5000; // 5 seconds

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

        // Initialize Handler and Runnable for periodic refresh
        handler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchConnectedDevices();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        };

        // Start the periodic refresh
        handler.post(refreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the periodic refresh when the activity is destroyed
        handler.removeCallbacks(refreshRunnable);
    }

    private void fetchConnectedDevices() {
        new Thread(() -> {
            try {
                // Use the token to call the API for connected devices
                String urlStr = "http://192.168.31.1/cgi-bin/luci/;stok=" + STOK + "/api/misystem/devicelist";
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to fetch connected devices. Response code: " + responseCode, Toast.LENGTH_SHORT).show());
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
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "No connected devices found", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to fetch connected devices: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}