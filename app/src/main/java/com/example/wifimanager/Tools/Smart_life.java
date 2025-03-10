package com.example.wifimanager.Tools;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.wifimanager.R;
import com.example.wifimanager.smart_life_options.*;

public class Smart_life extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_samrt_life);

        // Set up toolbar with back arrow
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up click listeners for each tool icon
        setupToolClickListeners();
    }

    private void setupToolClickListeners() {
        // Guest Wi-Fi icon click listener
        LinearLayout guestWifiLayout = findViewById(R.id.guest_wifi_layout);
        guestWifiLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Smart_life.this, GuestWifiActivity.class);
                startActivity(intent);
            }
        });

        // QoS icon click listener
        LinearLayout qosLayout = findViewById(R.id.qos_layout);
        qosLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Smart_life.this, QOS.class);
                startActivity(intent);
            }
        });

        // Health mode icon click listener
        LinearLayout healthModeLayout = findViewById(R.id.health_mode_layout);
        healthModeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Smart_life.this, HealthModeActivity.class);
                startActivity(intent);
            }
        });

        // Schedule reboot icon click listener
        LinearLayout scheduleRebootLayout = findViewById(R.id.schedule_reboot_layout);
        scheduleRebootLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Smart_life.this, ScheduleRebootActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}