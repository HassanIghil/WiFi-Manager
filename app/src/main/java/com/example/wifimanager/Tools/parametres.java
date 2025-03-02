package com.example.wifimanager.Tools;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.View;
import android.widget.LinearLayout;
import com.example.wifimanager.R;
import com.example.wifimanager.Settings_Components.*;

public class parametres extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parametres);

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

        // Find the Wi-Fi LinearLayout by its ID
        LinearLayout wifiSettingsLayout = findViewById(R.id.wifi_settings);

        // Set an OnClickListener on the Wi-Fi LinearLayout
        wifiSettingsLayout.setOnClickListener(v -> {
            // Create an Intent to start the wifi_settings activity
            Intent intent = new Intent(parametres.this, wifi_settings.class);
            startActivity(intent);
        });

        // Find the about router LinearLayout by its ID
        LinearLayout aboute_router = findViewById(R.id.about_router);

        // Set an OnClickListener on the about router LinearLayout
        aboute_router.setOnClickListener(v -> {
            // Create an Intent to start the about_router_settings activity
            Intent intent = new Intent(parametres.this, about_router_settings.class);
            startActivity(intent);
        });


        // Find the network settings LinearLayout by its ID
        LinearLayout network_settings = findViewById(R.id.network_settings);

        // Set an OnClickListener on the network settings  LinearLayout
        network_settings.setOnClickListener(v -> {
            // Create an Intent to start the about_router_settings activity
            Intent intent = new Intent(parametres.this, network_settings.class);
            startActivity(intent);
        });


        // Find the reboot router settings LinearLayout by its ID
        LinearLayout reboot_router = findViewById(R.id.reboot_router);

        // Set an OnClickListener on the reboot router settings  LinearLayout
        reboot_router.setOnClickListener(v -> {
            // Create an Intent to start the about_router_settings activity
            Intent intent = new Intent(parametres.this, reboot_settings.class);
            startActivity(intent);
        });

        // Find the reboot router settings LinearLayout by its ID
        LinearLayout hardware_settings = findViewById(R.id.hardware);

        // Set an OnClickListener on the hardware  settings  LinearLayout
        hardware_settings.setOnClickListener(v -> {
            // Create an Intent to start the hardware_settings activity
            Intent intent = new Intent(parametres.this, hardware_settings.class);
            startActivity(intent);
        });


        // Find the notification  settings LinearLayout by its ID
        LinearLayout notification_settings = findViewById(R.id.notification);

        // Set an OnClickListener on the notification  settings  LinearLayout
        notification_settings.setOnClickListener(v -> {
            // Create an Intent to start the hardware_settings activity
            Intent intent = new Intent(parametres.this, notification_settings.class);
            startActivity(intent);
        });


        // Find the region  settings LinearLayout by its ID
        LinearLayout region_settings = findViewById(R.id.region);

        // Set an OnClickListener on the region  settings  LinearLayout
        region_settings.setOnClickListener(v -> {
            // Create an Intent to start the region_settings activity
            Intent intent = new Intent(parametres.this, region_settings.class);
            startActivity(intent);
        });



    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}