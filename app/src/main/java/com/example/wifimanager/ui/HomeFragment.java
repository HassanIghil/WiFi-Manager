package com.example.wifimanager.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.wifimanager.DeviceAdapter;
import com.example.wifimanager.Devices_List.DeviceDetailsActivity;
import com.example.wifimanager.R;
import com.example.wifimanager.Tools.Firewall;
import com.example.wifimanager.miwifi.DO.MiWifiDeviceDO;
import com.example.wifimanager.miwifi.DO.MiWifiDevicelistDO;
import com.example.wifimanager.miwifi.DO.MiWifiStatusDO;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int REFRESH_INTERVAL = 2000; // 2 seconds
    private static final int DEVICE_REFRESH_INTERVAL = 5000; // 5 seconds

    // Fragment state control
    private final AtomicBoolean isFragmentActive = new AtomicBoolean(false);

    private Handler handler;
    private SpeedTestRunnable speedTestRunnable;
    private DeviceListRunnable deviceListRunnable;

    // UI Components
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView uploadSpeedTextView, downloadSpeedTextView;
    private DeviceAdapter adapter;
    private String STOK;
    private TextView statusTextView;
    private ImageView arrow;
    private SharedPreferences sharedPreferences;

    // Add this method to create a new instance of HomeFragment
    public static HomeFragment newInstance(String stok, String routerName) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("STOK", stok);
        args.putString("ROUTER_NAME", routerName);
        fragment.setArguments(args);
        return fragment;
    }

    // Weak reference handler implementation for speed test
    private static class SpeedTestRunnable implements Runnable {
        private final WeakReference<HomeFragment> fragmentRef;
        private final Handler handler = new Handler(Looper.getMainLooper());

        SpeedTestRunnable(HomeFragment fragment) {
            this.fragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void run() {
            HomeFragment fragment = fragmentRef.get();
            if (fragment != null && fragment.isFragmentActive.get()) {
                fragment.fetchSpeedTestData();
                handler.postDelayed(this, REFRESH_INTERVAL);
            }
        }

        void clean() {
            handler.removeCallbacks(this);
        }
    }

    // Weak reference handler implementation for device list refresh
    private static class DeviceListRunnable implements Runnable {
        private final WeakReference<HomeFragment> fragmentRef;
        private final Handler handler = new Handler(Looper.getMainLooper());

        DeviceListRunnable(HomeFragment fragment) {
            this.fragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void run() {
            HomeFragment fragment = fragmentRef.get();
            if (fragment != null && fragment.isFragmentActive.get()) {
                fragment.fetchConnectedDevices(false); // Automatic refresh without loading
                handler.postDelayed(this, DEVICE_REFRESH_INTERVAL);
            }
        }

        void clean() {
            handler.removeCallbacks(this);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        isFragmentActive.set(true);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("firewall_prefs", Context.MODE_PRIVATE);

        // Initialize components
        setupViews(view);
        setupRecyclerView(view);
        setupRefreshMechanism(view);

        // Initialize statusTextView
        statusTextView = view.findViewById(R.id.status);
        statusTextView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Firewall.class);
            intent.putExtra("STOK", STOK); // Pass the STOK token
            startActivityForResult(intent, 1); // Launch Firewall activity for result
        });

        arrow = view.findViewById(R.id.imageView4);
        arrow.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Firewall.class);
            intent.putExtra("STOK", STOK); // Pass the STOK token
            startActivityForResult(intent, 1); // Launch Firewall activity for result
        });

        // Check firewall state at launch
        checkFirewallState();

        startPeriodicUpdates();

        // Retrieve STOK from arguments
        if (getArguments() != null) {
            STOK = getArguments().getString("STOK");
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the firewall state when the fragment resumes
        checkFirewallState();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            // Refresh firewall state when returning from Firewall activity
            checkFirewallState();
        }
    }

    private void setupViews(View view) {
        // Add router name initialization
        TextView routerNameTextView = view.findViewById(R.id.routername);
        uploadSpeedTextView = view.findViewById(R.id.upload);
        downloadSpeedTextView = view.findViewById(R.id.textView4);

        // Retrieve router name from arguments
        if (getArguments() != null) {
            String routerName = getArguments().getString("ROUTER_NAME", "My Router");
            routerNameTextView.setText(routerName);
        }

        // Existing arrow setup code...
        ImageView upArrow = view.findViewById(R.id.imageView2);
        ImageView downArrow = view.findViewById(R.id.imageView3);
        Glide.with(this)
                .asGif()
                .load(R.drawable.up_arrow)
                .into(upArrow);
        Glide.with(this)
                .asGif()
                .load(R.drawable.down_arrow)
                .into(downArrow);
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Ensure STOK is retrieved from arguments first
        if (getArguments() != null) {
            STOK = getArguments().getString("STOK");
        }

        // Pass the context (this fragment's context) as the fourth argument
        adapter = new DeviceAdapter(new ArrayList<>(), device -> {
            Intent intent = new Intent(getActivity(), DeviceDetailsActivity.class);
            intent.putExtra("DEVICE_NAME", device.getName());
            intent.putExtra("DEVICE_IP", device.getIp().get(0).getIp());
            intent.putExtra("DEVICE_MAC", device.getMac());
            intent.putExtra("STOK", STOK); // Make sure STOK is passed here
            startActivity(intent);
        }, STOK, requireContext());

        // Add requireContext() as the fourth argument
        recyclerView.setAdapter(adapter);
    }

    private void setupRefreshMechanism(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isFragmentActive.get()) {
                fetchConnectedDevices(true); // Manual refresh with loading
            }
        });
    }

    private void startPeriodicUpdates() {
        handler = new Handler(Looper.getMainLooper());
        speedTestRunnable = new SpeedTestRunnable(this);
        deviceListRunnable = new DeviceListRunnable(this);
        handler.post(speedTestRunnable);
        handler.post(deviceListRunnable);
        fetchConnectedDevices(true); // Initial fetch with loading
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive.set(false);

        // Cleanup resources
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (speedTestRunnable != null) {
            speedTestRunnable.clean();
        }
        if (deviceListRunnable != null) {
            deviceListRunnable.clean();
        }

        // Clear Glide resources
        ImageView upArrow = getView().findViewById(R.id.imageView2);
        ImageView downArrow = getView().findViewById(R.id.imageView3);
        Glide.with(this).clear(upArrow);
        Glide.with(this).clear(downArrow);
    }

    private void fetchConnectedDevices(boolean showLoading) {
        if (!isFragmentActive.get()) return;
        if (showLoading) {
            safeRun(() -> swipeRefreshLayout.setRefreshing(true));
        }
        new Thread(() -> {
            try {
                String urlStr = "http://192.168.31.1/cgi-bin/luci/;stok=" + STOK + "/api/misystem/devicelist";
                HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
                connection.setRequestMethod("GET");
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    safeToast("Connection error: " + connection.getResponseCode());
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                MiWifiDevicelistDO deviceListResponse = new Gson().fromJson(response.toString(), MiWifiDevicelistDO.class);
                if (deviceListResponse == null || deviceListResponse.getList() == null) {
                    safeToast("No devices found");
                    return;
                }
                List<MiWifiDeviceDO> devices = deviceListResponse.getList();
                // Get the IP address of the current device
                String currentDeviceIp = getCurrentDeviceIp();
                Log.d("CurrentDeviceIP", "Current Device IP: " + currentDeviceIp);
                // Log the current device's IP
                // Sort the list to prioritize the current device
                Collections.sort(devices, (device1, device2) -> {
                    String ip1 = device1.getIp().get(0).getIp(); // Assuming each device has at least one IP
                    String ip2 = device2.getIp().get(0).getIp();

                    boolean isDevice1Current = ip1.equalsIgnoreCase(currentDeviceIp);
                    boolean isDevice2Current = ip2.equalsIgnoreCase(currentDeviceIp);
                    if (isDevice1Current) {
                        return -1; // Current device comes first
                    } else if (isDevice2Current) {
                        return 1; // Current device comes first
                    } else {
                        return 0; // Maintain the original order for other devices
                    }
                });
                updateUI(() -> {
                    adapter.updateDeviceList(devices);
                    calculateAndDisplaySpeeds(devices);
                    if (showLoading) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            } catch (Exception e) {
                safeToast("Error: " + e.getMessage());
                safeRun(() -> swipeRefreshLayout.setRefreshing(false));
            }
        }).start();
    }

    private String getCurrentDeviceIp() {
        WifiManager wifiManager = (WifiManager) requireContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    private void fetchSpeedTestData() {
        if (!isFragmentActive.get()) return;
        new Thread(() -> {
            try {
                String urlStr = "http://192.168.31.1/cgi-bin/luci/;stok=" + STOK + "/api/misystem/status";
                HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
                connection.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                MiWifiStatusDO statusResponse = new Gson().fromJson(response.toString(), MiWifiStatusDO.class);
                if (statusResponse == null || statusResponse.getWan() == null) {
                    safeToast("Speed data unavailable");
                    return;
                }
                updateUI(() -> {
                    MiWifiStatusDO.WAN wan = statusResponse.getWan();
                    uploadSpeedTextView.setText(formatSpeed(Long.parseLong(wan.getUpspeed())));
                    downloadSpeedTextView.setText(formatSpeed(Long.parseLong(wan.getDownspeed())));
                });
            } catch (Exception e) {
                safeToast("Speed error: " + e.getMessage());
            }
        }).start();
    }

    // Helper methods for safe UI operations
    private void safeToast(String message) {
        safeRun(() -> Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show());
    }

    private void safeRun(Runnable action) {
        if (getActivity() == null || !isAdded()) return;
        getActivity().runOnUiThread(() -> {
            if (isFragmentActive.get() && isAdded()) {
                action.run();
            }
        });
    }

    private void updateUI(Runnable updateAction) {
        safeRun(updateAction);
    }

    private void calculateAndDisplaySpeeds(List<MiWifiDeviceDO> devices) {
        long totalUpload = 0, totalDownload = 0;
        for (MiWifiDeviceDO device : devices) {
            for (MiWifiDeviceDO.IP ip : device.getIp()) {
                totalUpload += Long.parseLong(ip.getUpspeed());
                totalDownload += Long.parseLong(ip.getDownspeed());
            }
        }
        uploadSpeedTextView.setText(formatSpeed(totalUpload));
        downloadSpeedTextView.setText(formatSpeed(totalDownload));
    }

    private String formatSpeed(long speedInKB) {
        return speedInKB >= 1024 ? String.format("%.2f MB/s", speedInKB / 1024.0) : speedInKB + " KB/s";
    }

    private void checkFirewallState() {
        if (sharedPreferences != null) {
            int firewallState = sharedPreferences.getInt("firewall_state", 1); // Default to 1 (enabled)
            if (firewallState == 1) {
                // Firewall is on, display corresponding text
                SpannableStringBuilder sb = new SpannableStringBuilder("Status : Safe");
                sb.setSpan(new ForegroundColorSpan(Color.WHITE), 0, 7, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                sb.setSpan(new ForegroundColorSpan(Color.GREEN), 9, 13, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                statusTextView.setText(sb);
            } else {
                // Firewall is off, display corresponding text
                SpannableStringBuilder sb = new SpannableStringBuilder("Status : Not Safe");
                sb.setSpan(new ForegroundColorSpan(Color.WHITE), 0, 7, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                sb.setSpan(new ForegroundColorSpan(Color.RED), 9, sb.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                statusTextView.setText(sb);
            }
        }
    }
}