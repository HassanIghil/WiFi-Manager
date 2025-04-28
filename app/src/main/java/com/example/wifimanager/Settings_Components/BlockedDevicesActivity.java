package com.example.wifimanager.Settings_Components;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifimanager.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BlockedDevicesActivity extends AppCompatActivity {

    private static final String TAG = "BlockedDevices";
    private RecyclerView recyclerView;
    private BlockedDeviceAdapter adapter;
    private List<Device> blockedDevices;
    private List<Device> onlineDevices;
    private View emptyStateLayout;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddDevice;
    private ExecutorService executorService;
    private OkHttpClient client;
    private String STOK;
    private String BASE_URL;
    private String WIFI_INFO_URL;
    private String EDIT_DEVICE_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_devices);

        STOK = getIntent().getStringExtra("STOK");
        if (STOK == null || STOK.isEmpty()) {
            Toast.makeText(this, "Authentication token missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        BASE_URL = "http://192.168.31.1/cgi-bin/luci/;stok=" + STOK;
        WIFI_INFO_URL = BASE_URL + "/api/xqnetwork/wifi_macfilter_info";
        EDIT_DEVICE_URL = BASE_URL + "/api/xqnetwork/edit_device";

        initViews();
        setupToolbar();
        setupRecyclerView();

        client = new OkHttpClient();
        executorService = Executors.newSingleThreadExecutor();

        // Load blocked devices immediately when activity starts
        loadBlockedDevices();

        fabAddDevice.setOnClickListener(v -> showDeviceSelectionDialog());
    }

    private void initViews() {
        recyclerView = findViewById(R.id.blockedDevicesRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
        fabAddDevice = findViewById(R.id.fabAddBlockedDevice);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        blockedDevices = new ArrayList<>();
        adapter = new BlockedDeviceAdapter(blockedDevices, this::handleUnblockAction);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void handleUnblockAction(Device device) {
        showLoading(true);
        executorService.execute(() -> {
            try {
                String url = EDIT_DEVICE_URL + "?mac=" + device.getMac() + "&model=0&option=1";
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(this, "Device unblocked successfully", Toast.LENGTH_SHORT).show();
                        loadBlockedDevices(); // Refresh list after unblocking
                    } else {
                        Toast.makeText(this, "Failed to unblock device", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error unblocking device", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
            }
        });
    }

    private void loadBlockedDevices() {
        showLoading(true);
        executorService.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(WIFI_INFO_URL)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String responseBody = response.body().string();
                Log.d(TAG, "API Response: " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);

                // Get blocked MACs from macfilter array
                JSONArray macFilterArray = jsonObject.getJSONArray("macfilter");
                List<String> blockedMacs = new ArrayList<>();
                for (int i = 0; i < macFilterArray.length(); i++) {
                    blockedMacs.add(macFilterArray.getJSONObject(i).getString("mac").toLowerCase());
                }

                // Get all devices from list array
                JSONArray allDevices = jsonObject.getJSONArray("list");
                List<Device> blockedDevicesList = new ArrayList<>();
                onlineDevices = new ArrayList<>();

                // Create a map of all devices by MAC for easy lookup
                Map<String, Device> allDevicesMap = new HashMap<>();
                for (int i = 0; i < allDevices.length(); i++) {
                    JSONObject deviceJson = allDevices.getJSONObject(i);
                    Device device = parseDeviceFromJson(deviceJson);
                    allDevicesMap.put(device.getMac().toLowerCase(), device);

                    if (device.isOnline() && !blockedMacs.contains(device.getMac().toLowerCase())) {
                        onlineDevices.add(device);
                    }
                }

                // Create blocked devices list with full information
                for (String blockedMac : blockedMacs) {
                    Device blockedDevice = allDevicesMap.get(blockedMac);
                    if (blockedDevice == null) {
                        // If device not in main list, create basic device info
                        blockedDevice = new Device();
                        blockedDevice.setMac(blockedMac);
                        blockedDevice.setName(blockedMac); // Use MAC as name if not available
                        blockedDevice.setIp("Unknown");
                        blockedDevice.setType("wifi");
                        blockedDevice.setOnline(false);
                    }
                    blockedDevicesList.add(blockedDevice);
                }

                runOnUiThread(() -> {
                    blockedDevices.clear();
                    blockedDevices.addAll(blockedDevicesList);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    showLoading(false);

                    Log.d(TAG, "Blocked devices loaded: " + blockedDevices.size());
                    for (Device d : blockedDevices) {
                        Log.d(TAG, "Device: " + d.getName() + " (" + d.getMac() + ") IP: " + d.getIp());
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading devices", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading devices", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
            }
        });
    }

    private Device parseDeviceFromJson(JSONObject deviceJson) throws JSONException {
        Device device = new Device();
        device.setMac(deviceJson.getString("mac"));
        device.setName(deviceJson.optString("name", deviceJson.optString("origin_name", deviceJson.getString("mac"))));
        device.setIp(deviceJson.optString("ip", "Unknown"));
        device.setType(deviceJson.optString("type", "wifi"));
        device.setOnline(deviceJson.optInt("online", 0) == 1);
        return device;
    }

    private void showDeviceSelectionDialog() {
        if (onlineDevices == null || onlineDevices.isEmpty()) {
            Toast.makeText(this, "No online devices available to block", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] deviceNames = new String[onlineDevices.size()];
        for (int i = 0; i < onlineDevices.size(); i++) {
            deviceNames[i] = onlineDevices.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Select device to block")
                .setItems(deviceNames, (dialog, which) -> {
                    Device selectedDevice = onlineDevices.get(which);
                    blockDevice(selectedDevice);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void blockDevice(Device device) {
        showLoading(true);
        executorService.execute(() -> {
            try {
                String url = EDIT_DEVICE_URL + "?mac=" + device.getMac() + "&model=0&option=0";
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(this, "Device blocked successfully", Toast.LENGTH_SHORT).show();
                        loadBlockedDevices();
                    } else {
                        Toast.makeText(this, "Failed to block device", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error blocking device", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
            }
        });
    }

    private void updateEmptyState() {
        if (blockedDevices.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}