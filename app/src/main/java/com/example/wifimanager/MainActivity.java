package com.example.wifimanager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.wifimanager.ui.HomeFragment;
import com.example.wifimanager.ui.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private String STOK;
    private String routerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieve STOK and router name from the Intent
        STOK = getIntent().getStringExtra("STOK");
        routerName = getIntent().getStringExtra("ROUTER_NAME");

        // If STOK and router name are null, get them from SharedPreferences
        if (STOK == null || routerName == null) {
            SharedPreferences sharedPreferences = getSharedPreferences("MyWiFiApp", MODE_PRIVATE);
            STOK = sharedPreferences.getString("stok", null);
            routerName = sharedPreferences.getString("ROUTER_NAME", null);
        }

        // Show Toast for debugging (you can remove this later)
        if (STOK != null) {
            Toast.makeText(this, "STOK: " + STOK, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "STOK is null", Toast.LENGTH_LONG).show();
        }

        // Bottom Navigation Setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    // Pass both STOK and router name to HomeFragment
                    selectedFragment = HomeFragment.newInstance(STOK, routerName);
                    break;
                case R.id.navigation_settings:
                    selectedFragment = new SettingsFragment();
                    Bundle args = new Bundle();
                    args.putString("ROUTER_NAME", routerName); // Pass the router's name
                    args.putString("STOK", STOK); // Pass the token
                    selectedFragment.setArguments(args);
                    break;
            }
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });

        // Load the default fragment if no fragment has been saved
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
    }

    private void loadFragment(Fragment fragment) {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        } catch (Exception e) {
            Log.e(TAG, "Error loading fragment", e);
        }
    }
}
