package com.example.wifimanager.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.wifimanager.DeviceAdapter;
import com.example.wifimanager.R;
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
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int REFRESH_INTERVAL = 2000;

    // Fragment state control
    private final AtomicBoolean isFragmentActive = new AtomicBoolean(false);
    private Handler handler;
    private SpeedTestRunnable speedTestRunnable;

    // UI Components
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView uploadSpeedTextView, downloadSpeedTextView;
    private DeviceAdapter adapter;
    private String STOK;

    // Add this method to create a new instance of HomeFragment
    public static HomeFragment newInstance(String stok, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("STOK", stok);
        fragment.setArguments(args);
        return fragment;
    }

    // Weak reference handler implementation
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        isFragmentActive.set(true);

        // Initialize components
        setupViews(view);
        setupRecyclerView(view);
        setupRefreshMechanism(view);
        startPeriodicUpdates();

        // Retrieve STOK from arguments
        if (getArguments() != null) {
            STOK = getArguments().getString("STOK");
        }

        return view;
    }

    private void setupViews(View view) {
        uploadSpeedTextView = view.findViewById(R.id.upload);
        downloadSpeedTextView = view.findViewById(R.id.textView4);

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
        adapter = new DeviceAdapter(new ArrayList<>(), device -> {
            NavHostFragment.findNavController(HomeFragment.this)
                    .navigate(HomeFragmentDirections.actionHomeFragmentToDeviceDetailFragment(device));
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupRefreshMechanism(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isFragmentActive.get()) {
                fetchConnectedDevices();
            }
        });
    }

    private void startPeriodicUpdates() {
        handler = new Handler(Looper.getMainLooper());
        speedTestRunnable = new SpeedTestRunnable(this);
        handler.post(speedTestRunnable);
        fetchConnectedDevices();
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

        // Clear Glide resources
        ImageView upArrow = getView().findViewById(R.id.imageView2);
        ImageView downArrow = getView().findViewById(R.id.imageView3);
        Glide.with(this).clear(upArrow);
        Glide.with(this).clear(downArrow);
    }

    private void fetchConnectedDevices() {
        if (!isFragmentActive.get()) return;

        swipeRefreshLayout.setRefreshing(true);
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
                Collections.sort(devices, Comparator.comparingLong(MiWifiDeviceDO::getConnectionTime));

                updateUI(() -> {
                    adapter.updateDeviceList(devices);
                    calculateAndDisplaySpeeds(devices);
                    swipeRefreshLayout.setRefreshing(false);
                    fetchSpeedTestData(); // Fetch speed test data after updating the device list
                });

            } catch (Exception e) {
                safeToast("Error: " + e.getMessage());
                safeRun(() -> swipeRefreshLayout.setRefreshing(false));
            }
        }).start();
    }

    private void fetchSpeedTestData() {
        if (!isFragmentActive.get()) return;

        new Thread(() -> {
            try {
                String urlStr = "http://192.168.31.1/cgi-bin/luci/;stok=" + STOK + "/api/misystem/status";
                HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    safeToast("Speed test failed: " + connection.getResponseCode());
                    return;
                }

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
        return speedInKB >= 1024 ?
                String.format("%.2f MB/s", speedInKB / 1024.0) :
                speedInKB + " KB/s";
    }
}