package com.example.wifimanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.wifimanager.database.DatabaseHelper;
import com.example.wifimanager.ui.HomeFragment;
import com.example.wifimanager.ui.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private String STOK;
    private String routerName;
    private DatabaseHelper dbHelper;
    private BroadcastReceiver routerNameReceiver;

    private static final String TABLE_NAME = DatabaseHelper.TABLE_PREFERENCES;
    private static final String KEY_STOK = "stok";
    private static final String KEY_ROUTER_NAME = "router_name";
    private static final String KEY_FIREWALL_ENABLED = "firewall_enabled";
    private static final String KEY_BLOCKED_COUNT = "blocked_count";
    private static final String KEY_PROTECTED_TIME = "protected_time";
    private static final int NOTIFICATION_PERMISSION_CODE = 123;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); //force light mode
        setContentView(R.layout.activity_main);

        dbHelper = DatabaseHelper.getInstance(this);

        // Retrieve STOK and router name from the Intent
        STOK = getIntent().getStringExtra("STOK");
        routerName = getIntent().getStringExtra("ROUTER_NAME");

        // If STOK and router name are null, get them from database
        if (STOK == null || routerName == null) {
            STOK = dbHelper.getString(TABLE_NAME, KEY_STOK, null);
            routerName = dbHelper.getString(TABLE_NAME, KEY_ROUTER_NAME, null);
        } else {
            // Save new STOK to database when received from intent
            dbHelper.putString(TABLE_NAME, KEY_STOK, STOK);
        }

        // Register broadcast receiver for router name updates
        routerNameReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String newName = intent.getStringExtra("router_name");
                if (newName != null) {
                    routerName = newName;
                    dbHelper.putString(TABLE_NAME, KEY_ROUTER_NAME, newName);
                }
            }
        };
        registerReceiver(routerNameReceiver, new IntentFilter("ROUTER_NAME_UPDATED"));

        // Initialize firewall state, blocked, and protected values in database if not already set
        if (!dbHelper.containsKey(TABLE_NAME, KEY_FIREWALL_ENABLED)) {
            dbHelper.putBoolean(TABLE_NAME, KEY_FIREWALL_ENABLED, true); // Default to "On"
            dbHelper.putInt(TABLE_NAME, KEY_BLOCKED_COUNT, 0); // Initialize blocked count
            dbHelper.putInt(TABLE_NAME, KEY_PROTECTED_TIME, 0); // Initialize protected time
        }

        // Request notification permission for Android 13+
        checkNotificationPermission();

        // Bottom Navigation Setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    // Pass both STOK, router name, and firewall state to HomeFragment
                    selectedFragment = new HomeFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("STOK", STOK);
                    bundle.putString("ROUTER_NAME", routerName);
                    bundle.putBoolean("FIREWALL_STATE", dbHelper.getBoolean(TABLE_NAME, KEY_FIREWALL_ENABLED, true));
                    selectedFragment.setArguments(bundle);
                    break;
                case R.id.navigation_settings:
                    selectedFragment = new SettingsFragment();
                    Bundle settingsBundle = new Bundle();
                    settingsBundle.putString("STOK", STOK);
                    selectedFragment.setArguments(settingsBundle);
                    break;
            }

            if (selectedFragment != null) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, selectedFragment);
                transaction.commit();
                return true;
            }
            return false;
        });

        // Set Home as default selection
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            // Handle permission result if needed
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (routerNameReceiver != null) {
            unregisterReceiver(routerNameReceiver);
        }
    }
}